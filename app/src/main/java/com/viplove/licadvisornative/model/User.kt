package com.viplove.licadvisornative.model

data class User(
    var uid: String = "",
    var email: String = "",
    var name: String = "",
    var phone: String = "",
    var profilePictureUrl: String = "",
    var role: String = "",
    var isApproved: Boolean = false,
    var adminId: String = "",
    var agencyCode: String = "",
    var doCode: String = "",
    var startDate: Long? = null
)
