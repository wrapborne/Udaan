package com.viplove.licadvisornative.util

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.concurrent.CancellationException

object ErrorMapper {

    fun map(e: Exception): String {
        return when (e) {
            is GeneralSecurityException -> "Could not access secure credentials on this device. Please try again."
            is IOException -> "Network error. Please check your internet connection."
            is FirebaseNetworkException -> "Network error. Please check your internet connection."
            is FirebaseTooManyRequestsException -> "Too many attempts. Please wait a moment and try again."
            is FirebaseAuthException -> e.localizedMessage ?: "Authentication failed. Please try again."
            is FirebaseFirestoreException -> e.localizedMessage ?: "Database error. Please try again."
            is CancellationException -> "The operation was cancelled."
            else -> "An unexpected error occurred. Please try again later."
        }
    }
}
