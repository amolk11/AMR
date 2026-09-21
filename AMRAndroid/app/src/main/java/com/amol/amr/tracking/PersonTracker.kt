package com.amol.amr.tracking

import com.amol.amr.detection.BoundingBox
import com.amol.amr.detection.Detection
import com.amol.amr.navigation.PersonDetection
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class TrackingConfig(
    val iouMatchingThreshold: Float = 0.30f,
    val maxDistanceThresholdPixels: Float = 150.0f,
    val maxMissedFrames: Int = 10
)

data class TrackedPerson(
    val trackId: Int,
    var boundingBox: BoundingBox,
    var confidence: Float,
    var missedFrames: Int = 0,
    var totalHits: Int = 1
)

class PersonTracker(
    private val config: TrackingConfig = TrackingConfig()
) {
    private var nextTrackId = 1
    private val activeTracks = mutableListOf<TrackedPerson>()

    fun update(detections: List<Detection>): List<PersonDetection> {
        val matchedDetectionIndices = mutableSetOf<Int>()
        val matchedTrackIndices = mutableSetOf<Int>()

        // 1. Associate existing tracks with detections based on IoU and Distance
        for ((trackIdx, track) in activeTracks.withIndex()) {
            var bestDetIdx = -1
            var bestScore = 0.0f

            for ((detIdx, det) in detections.withIndex()) {
                if (detIdx in matchedDetectionIndices) continue

                val iou = computeIoU(track.boundingBox, det.boundingBox)
                val dist = hypot(track.boundingBox.centerX - det.boundingBox.centerX,
                    track.boundingBox.centerY - det.boundingBox.centerY)

                if (iou >= config.iouMatchingThreshold || dist <= config.maxDistanceThresholdPixels) {
                    val score = iou + (1.0f / (1.0f + dist * 0.01f))
                    if (score > bestScore) {
                        bestScore = score
                        bestDetIdx = detIdx
                    }
                }
            }

            if (bestDetIdx != -1) {
                val matchedDet = detections[bestDetIdx]
                track.boundingBox = matchedDet.boundingBox
                track.confidence = matchedDet.confidence
                track.missedFrames = 0
                track.totalHits++
                matchedDetectionIndices.add(bestDetIdx)
                matchedTrackIndices.add(trackIdx)
            } else {
                track.missedFrames++
            }
        }

        // 2. Spawn new tracks for unmatched detections
        for ((detIdx, det) in detections.withIndex()) {
            if (detIdx !in matchedDetectionIndices) {
                val newTrack = TrackedPerson(
                    trackId = nextTrackId++,
                    boundingBox = det.boundingBox,
                    confidence = det.confidence
                )
                activeTracks.add(newTrack)
            }
        }

        // 3. Remove tracks that have been lost for too long
        activeTracks.removeAll { it.missedFrames > config.maxMissedFrames }

        // 4. Return formatted PersonDetection list with assigned IDs
        return activeTracks.filter { it.missedFrames == 0 }.map { track ->
            PersonDetection(
                x1 = track.boundingBox.x1,
                y1 = track.boundingBox.y1,
                x2 = track.boundingBox.x2,
                y2 = track.boundingBox.y2,
                trackId = track.trackId,
                conf = track.confidence
            )
        }
    }

    private fun computeIoU(b1: BoundingBox, b2: BoundingBox): Float {
        val interX1 = max(b1.x1, b2.x1)
        val interY1 = max(b1.y1, b2.y1)
        val interX2 = min(b1.x2, b2.x2)
        val interY2 = min(b1.y2, b2.y2)

        val interW = max(0.0f, interX2 - interX1)
        val interH = max(0.0f, interY2 - interY1)
        val interArea = interW * interH

        val unionArea = b1.area + b2.area - interArea
        return if (unionArea > 0.0f) interArea / unionArea else 0.0f
    }

    fun reset() {
        activeTracks.clear()
        nextTrackId = 1
    }
}
