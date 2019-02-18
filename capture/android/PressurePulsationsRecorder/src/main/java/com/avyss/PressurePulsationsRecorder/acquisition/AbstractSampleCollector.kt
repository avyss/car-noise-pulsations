package com.avyss.PressurePulsationsRecorder.acquisition

import android.util.Log

abstract class AbstractSampleCollector(
        val samplingRate: Float,
        maxRecordingLengthSec: Int,
        private val recStartTimeNanos: Long
) {

    @Volatile
    private var collectionFinished: Boolean = false

    private val maxSamples: Int = Math.round(maxRecordingLengthSec * samplingRate)
    private val counts: IntArray = IntArray(maxSamples)
    private val sums: FloatArray = FloatArray(maxSamples)
    private var firstSampleTimeNanos: Long = -1
    private var lastSampleIndex: Int = -1

    // no samples captured?
    // some samples were captured
    val collectedSamples: FloatArray
        get() {

            if (!collectionFinished) {
                throw IllegalStateException("samples are still being collected, call stopCollecting() first")
            }
            if (lastSampleIndex < 0) {
                return FloatArray(0)
            }
            val samples = FloatArray(lastSampleIndex + 1)

            for (i in 0 until lastSampleIndex + 1) {
                if (counts[i] == 0) {
                    samples[i] = java.lang.Float.NaN
                } else {
                    val currValue = sums[i] / counts[i]
                    samples[i] = currValue
                }
            }

            return samples
        }

    fun stopCollecting() {
        collectionFinished = true
    }

    fun firstSampleTimeDelay(): Float {
        if (!collectionFinished) {
            throw IllegalStateException("samples are still being collected, call stopCollecting() first")
        }

        if (lastSampleIndex < 0) {
            // no samples captured
            return java.lang.Float.NaN
        } else {
            // some samples were captured
            return (firstSampleTimeNanos - recStartTimeNanos).toFloat() / NANOS_IN_SECOND
        }

    }

    protected fun onSampleAcquired(sampleValue: Float, timestamp: Long) {

        if (collectionFinished) {
            return
        }

        if (firstSampleTimeNanos == -1L) {
            firstSampleTimeNanos = timestamp
        }

        val elapsedSeconds = (timestamp - firstSampleTimeNanos).toFloat() / NANOS_IN_SECOND

        val sampleIndex = Math.round(elapsedSeconds * samplingRate)

        Log.v("received", "index $sampleIndex value $sampleValue")

        if (sampleIndex < 0 || sampleIndex >= maxSamples) {
            return
        }

        sums[sampleIndex] += sampleValue
        counts[sampleIndex]++

        lastSampleIndex = sampleIndex
    }

    companion object {
        private const val NANOS_IN_SECOND: Long = 1000L * 1000L * 1000L
    }

}
