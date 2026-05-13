package com.viplove.licadvisornative.model

data class Circular(
    var circularId: String = "",
    var title: String = "",
    var category: String = "",
    var tags: List<String> = emptyList(),
    var description: String = "",
    var fileUrl: String = "",
    var fileName: String = "",
    var fileType: String = "",
    var fileExtension: String = "",
    var fileSizeBytes: Long = 0L,
    var uploadedBy: String = "",
    var uploadedByName: String = "",
    var uploadedAt: Long = System.currentTimeMillis(),
    var searchTerms: List<String> = emptyList(),
    var language: String = "",
    var thumbnailUrl: String? = null
)
