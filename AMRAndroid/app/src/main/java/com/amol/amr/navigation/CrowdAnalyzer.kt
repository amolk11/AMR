package com.amol.amr.navigation

enum class CrowdDensity(val value: String) {
    EMPTY("EMPTY"),
    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH")
}

object CrowdAnalyzer {
    fun analyzeCrowdDensity(peopleCount: Int): CrowdDensity {
        return when {
            peopleCount <= 0 -> CrowdDensity.EMPTY
            peopleCount <= 2 -> CrowdDensity.LOW
            peopleCount <= 4 -> CrowdDensity.MEDIUM
            else -> CrowdDensity.HIGH
        }
    }
}
