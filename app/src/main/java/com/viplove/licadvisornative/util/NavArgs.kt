package com.viplove.licadvisornative.util

import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object NavArgs {
    /**
     * Decodes a URL passed as a navigation argument exactly once.
     *
     * Navigation already decodes route arguments, so a second decode turns the `%2F` inside
     * Firebase Storage URLs into `/` and breaks them. A value that already looks like a URL
     * is returned as is; anything else is decoded once.
     */
    fun decodeUrl(raw: String): String {
        if (raw.isBlank() || raw.startsWith("http://") || raw.startsWith("https://")) return raw
        return try {
            URLDecoder.decode(raw, StandardCharsets.UTF_8.toString())
        } catch (e: IllegalArgumentException) {
            raw
        }
    }
}
