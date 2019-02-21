package com.avyss.PressurePulsationsRecorder.data

import android.util.Log

/**
 * Collects samples and assigns them to eqi-sized time slots, thus producing
 * constant-rate time series from arbitrarily-timed set of samples.
 * <p>
 * Values for time slots having multiple samples are averaged. <br>
 * Values for time slots having no samples are returned as NaNs.
 * <p>
 * Each sample contains a vector of (at least one) float values.
 */
class RateAccommodatingSampleCollector(
        val samplesPerSecond: Float,
        maxRecordingLengthSec: Int,
        private val recStartTimeNanos: Long,
        private val nValuesPerSample: Int
) {

    companion object {
        private const val NANOS_IN_SECOND: Long = 1000L * 1000L * 1000L
    }

    private val maxNSamples = Math.round(maxRecordingLengthSec * samplesPerSecond)
    private val counts      = IntArray(maxNSamples)
    private val sums        = Array(nValuesPerSample) {FloatArray(maxNSamples)}

    fun onSampleAcquired(timestamp: Long, vararg sampledValues: Float) {

        if (sampledValues.size != nValuesPerSample) {
            throw IllegalArgumentException()
        }

        val elapsedSeconds = (timestamp - recStartTimeNanos).toFloat() / NANOS_IN_SECOND

        val sampleIndex = Math.round(elapsedSeconds * samplesPerSecond)

        Log.v("received", "index $sampleIndex value " + sampledValues.joinToString { f->f.toString() })

        if (sampleIndex < 0 || sampleIndex >= maxNSamples) {
            return
        }

        for (n in 0 until nValuesPerSample) {
            sums[n][sampleIndex] += sampledValues[n]
        }

        counts[sampleIndex]++
    }

    fun getExportable(): ExportableData {
        return object: ExportableData {

            override val rowsIterator: Iterator<FloatArray>
                get() = NonemptySampleRangeIterator()

        }
    }

    private inner class NonemptySampleRangeIterator: Iterator<FloatArray> {

        private var currSampleIdx: Int
        private var lastSampleIdx: Int

        init {

            // find index of first slot having a sample
            var idx = 0
            while ((idx < maxNSamples) && (counts[idx] == 0)) {
                idx++
            }
            currSampleIdx = idx

            if (currSampleIdx == maxNSamples) {
                // if all slots are empty - the iterator will return no data
                lastSampleIdx  = currSampleIdx - 1
            } else {

                // find index of last slot having a sample
                // (it may be the same one as the first slot, if only one sample has arrived)
                lastSampleIdx = currSampleIdx
                while (idx < maxNSamples) {
                    if (counts[idx] != 0) {
                        lastSampleIdx = idx
                    }
                    idx++
                }
            }

        }

        override fun hasNext(): Boolean {
            return currSampleIdx <= lastSampleIdx
        }

        override fun next(): FloatArray {
            val retData = FloatArray(nValuesPerSample + 1)

            val time = currSampleIdx.toFloat() / samplesPerSecond
            retData[0] = time
            for (n in 0 until nValuesPerSample) {
                if (counts[currSampleIdx] == 0) {
                    retData[n+1] = java.lang.Float.NaN
                } else {
                    retData[n+1]= sums[n][currSampleIdx] / counts[currSampleIdx]
                }
            }

            currSampleIdx++

            return retData
        }

    }

}
