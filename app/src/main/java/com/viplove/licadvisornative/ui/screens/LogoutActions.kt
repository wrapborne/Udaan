package com.viplove.licadvisornative.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.viplove.licadvisornative.MainActivity

fun restartAppAfterLogout(context: Context) {
    val intent = Intent(context, MainActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    context.startActivity(intent)
    (context as? Activity)?.finish()
}
