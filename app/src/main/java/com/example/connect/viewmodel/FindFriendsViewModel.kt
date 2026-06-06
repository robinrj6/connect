package com.example.connect.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.connect.model.FriendStatus
import com.example.connect.model.User
import com.example.connect.model.UserWithStatus
import com.example.connect.repository.FirestoreRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FindFriendsViewModel(private val repository: FirestoreRepository) : ViewModel() {

    var searchQuery by mutableStateOf("")
        private set

    var searchResults by mutableStateOf<List<UserWithStatus>>(emptyList())
        private set

    var isSearching by mutableStateOf(false)
        private set

    private var searchJob: Job? = null
    private var statusesListener: ListenerRegistration? = null
    private var statusByUid: Map<String, FriendStatus> = emptyMap()

    init {
        observeStatusesRealtime()
    }

    fun onQueryChange(newQuery: String) {
        searchQuery = newQuery
        searchJob?.cancel()
        
        if (newQuery.isBlank()) {
            searchResults = emptyList()
            isSearching = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // Debounce search
            isSearching = true
            try {
                searchResults = repository.searchUsers(newQuery).map {
                    it.copy(status = statusByUid[it.user.uid] ?: it.status)
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                isSearching = false
            }
        }
    }

    fun onClickAddFriend(user: User) {
        viewModelScope.launch {
            try {
                repository.requestFriend(user)
                searchResults = searchResults.map {
                    if (it.user.uid == user.uid) it.copy(status = FriendStatus.SENT) else it
                }

            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun observeStatusesRealtime() {
        statusesListener?.remove()
        statusesListener = repository.observeMyFriendStatuses(
            onUpdate = { updatedStatuses ->
                statusByUid = updatedStatuses
                searchResults = searchResults.map {
                    it.copy(status = statusByUid[it.user.uid] ?: FriendStatus.NONE)
                }
            }
        )
    }

    override fun onCleared() {
        statusesListener?.remove()
        super.onCleared()
    }
}
