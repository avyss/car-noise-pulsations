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
        val SPEED_COLUMNS_NAMES: Array<String> = arrayOf("time", "speed")
        val BEARING_COLUMNS_NAMES: Array<String> = arrayOf("time", "bearing")
        val SAMPLING_RATE_COLUMNS_NAMES = arrayOf("sampling_rate")
    }

    private val speedCollector = RateAccommodatingSampleCollector(
            samplesPerSecond,
            maxRecordingLengthSec,
            recStartTimeNanos,
            1)

    private val bearingCollector = RateAccommodatingSampleCollector(
            samplesPerSecond,
            maxRecordingLengthSec,
            recStartTimeNanos,
            1)

    override fun onLocationChanged(location: Location) {
        if (location.hasSpeed()) {
            speedCollector.onSampleAcquired(location.elapsedRealtimeNanos, location.speed)
        }
        if (location.hasBearing()) {
            bearingCollector.onSampleAcquired(location.elapsedRealtimeNanos, location.bearing)
        }
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

    override fun onProviderEnabled(provider: String) {}

    override fun onProviderDisabled(provider: String) {}

    fun exportableSpeedSamples(): NamedExportableData {
        return speedCollector.getExportable().withNames(SPEED_COLUMNS_NAMES)
    }

    fun exportableBearingSamples(): NamedExportableData {
        return bearingCollector.getExportable().withNames(BEARING_COLUMNS_NAMES)
    }

    fun exportableFs(): NamedExportableData {
        return NamedExportableValuesLine(SAMPLING_RATE_COLUMNS_NAMES, floatArrayOf(samplesPerSecond))
    }
}
