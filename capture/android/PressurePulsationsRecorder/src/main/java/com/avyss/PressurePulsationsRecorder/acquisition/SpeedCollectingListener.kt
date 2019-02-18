package com.avyss.PressurePulsationsRecorder.acquisition

import android.location.Location
import android.location.LocationListener
import android.os.Bundle

class SpeedCollectingListener(
        samplesPerSecond: Float,
        maxRecordingLengthSec: Int,
        recStartTimeNanos: Long
) : AbstractSampleCollector(2, samplesPerSecond, maxRecordingLengthSec, recStartTimeNanos), LocationListener {

    companion object {
        val COLLECTED_VALUES_NAMES: Array<String> = arrayOf("time", "speed", "bearing")
    }

    override fun onLocationChanged(location: Location) {
        onSampleAcquired(location.elapsedRealtimeNanos, location.speed, location.bearing)
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

    override fun onProviderEnabled(provider: String) {}

    override fun onProviderDisabled(provider: String) {}

}
