package com.avyss.PressurePulsationsRecorder.acquisition;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import java.util.function.Consumer;

public class PressureCollectingListener extends AbstractSampleCollector implements SensorEventListener {

    private final Consumer<Integer> progressListener;

    private int lastSecondsProgress;

    public PressureCollectingListener(float samplesPerSecond, int maxRecordingLengthSec, long recStartTimeNanos, Consumer<Integer> progressListener) {
        super(samplesPerSecond, maxRecordingLengthSec, recStartTimeNanos);

        this.progressListener = progressListener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if ( event.sensor.getType() != Sensor.TYPE_PRESSURE) {
            return;
        }

        Float elapsedSeconds = onSampleAcquired(event.values[0], event.timestamp);

        if (elapsedSeconds != null) {
            int currSecondsProgress = (int) Math.floor(elapsedSeconds);
            if (lastSecondsProgress != currSecondsProgress) {
                lastSecondsProgress = currSecondsProgress;
                progressListener.accept(currSecondsProgress);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
