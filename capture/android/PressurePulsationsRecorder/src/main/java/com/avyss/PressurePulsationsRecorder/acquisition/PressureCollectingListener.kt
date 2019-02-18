package com.avyss.PressurePulsationsRecorder.acquisition

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener

import java.util.function.Consumer

class PressureCollectingListener(
        samplesPerSecond: Float,
        maxRecordingLengthSec: Int,
        recStartTimeNanos: Long
) : AbstractSampleCollector(samplesPerSecond, maxRecordingLengthSec, recStartTimeNanos), SensorEventListener {

    private var lastSecondsProgress: Int = 0

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_PRESSURE) {
            return
        }

        onSampleAcquired(event.values[0], event.timestamp)
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}
