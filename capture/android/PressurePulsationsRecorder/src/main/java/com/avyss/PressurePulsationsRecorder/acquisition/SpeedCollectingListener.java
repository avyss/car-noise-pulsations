package com.avyss.PressurePulsationsRecorder.acquisition;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public class SpeedCollectingListener extends AbstractSampleCollector implements LocationListener {

    public SpeedCollectingListener(float samplesPerSecond, int maxRecordingLengthSec, long recStartTimeNanos) {
        super(samplesPerSecond, maxRecordingLengthSec, recStartTimeNanos);
    }

    @Override
    public void onLocationChanged(Location location) {
        onSampleAcquired(location.getSpeed(), location.getElapsedRealtimeNanos());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

 }
