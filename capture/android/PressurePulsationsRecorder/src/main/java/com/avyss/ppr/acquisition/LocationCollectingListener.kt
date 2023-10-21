package com.avyss.ppr.acquisition

import android.location.Location
import android.location.LocationListener
import com.avyss.ppr.data.NamedExportableData
import com.avyss.ppr.data.NamedExportableValuesLine
import com.avyss.ppr.data.RateAccommodatingSampleCollector

class LocationCollectingListener(
    private val samplesPerSecond: Float,
    maxRecordingLengthSec: Int,
    recStartTimeNanos: Long
) : LocationListener {

    companion object {
        val SPEED_COLUMNS_NAMES: Array<String> = arrayOf("time [sec]", "speed [km/h]")
        val BEARING_COLUMNS_NAMES: Array<String> = arrayOf("time [sec]", "bearing [deg]")
        val SAMPLING_RATE_COLUMNS_NAMES = arrayOf("sampling_rate [Hz]")
    }

    var dataCount: Int = 0

    private val speedCollector = RateAccommodatingSampleCollector(
        samplesPerSecond,
        maxRecordingLengthSec,
        recStartTimeNanos,
        1
    )

    private val bearingCollector = RateAccommodatingSampleCollector(
        samplesPerSecond,
        maxRecordingLengthSec,
        recStartTimeNanos,
        1
    )

    override fun onLocationChanged(location: Location) {
        dataCount++

        if (location.hasSpeed()) {
            speedCollector.onSampleAcquired(location.elapsedRealtimeNanos, location.speed)
        }
        if (location.hasBearing()) {
            bearingCollector.onSampleAcquired(location.elapsedRealtimeNanos, location.bearing)
        }
    }

    override fun onProviderEnabled(provider: String) {}

    override fun onProviderDisabled(provider: String) {}

    fun exportableSpeedSamples(): NamedExportableData {
        return speedCollector.getExportable().withNames(SPEED_COLUMNS_NAMES)
    }

    fun exportableBearingSamples(): NamedExportableData {
        return bearingCollector.getExportable().withNames(BEARING_COLUMNS_NAMES)
    }

    fun exportableFs(): NamedExportableData {
        return NamedExportableValuesLine(
            SAMPLING_RATE_COLUMNS_NAMES,
            floatArrayOf(samplesPerSecond)
        )
    }
}
