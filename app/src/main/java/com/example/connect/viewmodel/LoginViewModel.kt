package com.example.connect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.connect.repository.FirestoreRepository
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: FirestoreRepository): ViewModel() {

    fun successLogin(userId: String, userName: String, userEmail: String) {
        viewModelScope.launch {
            try {
                repository.newUser(userId, userName, userEmail)

            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}