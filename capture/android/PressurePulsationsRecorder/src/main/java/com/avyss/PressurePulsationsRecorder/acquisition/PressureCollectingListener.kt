package com.avyss.PressurePulsationsRecorder.acquisition

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener

class PressureCollectingListener(
        samplesPerSecond: Float,
        maxRecordingLengthSec: Int,
        recStartTimeNanos: Long
) : AbstractSampleCollector(1, samplesPerSecond, maxRecordingLengthSec, recStartTimeNanos), SensorEventListener {

    companion object {
        val COLLECTED_VALUES_NAMES: Array<String> = arrayOf("time", "pressure")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_PRESSURE) {
            return
        }

        onSampleAcquired(event.timestamp, event.values[0])
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}
