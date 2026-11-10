package com.viplove.licadvisornative.ui.viewmodel

import java.io.File

/** An image the superadmin picked, copied into app storage with its real type. */
data class PickedImage(val file: File, val mimeType: String, val extension: String)

/**
 * One upload form on the graphics management screen (template or footer). See STATE_MACHINES.md.
 *
 * ```
 * Editing ──startUpload (name + image)──▶ Uploading ──succeeded──▶ Editing (empty form)
 *    ▲                                         │
 *    └──────────────── failed (form kept) ─────┘
 * ```
 *
 * The typed name, role and picked image stay in the form until the upload succeeds, so a
 * failed upload can be retried without re-entering anything.
 */
data class GraphicUploadDraft(
    val name: String = "",
    val visibleToRole: String = ROLE_ALL,
    val image: PickedImage? = null,
    val isUploading: Boolean = false
) {
    val canUpload: Boolean get() = !isUploading && image != null && name.isNotBlank()

    fun withName(value: String): GraphicUploadDraft = if (isUploading) this else copy(name = value)
    fun withRole(value: String): GraphicUploadDraft = if (isUploading) this else copy(visibleToRole = value)
    fun withImage(value: PickedImage?): GraphicUploadDraft = if (isUploading) this else copy(image = value)

    /** Null when the upload can't start: already uploading, no image, or no name. */
    fun startUpload(): GraphicUploadDraft? = if (canUpload) copy(isUploading = true) else null

    fun uploadSucceeded(): GraphicUploadDraft = GraphicUploadDraft()
    fun uploadFailed(): GraphicUploadDraft = copy(isUploading = false)

    companion object {
        const val ROLE_ALL = "all"
        const val ROLE_ADMIN = "admin"
        const val ROLE_ADVISOR = "advisor"
    }
}
