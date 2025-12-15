package com.example.roommade.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth = Firebase.auth

    private val _user = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user

    val isLoading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()
                _user.value = auth.currentUser
            } catch (t: Throwable) {
                error.value = t.message
            } finally {
                isLoading.value = false
            }
        }
    }

    fun signOut() {
        auth.signOut()
        _user.value = null
    }
}
