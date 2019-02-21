package com.avyss.PressurePulsationsRecorder.acquisition

import android.location.Location
import android.location.LocationListener
import android.os.Bundle
import com.avyss.PressurePulsationsRecorder.data.NamedExportableData
import com.avyss.PressurePulsationsRecorder.data.NamedExportableValuesLine
import com.avyss.PressurePulsationsRecorder.data.RateAccommodatingSampleCollector

class SpeedCollectingListener(
        private val samplesPerSecond: Float,
        maxRecordingLengthSec: Int,
        recStartTimeNanos: Long
): LocationListener {

    companion object {
        val SAMPLES_COLUMNS_NAMES: Array<String> = arrayOf("time", "speed", "bearing")
        val SAMPLING_RATE_COLUMNS_NAMES = arrayOf("sampling_rate")
    }

    private val sampleCollector = RateAccommodatingSampleCollector(
            samplesPerSecond,
            maxRecordingLengthSec,
            recStartTimeNanos,
            2)

    override fun onLocationChanged(location: Location) {
        sampleCollector.onSampleAcquired(location.elapsedRealtimeNanos, location.speed, location.bearing)
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

    override fun onProviderEnabled(provider: String) {}

    override fun onProviderDisabled(provider: String) {}

    fun exportableSamples(): NamedExportableData {
        return sampleCollector.getExportable().withNames(SAMPLES_COLUMNS_NAMES)
    }

    fun exportableFs(): NamedExportableData {
        return NamedExportableValuesLine(SAMPLING_RATE_COLUMNS_NAMES, floatArrayOf(samplesPerSecond))
    }
}
