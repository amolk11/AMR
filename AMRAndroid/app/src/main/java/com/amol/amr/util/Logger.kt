package com.amol.amr.util

import android.util.Log

object Logger {
    var isDebugEnabled: Boolean = true
    private const val DEFAULT_TAG = "AMR_ROBOT"

    fun d(tag: String = DEFAULT_TAG, message: String) {
        if (isDebugEnabled) Log.d(tag, message)
    }

    fun i(tag: String = DEFAULT_TAG, message: String) {
        Log.i(tag, message)
    }

    fun w(tag: String = DEFAULT_TAG, message: String) {
        Log.w(tag, message)
    }

    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
}
