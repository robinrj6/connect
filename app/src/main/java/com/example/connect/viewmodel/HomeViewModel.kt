package com.example.connect.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.connect.model.User
import com.example.connect.model.UserWithStatus
import com.example.connect.repository.FirestoreRepository
import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.auth.auth
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.max

class HomeViewModel(
    private val repository: FirestoreRepository,
    appContext: Context
) : ViewModel() {

    private val prefs = appContext.getSharedPreferences(
        "home_emoji_baseline",
        Context.MODE_PRIVATE
    )

    private var friendsListener: ListenerRegistration? = null
    private var pendingListener: ListenerRegistration? = null
    private val emojiListeners = mutableMapOf<String, ListenerRegistration>()

    var pendingRequests: List<UserWithStatus> by mutableStateOf<List<UserWithStatus>>(emptyList())
        private set
    var friends: List<UserWithStatus> by mutableStateOf<List<UserWithStatus>>(emptyList())
        private set
    var receivedEmojiCountsByFriend: Map<String, Map<String, Int>> by mutableStateOf(emptyMap())
        private set
    var baselineEmojiCountsByFriend: Map<String, Map<String, Int>> by mutableStateOf(emptyMap())
        private set

    var showNotifications by mutableStateOf(false)
        private set

    init {
        loadBaselineSnapshot()
        observeFriendsRealtime()
        observePendingRequestsRealtime()
    }

    fun onClickNotification() {
        showNotifications = true
    }

    fun onDismissNotifications() {
        showNotifications = false
    }

    fun onAcceptRequest(user: User) {
        viewModelScope.launch {
            repository.acceptFriend(user)
            pendingRequests = pendingRequests.filter { it.user.uid != user.uid }
        }
    }

    fun onRejectRequest(user: User) {
        viewModelScope.launch {
            repository.rejectFriend(user)
            pendingRequests = pendingRequests.filter { it.user.uid != user.uid }
        }
    }

    fun loadFriends() {
        viewModelScope.launch {
            val result = repository.getFriends()
            friends = result.sortedByDescending { it.fav }
            loadReceivedEmojiCounts(friends)
        }
    }


    var friendsEmojiCounts = mutableStateMapOf<String, Map<String, Int>>()
        private set

    fun startEmojiListeners(friends: List<UserWithStatus>) {
        val friendIds = friends.map { it.user.uid }.toSet()

        // Remove listeners for friends no longer in list
        val removedIds = emojiListeners.keys - friendIds
        removedIds.forEach { id ->
            emojiListeners.remove(id)?.remove()
            friendsEmojiCounts.remove(id)
            receivedEmojiCountsByFriend = receivedEmojiCountsByFriend - id
        }

        // Add listener only once per active friend
        friends.forEach { friend ->
            if (emojiListeners.containsKey(friend.user.uid)) return@forEach

            val registration = repository.observeEmojis(
                senderId = friend.user.uid,
                onUpdate = { newCounts ->
                    friendsEmojiCounts[friend.user.uid] = newCounts
                    receivedEmojiCountsByFriend = receivedEmojiCountsByFriend + (friend.user.uid to newCounts)
                },
                onError = { /* Handle error */ }
            )

            if (registration != null) {
                emojiListeners[friend.user.uid] = registration
            }
        }
    }
    private fun observeFriendsRealtime() {
        friendsListener?.remove()
        friendsListener = repository.observeFriends(
            onUpdate = { updatedFriends ->
                viewModelScope.launch {
                    friends = updatedFriends.sortedByDescending { it.fav == true }
                    loadReceivedEmojiCounts(friends)
                    startEmojiListeners(friends)
                }
            }
        )
    }

    private fun observePendingRequestsRealtime() {
        pendingListener?.remove()
        pendingListener = repository.observePendingRequests(
            onUpdate = { updatedPending ->
                pendingRequests = updatedPending
            }
        )
    }

    private suspend fun loadReceivedEmojiCounts(friendList: List<UserWithStatus>) {
        if (friendList.isEmpty()) {
            receivedEmojiCountsByFriend = emptyMap()
            return
        }

        receivedEmojiCountsByFriend = coroutineScope {
            friendList
                .map { friend ->
                    async {
                        friend.user.uid to repository.getReceivedEmojiCounts(friend.user.uid)
                    }
                }
                .awaitAll()
                .toMap()
        }
    }

    fun receivedEmojiCounts(friendId: String): Map<String, Int> {
        return friendsEmojiCounts[friendId]
            ?: receivedEmojiCountsByFriend[friendId]
            ?: emptyMap()
    }

    fun receivedEmojiCountsSinceLastVisit(friendId: String): Map<String, Int> {
        val current = receivedEmojiCounts(friendId)
        if (current.isEmpty()) return emptyMap()

        val baseline = baselineEmojiCountsByFriend[friendId].orEmpty()
        return current.mapValues { (emoji, count) ->
            max(count - (baseline[emoji] ?: 0), 0)
        }
    }

    fun persistCurrentEmojiSnapshotAsBaseline() {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val keyPrefix = baselineKeyPrefix(uid)
        val snapshot = friends.associate { friend ->
            val friendId = friend.user.uid
            friendId to receivedEmojiCounts(friendId)
        }

        val editor = prefs.edit()
        prefs.all.keys
            .filter { it.startsWith(keyPrefix) }
            .forEach { editor.remove(it) }

        snapshot.forEach { (friendId, counts) ->
            val json = JSONObject()
            counts.forEach { (emoji, count) -> json.put(emoji, count) }
            editor.putString("$keyPrefix$friendId", json.toString())
        }
        editor.apply()
        baselineEmojiCountsByFriend = snapshot
    }

    private fun loadBaselineSnapshot() {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val keyPrefix = baselineKeyPrefix(uid)

        baselineEmojiCountsByFriend = prefs.all
            .asSequence()
            .filter { (key, value) -> key.startsWith(keyPrefix) && value is String }
            .mapNotNull { (key, value) ->
                val friendId = key.removePrefix(keyPrefix)
                val jsonString = value as? String ?: return@mapNotNull null

                runCatching {
                    val json = JSONObject(jsonString)
                    val counts = mutableMapOf<String, Int>()
                    json.keys().forEach { emoji ->
                        counts[emoji] = json.optInt(emoji, 0)
                    }
                    friendId to counts.toMap()
                }.getOrNull()
            }
            .toMap()
    }

    private fun baselineKeyPrefix(uid: String): String = "baseline_${uid}_"

    fun unfriend(user: User) {
        viewModelScope.launch {
            repository.removeFriend(user)
            friends = friends.filter { it.user.uid != user.uid }
            receivedEmojiCountsByFriend = receivedEmojiCountsByFriend - user.uid
            friendsEmojiCounts.remove(user.uid)
            emojiListeners.remove(user.uid)?.remove()
        }
    }

    fun favFriend(user: UserWithStatus, fav: Boolean) {
        viewModelScope.launch {
            repository.setFav(user, fav)
            friends = friends
                .map {
                    if (it.user.uid == user.user.uid) it.copy(fav = fav) else it
                }
                .sortedByDescending { it.fav == true }
        }
    }

    fun setNickname(user: UserWithStatus, nickname: String) {
        viewModelScope.launch {
            repository.setNickname(user.user.uid, nickname)
            friends = friends.map {
                if (it.user.uid == user.user.uid) {
                    it.copy(nickname = nickname.trim().ifBlank { null })
                } else {
                    it
                }
            }
        }
    }

    override fun onCleared() {
        persistCurrentEmojiSnapshotAsBaseline()
        friendsListener?.remove()
        pendingListener?.remove()
        emojiListeners.values.forEach { it.remove() }
        emojiListeners.clear()
        super.onCleared()
    }
}