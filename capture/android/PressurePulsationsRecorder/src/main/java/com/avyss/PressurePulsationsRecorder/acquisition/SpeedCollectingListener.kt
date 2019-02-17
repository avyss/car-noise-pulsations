package com.avyss.PressurePulsationsRecorder.acquisition

import android.location.Location
import android.location.LocationListener
import android.os.Bundle

class SpeedCollectingListener(
        samplesPerSecond: Float,
        maxRecordingLengthSec: Int,
        recStartTimeNanos: Long
) : AbstractSampleCollector(samplesPerSecond, maxRecordingLengthSec, recStartTimeNanos), LocationListener {

    override fun onLocationChanged(location: Location) {
        onSampleAcquired(location.speed, location.elapsedRealtimeNanos)
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

    override fun onProviderEnabled(provider: String) {}

    override fun onProviderDisabled(provider: String) {}

}
