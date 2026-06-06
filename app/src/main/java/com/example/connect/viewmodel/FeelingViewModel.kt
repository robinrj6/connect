package com.example.connect.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.connect.model.EmojiAction
import com.example.connect.model.allEmojis
import com.example.connect.repository.FirestoreRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class FeelingViewModel(private val repository: FirestoreRepository) : ViewModel() {

    var selectedEmoji by mutableStateOf(allEmojis[0])
        private set

    var counts by mutableStateOf(allEmojis.associate { it.emoji to 0 })
        private set

    var receivedCounts by mutableStateOf(allEmojis.associate { it.emoji to 0 })
        private set

    var userName by mutableStateOf("")
        private set

    var nickName by mutableStateOf("")
        private set

    var currentDefaultEmoji by mutableStateOf<String?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var dropdownOpen by mutableStateOf(false)
        private set
    var receivedDropdownOpen by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun loadInitial(receiverId: String) {
        viewModelScope.launch {
            isLoading = true
            try {
                val defaultEmoji = repository.getDefaultEmoji(receiverId)
                currentDefaultEmoji = defaultEmoji
                defaultEmoji
                    ?.let { key -> allEmojis.firstOrNull { it.emoji == key } }
                    ?.let { selectedEmoji = it }

                val sentDeferred = async { repository.getSentEmojiCounts(receiverId) }
                val receivedDeferred = async { repository.getReceivedEmojiCounts(receiverId) }
                val nameDeferred = async { repository.getUserName(receiverId) }
                val nickDeferred = async { repository.getNickName(receiverId) }

                val sent = sentDeferred.await()
                val received = receivedDeferred.await()

                counts = allEmojis.associate { it.emoji to (sent[it.emoji] ?: 0) }
                receivedCounts = allEmojis.associate { it.emoji to (received[it.emoji] ?: 0) }
                userName = nameDeferred.await()
                nickName = nickDeferred.await()
            } catch (e: Exception) {
                error = "Failed to load data"
            } finally {
                isLoading = false
            }
        }
    }

    fun toggleReceivedDropdown() {
        receivedDropdownOpen = !receivedDropdownOpen
    }

    fun loadReceivedCounts(receiverId: String) {
        viewModelScope.launch {
            try {
                val fetchedDeferred = async { repository.getReceivedEmojiCounts(receiverId) }
                val nameDeferred = async { repository.getUserName(receiverId) }
                val fetched = fetchedDeferred.await()
                receivedCounts = allEmojis.associate { it.emoji to (fetched[it.emoji] ?: 0) }
                userName = nameDeferred.await()
            } catch (e: Exception) {
                error = "Failed to load received counts"
            }
        }
    }

    fun loadCounts(receiverId: String) {
        viewModelScope.launch {
            try {
                val fetched = repository.getSentEmojiCounts(receiverId)
                counts = allEmojis.associate { it.emoji to (fetched[it.emoji] ?: 0) }
            } catch (e: Exception) {
                error = "Failed to load sent counts"
            }
        }
    }

    fun onEmojiSelected(emoji: EmojiAction) {
        selectedEmoji = emoji
        dropdownOpen = false
    }

    fun onEmojiClick(receiverId: String) {
        val emojiKey = selectedEmoji.emoji
        counts = counts + (emojiKey to (counts[emojiKey] ?: 0) + 1)
        viewModelScope.launch {
            try {
                repository.incrementEmojiCount(receiverId, emojiKey)
            } catch (e: Exception) {
                error = "Failed to send emoji"
            }
        }
    }

    fun setSelectedAsDefault(receiverId: String) {
        val emojiKey = selectedEmoji.emoji
        viewModelScope.launch {
            try {
                repository.setDefaultEmoji(receiverId, emojiKey)
                currentDefaultEmoji = emojiKey
            } catch (e: Exception) {
                error = "Failed to save default emoji"
            }
        }
    }

    fun toggleDropdown() {
        dropdownOpen = !dropdownOpen
    }
}
