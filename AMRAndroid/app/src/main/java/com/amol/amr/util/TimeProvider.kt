package com.amol.amr.util

import android.os.SystemClock

/**
 * Provides high-resolution monotonic time (in seconds and milliseconds)
 * independent of system clock / timezone shifts.
 */
interface TimeProvider {
    fun currentTimeSeconds(): Double
    fun currentTimeMillis(): Long
}

class SystemTimeProvider : TimeProvider {
    override fun currentTimeSeconds(): Double {
        return SystemClock.elapsedRealtimeNanos() / 1_000_000_000.0
    }

    override fun currentTimeMillis(): Long {
        return SystemClock.elapsedRealtime()
    }
}
