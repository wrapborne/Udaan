package com.viplove.licadvisornative.util

import java.io.IOException
import java.security.GeneralSecurityException
import java.util.concurrent.CancellationException

object ErrorMapper {

    fun map(e: Exception): String {
        return when (e) {
            is GeneralSecurityException -> "Could not access secure credentials on this device. Please try again."
            is IOException -> "Network error. Please check your internet connection."
            is CancellationException -> "The operation was cancelled."
            else -> "An unexpected error occurred. Please try again later."
        }
    }
}
