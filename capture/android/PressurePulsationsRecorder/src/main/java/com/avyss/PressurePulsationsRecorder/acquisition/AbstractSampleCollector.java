package com.avyss.PressurePulsationsRecorder.acquisition;

import android.os.SystemClock;
import android.util.Log;

abstract public class AbstractSampleCollector {

    private static final long NANOS_IN_SECOND = 1000*1000*1000;

    final private float samplesPerSecond;
    final private int   maxSamples;
    final private long  recStartTimeNanos;

    private volatile boolean collectionFinished;
    private long firstSampleTimeNanos;
    private int  lastSampleIndex;
    private int   [] counts;
    private float [] sums;

    public AbstractSampleCollector(float samplesPerSecond, int maxRecordingLengthSec, long recStartTimeNanos) {

        this.samplesPerSecond = samplesPerSecond;
        this.maxSamples = Math.round(maxRecordingLengthSec * samplesPerSecond);
        this.recStartTimeNanos = SystemClock.elapsedRealtimeNanos();

        firstSampleTimeNanos = -1;
        lastSampleIndex = -1;

        counts = new int[maxSamples];
        sums = new float[maxSamples];

        collectionFinished = false;
    }

    public void stopCollecting() {
        collectionFinished = true;
    }

    public boolean hasSamples() {
        if (!collectionFinished) {
            throw new IllegalStateException("samples are still being collected, call stopCollecting() first");
        }

        return (firstSampleTimeNanos != -1);
    }

    public float [] getCollectedSamples() {

        if (!collectionFinished) {
            throw new IllegalStateException("samples are still being collected, call stopCollecting() first");
        }

        // no samples captured?
        if (lastSampleIndex < 0) {
            return null;
        }

        // some samples were captured
        float [] samples = new float[lastSampleIndex + 1];

        float prevValue = 0.0f;
        for (int i = 0; i < lastSampleIndex + 1; i++) {
            if (counts[i] == 0) {
                samples[i] = prevValue;
            } else {
                float currValue = sums[i] / counts[i];
                samples[i] = currValue;
                prevValue  = currValue;
            }
        }

        return samples;
    }

    public float firstSampleTimeDelay() {
        if (!collectionFinished) {
            throw new IllegalStateException("samples are still being collected, call stopCollecting() first");
        }

        // no samples captured?
        if (lastSampleIndex < 0) {
            return Float.NaN;
        }

        // some samples were captured
        return (float) (firstSampleTimeNanos - recStartTimeNanos) / NANOS_IN_SECOND;
    }

    public float getSamplingRate() {
        return samplesPerSecond;
    }

    protected Float onSampleAcquired(float sampleValue, long timestamp) {

        if (collectionFinished) {
            return null;
        }

        if (firstSampleTimeNanos == -1) {
            firstSampleTimeNanos = timestamp;
        }

        float elapsedSeconds = (float)(timestamp - firstSampleTimeNanos) / NANOS_IN_SECOND;

        int sampleIndex = Math.round(elapsedSeconds * samplesPerSecond);

        Log.d("received", "index " + sampleIndex + " value " + sampleValue);

        if (sampleIndex < 0 || sampleIndex >= maxSamples) {
            return null;
        }

        sums[sampleIndex] += sampleValue;
        counts[sampleIndex]++;

        lastSampleIndex = sampleIndex;
        return elapsedSeconds;
    }

}
