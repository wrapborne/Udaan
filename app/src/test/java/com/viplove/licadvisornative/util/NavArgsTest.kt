package com.viplove.licadvisornative.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URLEncoder

class NavArgsTest {

    private val storageUrl =
        "https://firebasestorage.googleapis.com/v0/b/app.appspot.com/o/graphics%2Ftemplates%2Fa.png?alt=media&token=abc"

    @Test
    fun alreadyDecodedUrlIsLeftAlone() {
        // Navigation decodes route arguments itself; decoding again would turn %2F into "/".
        assertEquals(storageUrl, NavArgs.decodeUrl(storageUrl))
    }

    @Test
    fun encodedUrlIsDecodedOnce() {
        assertEquals(storageUrl, NavArgs.decodeUrl(URLEncoder.encode(storageUrl, "UTF-8")))
    }

    @Test
    fun blankStaysBlank() {
        assertEquals("", NavArgs.decodeUrl(""))
    }
}
