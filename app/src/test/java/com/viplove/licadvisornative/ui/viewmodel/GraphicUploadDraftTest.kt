package com.viplove.licadvisornative.ui.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GraphicUploadDraftTest {

    private val image = PickedImage(File("template.png"), "image/png", "png")
    private val ready = GraphicUploadDraft(name = "Diwali offer", visibleToRole = GraphicUploadDraft.ROLE_ADVISOR, image = image)

    @Test
    fun cannotUploadWithoutImageOrName() {
        assertNull(GraphicUploadDraft(name = "x").startUpload())
        assertNull(GraphicUploadDraft(image = image).startUpload())
        assertNull(GraphicUploadDraft(name = "   ", image = image).startUpload())
    }

    @Test
    fun uploadingBlocksEditsAndSecondUpload() {
        val uploading = ready.startUpload()!!
        assertTrue(uploading.isUploading)
        assertFalse(uploading.canUpload)
        assertNull(uploading.startUpload())
        assertEquals(uploading, uploading.withName("changed").withRole(GraphicUploadDraft.ROLE_ALL).withImage(null))
    }

    @Test
    fun failedUploadKeepsTheForm() {
        val failed = ready.startUpload()!!.uploadFailed()
        assertEquals(ready, failed)
        assertTrue(failed.canUpload)
    }

    @Test
    fun successfulUploadClearsTheForm() {
        assertEquals(GraphicUploadDraft(), ready.startUpload()!!.uploadSucceeded())
    }
}
