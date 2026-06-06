package com.example.connect.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.connect.repository.FirestoreRepository
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: FirestoreRepository): ViewModel() {

    fun deleteAccount() {
        viewModelScope.launch {
            try {
                repository.deleteAccount()

            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}