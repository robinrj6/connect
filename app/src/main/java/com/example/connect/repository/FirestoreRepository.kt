package com.example.connect.repository

import com.example.connect.model.FriendStatus
import com.example.connect.model.User
import com.example.connect.model.UserWithStatus
import com.google.firebase.Firebase
import java.time.LocalDate
import java.time.ZoneOffset
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreRepository(private val db: FirebaseFirestore) {

    fun observeEmojis(
        senderId: String,
        onUpdate: (Map<String, Int>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration? {
        val uid = currentUserId ?: return null
        // received counts are stored as sender_receiver (friend_me)
        val docId = "${senderId}_${uid}"
        val today = LocalDate.now(ZoneOffset.UTC).toString()

        return db.collection("interactions").document(docId)
            .collection("daily").document(today)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    // 1. Get the "counts" MAP field from the document
                    @Suppress("UNCHECKED_CAST")
                    val rawCounts = snapshot.get("counts") as? Map<String, Any> ?: emptyMap()

                    // 2. Convert Firestore Longs to Kotlin Ints
                    val emojiMap = rawCounts.mapValues { entry ->
                        when (val value = entry.value) {
                            is Long -> value.toInt()
                            is Int -> value
                            else -> 0
                        }
                    }
                    onUpdate(emojiMap)
                } else {
                    onUpdate(emptyMap())
                }
            }
    }
    fun observeFriends(
        onUpdate: (List<UserWithStatus>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration? {
        val uid = currentUserId ?: return null
        return db.collection("users").document(uid)
            .collection("friends")
            .whereEqualTo("status", FriendStatus.FRIEND.value)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val friends = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val friendUser = doc.toObject(User::class.java) ?: return@mapNotNull null
                    val isFav = doc.getBoolean("fav") ?: false
                    val nickname = doc.getString("nickname")
                    UserWithStatus(friendUser, FriendStatus.FRIEND, isFav, nickname)
                }

                onUpdate(friends)
            }
    }

    fun observePendingRequests(
        onUpdate: (List<UserWithStatus>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration? {
        val uid = currentUserId ?: return null
        return db.collection("users").document(uid)
            .collection("friends")
            .whereEqualTo("status", FriendStatus.PENDING.value)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val requests = snapshot?.toObjects(User::class.java).orEmpty()
                onUpdate(requests.map { UserWithStatus(it, FriendStatus.PENDING, null) })
            }
    }

    fun observeMyFriendStatuses(
        onUpdate: (Map<String, FriendStatus>) -> Unit,
        onError: (Exception) -> Unit = {}
    ): ListenerRegistration? {
        val uid = currentUserId ?: return null
        return db.collection("users").document(uid)
            .collection("friends")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val statuses = snapshot?.documents.orEmpty().associate { doc ->
                    doc.id to FriendStatus.from(doc.getString("status") ?: "none")
                }
                onUpdate(statuses)
            }
    }

    suspend fun incrementEmojiCount(receiverId: String, emoji: String) {
        val uid = currentUserId ?: return
        val docId = "${uid}_${receiverId}"
        val today = LocalDate.now(ZoneOffset.UTC).toString()

        db.collection("interactions")
            .document(docId)
            .collection("daily")
            .document(today)
            .set(
                mapOf("counts" to mapOf(emoji to FieldValue.increment(1))),
                SetOptions.merge()
            ).await()
    }

    suspend fun getSentEmojiCounts(receiverId: String): Map<String, Int> {
        val uid = currentUserId ?: return emptyMap()
        return fetchEmojiCounts(senderId = uid, receiverId = receiverId)
    }

    suspend fun getReceivedEmojiCounts(senderId: String): Map<String, Int> {
        val uid = currentUserId ?: return emptyMap()
        return fetchEmojiCounts(senderId = senderId, receiverId = uid)
    }

    private suspend fun fetchEmojiCounts(senderId: String, receiverId: String): Map<String, Int> {
        val today = LocalDate.now(ZoneOffset.UTC).toString()
        val docId = "${senderId}_${receiverId}"
        val snapshot = db.collection("interactions")
            .document(docId)
            .collection("daily")
            .document(today)
            .get().await()
        @Suppress("UNCHECKED_CAST")
        val raw = snapshot.get("counts") as? Map<String, Any> ?: return emptyMap()
        return raw.mapValues { (it.value as? Long)?.toInt() ?: (it.value as? Int) ?: 0 }
    }

    suspend fun getUserName(userId: String): String {
        val snapshot = db.collection("users").document(userId).get().await()
        return snapshot.getString("name") ?: ""
    }
    suspend fun getNickName(friendId: String): String {
        val uid = currentUserId ?: return ""
        val snapshot = db.collection("users").document(uid)
            .collection("friends").document(friendId).get().await()
        return snapshot.getString("nickname") ?: ""
    }

    suspend fun newUser(userId: String, userName: String, userEmail: String) {
        val userRef = db.collection("users").document(userId)
        val snapshot = userRef.get().await()

        if (snapshot.exists()) {
            val updates = mapOf(
                "lastLogin" to FieldValue.serverTimestamp()
            )
            userRef.update(updates).await()
        } else {
            val user = mapOf(
                "uid" to userId,
                "name" to userName,
                "email" to userEmail,
                "createdAt" to FieldValue.serverTimestamp(),
                "lastLogin" to FieldValue.serverTimestamp()
            )
            userRef.set(user).await()
        }
    }

    suspend fun deleteAccount() {
        val uid = currentUserId ?: return
        db.collection("users").document(uid).delete().await()
        db.collection("users").document(uid).collection("friends").get().await()
            .documents.forEach { it.reference.delete().await() }
//        db.collection("interactions").whereEqualTo("senderId", uid).get().await()
//            .documents.forEach { it.reference.delete().await() }
//        db.collection("interactions").whereEqualTo("receiverId", uid).get().await()
//            .documents.forEach { it.reference.delete().await() }
        Firebase.auth.currentUser?.delete()
        Firebase.auth.signOut()
    }

    suspend fun searchUsers(query: String): List<UserWithStatus> {
        val uid = currentUserId ?: return emptyList()
        if (query.isBlank()) return emptyList()

        val nameQuery = db.collection("users")
            .whereGreaterThanOrEqualTo("name", query)
            .whereLessThanOrEqualTo("name", query + "\uf8ff")
            .get().await().toObjects(User::class.java)

        val emailQuery = db.collection("users")
            .whereGreaterThanOrEqualTo("email", query)
            .whereLessThanOrEqualTo("email", query + "\uf8ff")
            .get().await().toObjects(User::class.java)

        val results = (nameQuery + emailQuery)
            .distinctBy { it.uid }
            .filter { it.uid != uid }

        if (results.isEmpty()) return emptyList()

        val myFriends = db.collection("users").document(uid)
            .collection("friends")
            .get().await()
            .documents
            .associate { it.id to FriendStatus.from(it.getString("status") ?: "none") }

        return results.map { user ->
            UserWithStatus(
                user = user, status = myFriends[user.uid] ?: FriendStatus.NONE, fav = null
            )
        }
    }

    private val currentUserId: String?
        get() = Firebase.auth.currentUser?.uid

    /**
     * Sends a friend request.
     * Sets 'sent' for the current user and 'pending' for the target user.
     */
    suspend fun requestFriend(user: User) {
        val batch = db.batch()
        val uid = currentUserId ?: return
        // My record (I sent it)
        val myRef = db.collection("users").document(uid)
            .collection("friends").document(user.uid)
        batch.set(myRef, mapOf(
            "uid" to user.uid,
            "name" to user.name,
            "status" to FriendStatus.SENT.value,
            "timestamp" to FieldValue.serverTimestamp()
        ))

        // Their record (They received it)
        val theirRef = db.collection("users").document(user.uid)
            .collection("friends").document(uid)
        batch.set(theirRef, mapOf(
            "uid" to uid,
            "name" to (Firebase.auth.currentUser?.displayName ?: "Unknown"),
            "status" to FriendStatus.PENDING.value,
            "timestamp" to FieldValue.serverTimestamp()
        ))

        batch.commit().await()
    }

    suspend fun setFav(user: UserWithStatus, fav: Boolean) {
        val uid = currentUserId ?: return
        db.collection("users").document(uid)
            .collection("friends").document(user.user.uid)
            .set(mapOf("fav" to fav), SetOptions.merge()).await()
    }

    suspend fun setNickname(friendId: String, nickname: String?) {
        val uid = currentUserId ?: return
        val sanitized = nickname?.trim().orEmpty()
        val value = if (sanitized.isEmpty()) null else sanitized
        db.collection("users").document(uid)
            .collection("friends").document(friendId)
            .set(mapOf("nickname" to value), SetOptions.merge()).await()
    }

    suspend fun setDefaultEmoji(friendId: String, emoji: String) {
        val uid = currentUserId ?: return
        db.collection("users").document(uid)
            .collection("friends").document(friendId)
            .set(mapOf("defaultEmoji" to emoji), SetOptions.merge()).await()
    }

    suspend fun getDefaultEmoji(friendId: String): String? {
        val uid = currentUserId ?: return null
        val snapshot = db.collection("users").document(uid)
            .collection("friends").document(friendId)
            .get().await()
        return snapshot.getString("defaultEmoji")
    }
    /**
     * Accepts a friend request.
     * Updates both documents to 'friend' status.
     */
    suspend fun acceptFriend(user: User) {
        val uid = currentUserId ?: return
        val batch = db.batch()
        val myRef = db.collection("users").document(uid)
            .collection("friends").document(user.uid)
        val theirRef = db.collection("users").document(user.uid)
            .collection("friends").document(uid)
        batch.update(myRef, "status", FriendStatus.FRIEND.value)
        batch.update(theirRef, "status", FriendStatus.FRIEND.value)
        batch.commit().await()
    }

    /**
     * Rejects or Removes a friend.
     * Deletes the records from both users' collections.
     */
    suspend fun removeFriend(user: User) {
        val uid = currentUserId ?: return
        val batch = db.batch()
        val myRef = db.collection("users").document(uid)
            .collection("friends").document(user.uid)
        val theirRef = db.collection("users").document(user.uid)
            .collection("friends").document(uid)
        batch.delete(myRef)
        batch.delete(theirRef)
        batch.commit().await()
    }

    // Alias for removeFriend for clarity in the UI logic
    suspend fun rejectFriend(user: User) = removeFriend(user)

    suspend fun getFriends(): List<UserWithStatus> {
        val uid = currentUserId ?: return emptyList()
        val friendDocs = db.collection("users").document(uid)
            .collection("friends")
            .whereEqualTo("status", FriendStatus.FRIEND.value)
            .get()
            .await()
            .documents

        return friendDocs.mapNotNull { doc ->
            val friendUser = doc.toObject(User::class.java) ?: return@mapNotNull null
            val isFav = doc.getBoolean("fav") ?: false
            val nickname = doc.getString("nickname")
            UserWithStatus(friendUser, FriendStatus.FRIEND, isFav, nickname)
        }
    }
}
