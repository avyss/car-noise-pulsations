package com.avyss.PressurePulsationsRecorder;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.avyss.PressurePulsationsRecorder.acquisition.PressureCollectingListener;
import com.avyss.PressurePulsationsRecorder.acquisition.RecordingDetails;
import com.avyss.PressurePulsationsRecorder.acquisition.SpeedCollectingListener;
import com.avyss.PressurePulsationsRecorder.exporting.Exporter;

import java.util.Date;

public class MainActivity extends Activity {

    private static final String DEFAULT_RECORDING_TITLE = "driving around";
    private static final int    DEFAULT_MAX_SAMPLING_TIME_MINUTES = 60;
    private static final float  DEFAULT_PRESSURE_SAMPLES_PER_SECOND = 60.0f;
    private static final float  DEFAULT_SPEED_SAMPLES_PER_SECOND = 1.0f;

    //private SimulationView mSimulationView;
    private SensorManager sensorManager;
    private LocationManager locationManager;
    private WakeLock wakeLock;
    private RecordingDetails recDetails;

    private PressureCollectingListener pressureCollector;
    private SpeedCollectingListener speedCollector;
    private boolean speedAvailable;

    private EditText titleText;
    private EditText maxRecordingLengthText;
    private EditText pressureSamplesPerSecondText;
    private EditText speedSamplesPerSecondText;
    private Button startRecordingButton;
    private Button finishRecordingButton;
    private TextView recordingLabel;
    private ProgressBar progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        maxRecordingLengthText = (EditText) findViewById(R.id.MAX_RECORDING_TIME);
        titleText = (EditText) findViewById(R.id.TITLE);
        pressureSamplesPerSecondText = (EditText) findViewById(R.id.PRESSURE_SAMPLES_PER_SECOND);
        speedSamplesPerSecondText = (EditText) findViewById(R.id.SPEED_SAMPLES_PER_SECOND);
        startRecordingButton = (Button) findViewById(R.id.START_RECORDING);
        finishRecordingButton = (Button) findViewById(R.id.FINISH_RECORDING);
        recordingLabel = (TextView) findViewById((R.id.RECORDING_LABEL));
        progressBar = (ProgressBar) findViewById(R.id.PROGRESS_BAR);

        titleText.setText(DEFAULT_RECORDING_TITLE);
        maxRecordingLengthText.setText(String.valueOf(DEFAULT_MAX_SAMPLING_TIME_MINUTES));
        pressureSamplesPerSecondText.setText(String.valueOf(DEFAULT_PRESSURE_SAMPLES_PER_SECOND));
        speedSamplesPerSecondText.setText(String.valueOf(DEFAULT_SPEED_SAMPLES_PER_SECOND));
        recordingLabel.setEnabled(false);
        progressBar.setEnabled(false);

        startRecordingButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doStartRecording();
            }
        });
        startRecordingButton.setEnabled(true);

        finishRecordingButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doStopRecording();
            }
        });
        finishRecordingButton.setEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        askForPermission(Manifest.permission.ACCESS_FINE_LOCATION, 1);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void askForPermission(String permission, Integer requestCode) {
        if (getBaseContext().checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission)) {

                Toast.makeText(this,
                        "" + permission + " needed to obtain the speed data",
                        Toast.LENGTH_SHORT)
                     .show();

                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);

            } else {

                ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
            }
        }
    }

    private void doStartRecording() {
        int maxRecordingLentghSec = Integer.parseInt(maxRecordingLengthText.getText().toString()) * 60;
        float pressureSamplesPerSecond = Float.parseFloat(pressureSamplesPerSecondText.getText().toString());
        float speedSamplesPerSecond = Float.parseFloat(speedSamplesPerSecondText.getText().toString());

        recDetails = new RecordingDetails(new Date());
        long recStartTimeNanos = SystemClock.elapsedRealtimeNanos();

        wakeLock.acquire();

        pressureCollector = new PressureCollectingListener(
                pressureSamplesPerSecond,
                maxRecordingLentghSec,
                recStartTimeNanos,
                progressSeconds -> {
                    progressBar.setProgress(progressSeconds);
                }
        );

        sensorManager.registerListener(
                pressureCollector,
                sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE),
                SensorManager.SENSOR_DELAY_FASTEST);

        speedCollector = new SpeedCollectingListener(
                speedSamplesPerSecond,
                maxRecordingLentghSec,
                recStartTimeNanos
        );

        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    Math.round(1000 / speedSamplesPerSecond / 2),
                    0,
                    speedCollector);
            speedAvailable = true;
        }
        catch(SecurityException e) {
            Log.e("init", "no permission to read GPS, so no velocity info");
            speedAvailable = false;
        }

        maxRecordingLengthText.setEnabled(false);
        pressureSamplesPerSecondText.setEnabled(false);
        speedSamplesPerSecondText.setEnabled(false);
        startRecordingButton.setEnabled(false);
        finishRecordingButton.setEnabled(true);
        progressBar.setMax(maxRecordingLentghSec);
        recordingLabel.setEnabled(true);
        progressBar.setProgress(0);
        progressBar.setEnabled(true);
    }

    private void doStopRecording() {
        wakeLock.release();

        pressureCollector.stopCollecting();
        speedCollector.stopCollecting();

        sensorManager.unregisterListener(pressureCollector);
        if (speedAvailable) {
            locationManager.removeUpdates(speedCollector);
        }

        String title = titleText.getText().toString();
        recDetails.setTitle(title);

        Exporter exporter = new Exporter(MainActivity.this);
        exporter.exportResults(
                getBaseContext(),
                recDetails,
                pressureCollector,
                speedCollector);

        pressureCollector = null;
        speedCollector = null;
        recDetails = null;

        maxRecordingLengthText.setEnabled(true);
        pressureSamplesPerSecondText.setEnabled(true);
        speedSamplesPerSecondText.setEnabled(true);
        startRecordingButton.setEnabled(true);
        finishRecordingButton.setEnabled(false);
        recordingLabel.setEnabled(false);
        progressBar.setProgress(0);
        progressBar.setEnabled(false);
    }
}
