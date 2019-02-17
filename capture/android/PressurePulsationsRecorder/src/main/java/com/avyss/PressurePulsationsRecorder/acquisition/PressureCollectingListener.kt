package com.avyss.PressurePulsationsRecorder.acquisition

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener

import java.util.function.Consumer

class PressureCollectingListener(
        samplesPerSecond: Float,
        maxRecordingLengthSec: Int,
        recStartTimeNanos: Long,
        private val progressListener: Consumer<Int>
) : AbstractSampleCollector(samplesPerSecond, maxRecordingLengthSec, recStartTimeNanos), SensorEventListener {

    private var lastSecondsProgress: Int = 0

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_PRESSURE) {
            return
        }

        val elapsedSeconds = onSampleAcquired(event.values[0], event.timestamp)

        if (elapsedSeconds != null) {
            val currSecondsProgress = Math.floor(elapsedSeconds.toDouble()).toInt()
            if (lastSecondsProgress != currSecondsProgress) {
                lastSecondsProgress = currSecondsProgress
                progressListener.accept(currSecondsProgress)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}
