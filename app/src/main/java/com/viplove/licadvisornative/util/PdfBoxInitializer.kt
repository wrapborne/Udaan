package com.viplove.licadvisornative.util

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * Loads PDFBox's resources on a background thread at app start, so the first screen doesn't
 * wait for them. Code that uses PDFBox calls [await] first; it returns at once when loading
 * has finished and otherwise waits for it.
 */
object PdfBoxInitializer {
    private val started = AtomicBoolean(false)
    private val ready = CountDownLatch(1)

    fun warmUp(context: Context) {
        if (!started.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        thread(name = "pdfbox-init") {
            try {
                PDFBoxResourceLoader.init(appContext)
            } finally {
                ready.countDown()
            }
        }
    }

    fun await() {
        if (started.get()) ready.await()
    }
}
