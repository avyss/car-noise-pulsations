package com.avyss.PressurePulsationsRecorder.acquisition

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import com.avyss.PressurePulsationsRecorder.data.NamedExportableData
import com.avyss.PressurePulsationsRecorder.data.NamedExportableValuesLine
import com.avyss.PressurePulsationsRecorder.data.RateAccommodatingSampleCollector

class PressureCollectingListener(
        private val samplesPerSecond: Float,
        maxRecordingLengthSec: Int,
        recStartTimeNanos: Long
): SensorEventListener {

    var dataCount: Int = 0

    private val sampleCollector = RateAccommodatingSampleCollector(
            samplesPerSecond,
            maxRecordingLengthSec,
            recStartTimeNanos,
            1)

    companion object {
        val SAMPLES_COLUMNS_NAMES = arrayOf("time [sec]", "pressure [hPa]")
        val SAMPLING_RATE_COLUMNS_NAMES = arrayOf("sampling_rate [Hz]")
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_PRESSURE) {
            return
        }
        dataCount++

        sampleCollector.onSampleAcquired(event.timestamp, event.values[0])
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

    fun exportablePressureSamples(): NamedExportableData {
        return sampleCollector.getExportable().withNames(SAMPLES_COLUMNS_NAMES)
    }

    fun exportableFs(): NamedExportableData {
        return NamedExportableValuesLine(SAMPLING_RATE_COLUMNS_NAMES, floatArrayOf(samplesPerSecond))
    }

}
