package com.avyss.PressurePulsationsRecorder.acquisition

import android.util.Log

abstract class AbstractSampleCollector(
        private val nValuesPerSample: Int,
        val samplingRate: Float,
        maxRecordingLengthSec: Int,
        private val recStartTimeNanos: Long
) {

    @Volatile
    private var collectionFinished: Boolean = false

    private val maxSamples = Math.round(maxRecordingLengthSec * samplingRate)
    private val counts = IntArray(maxSamples)
    private val sums = Array(nValuesPerSample) {FloatArray(maxSamples)}
    private var firstSampleTimeNanos: Long = -1
    private var lastSampleIndex: Int = -1

    // no samples captured?
    // some samples were captured
    val collectedSamples: Array<FloatArray>
        get() {

            if (!collectionFinished) {
                throw IllegalStateException("samples are still being collected, call stopCollecting() first")
            }
            if (lastSampleIndex < 0) {
                return Array(0, {FloatArray(0)})
            }

            val samples = Array(nValuesPerSample, {FloatArray(lastSampleIndex + 1)})

            for (i in 0..lastSampleIndex) {
                for (n in 0 until nValuesPerSample) {
                    if (counts[i] == 0) {
                        samples[n][i] = java.lang.Float.NaN
                    } else {
                        samples[n][i] = sums[n][i] / counts[i]
                    }
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

    protected fun onSampleAcquired(timestamp: Long, vararg sampledValues: Float) {

        if (collectionFinished) {
            return
        }

        if (sampledValues.size != nValuesPerSample) {
            throw IllegalArgumentException()
        }

        if (firstSampleTimeNanos == -1L) {
            firstSampleTimeNanos = timestamp
        }

        val elapsedSeconds = (timestamp - firstSampleTimeNanos).toFloat() / NANOS_IN_SECOND

        val sampleIndex = Math.round(elapsedSeconds * samplingRate)

        Log.v("received", "index $sampleIndex value " + sampledValues.joinToString { f->f.toString() })

        if (sampleIndex < 0 || sampleIndex >= maxSamples) {
            return
        }

        for (n in 0 until nValuesPerSample) {
            sums[n][sampleIndex] += sampledValues[n]
        }
        counts[sampleIndex]++

        lastSampleIndex = sampleIndex
    }

    companion object {
        private const val NANOS_IN_SECOND: Long = 1000L * 1000L * 1000L
    }

}
