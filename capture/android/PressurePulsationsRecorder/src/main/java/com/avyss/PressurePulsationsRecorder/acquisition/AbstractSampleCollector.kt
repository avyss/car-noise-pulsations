package com.avyss.PressurePulsationsRecorder.acquisition

import android.util.Log

abstract class AbstractSampleCollector(
        private val nValuesPerSample: Int,
        val samplingRate: Float,
        maxRecordingLengthSec: Int,
        private val recStartTimeNanos: Long
) {

    companion object {
        private const val NANOS_IN_SECOND: Long = 1000L * 1000L * 1000L
    }

    @Volatile
    private var collectionFinished: Boolean = false

    private val maxNSamples = Math.round(maxRecordingLengthSec * samplingRate)
    private val counts      = IntArray(maxNSamples)
    private val sums        = Array(nValuesPerSample) {FloatArray(maxNSamples)}

    val collectedData: Array<FloatArray>
        get() {

            if (!collectionFinished) {
                throw IllegalStateException("samples are still being collected, call stopCollecting() first")
            }

            // find index of first actual sample
            var idx = 0;
            while (idx < maxNSamples && counts[idx] == 0) {
                idx++;
            }
            if (idx == maxNSamples) {
                return Array(0, {FloatArray(0)})
            }
            val firstSampleIdx = idx;

            // find index of last actual sample
            // (it may be the same one as the first, if there only one sample has arrived)
            var lastSampleIdx = firstSampleIdx;
            while (idx < maxNSamples) {
                if (counts[idx] != 0) {
                    lastSampleIdx = idx;
                }
                idx++;
            }

            val retData = Array(1 + nValuesPerSample, {FloatArray(lastSampleIdx - firstSampleIdx + 1)})

            for (i in firstSampleIdx .. lastSampleIdx) {
                val time = i.toFloat() / samplingRate
                retData[0][i] = time
                for (n in 0 until nValuesPerSample) {
                    if (counts[i] == 0) {
                        retData[n+1][i] = java.lang.Float.NaN
                    } else {
                        retData[n+1][i] = sums[n][i] / counts[i]
                    }
                }
            }

            return retData
        }

    fun stopCollecting() {
        collectionFinished = true
    }

    protected fun onSampleAcquired(timestamp: Long, vararg sampledValues: Float) {

        if (collectionFinished) {
            return
        }

        if (sampledValues.size != nValuesPerSample) {
            throw IllegalArgumentException()
        }

        val elapsedSeconds = (timestamp - recStartTimeNanos).toFloat() / NANOS_IN_SECOND

        val sampleIndex = Math.round(elapsedSeconds * samplingRate)

        Log.v("received", "index $sampleIndex value " + sampledValues.joinToString { f->f.toString() })

        if (sampleIndex < 0 || sampleIndex >= maxNSamples) {
            return
        }

        for (n in 0 until nValuesPerSample) {
            sums[n][sampleIndex] += sampledValues[n]
        }

        counts[sampleIndex]++
    }

}
