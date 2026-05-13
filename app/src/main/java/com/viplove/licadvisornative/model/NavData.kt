// File: app/src/main/java/com/viplove/licadvisornative/model/NavData.kt
package com.viplove.licadvisornative.model

/**
 * Represents the parsed NAV data for a single ULIP fund.
 */
data class NavData(
    val planName: String,
    val fundName: String,
    val sfin: String, // Segregated Fund Identification Number
    val nav: Double,
    val launchDate: String
)
