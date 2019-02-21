package com.avyss.PressurePulsationsRecorder

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.Bundle
import android.os.CountDownTimer
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.os.SystemClock
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

import com.avyss.PressurePulsationsRecorder.acquisition.PressureCollectingListener
import com.avyss.PressurePulsationsRecorder.acquisition.RecordingDetails
import com.avyss.PressurePulsationsRecorder.acquisition.SpeedCollectingListener
import com.avyss.PressurePulsationsRecorder.exporting.Exporter

import java.io.IOException
import java.util.Date
import kotlin.math.roundToInt

class MainActivity : Activity() {

    //private SimulationView mSimulationView;
    private var sensorManager: SensorManager? = null
    private var locationManager: LocationManager? = null
    private var wakeLock: WakeLock? = null
    private var recDetails: RecordingDetails? = null

    private var pressureCollector: PressureCollectingListener? = null
    private var speedCollector: SpeedCollectingListener? = null
    private var speedAvailable: Boolean = false

    private var titleText: EditText? = null
    private var maxRecordingLengthText: EditText? = null
    private var pressureSamplesPerSecondText: EditText? = null
    private var speedSamplesPerSecondText: EditText? = null
    private var startRecordingButton: Button? = null
    private var finishRecordingButton: Button? = null
    private var recordingLabel: TextView? = null
    private var countdownTimer: CountDownTimer? = null
    private var progressBar: ProgressBar? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.name)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        maxRecordingLengthText = findViewById<View>(R.id.MAX_RECORDING_TIME) as EditText
        titleText = findViewById<View>(R.id.TITLE) as EditText
        pressureSamplesPerSecondText = findViewById<View>(R.id.PRESSURE_SAMPLES_PER_SECOND) as EditText
        speedSamplesPerSecondText = findViewById<View>(R.id.SPEED_SAMPLES_PER_SECOND) as EditText
        startRecordingButton = findViewById<View>(R.id.START_RECORDING) as Button
        finishRecordingButton = findViewById<View>(R.id.FINISH_RECORDING) as Button
        recordingLabel = findViewById<View>(R.id.RECORDING_LABEL) as TextView
        progressBar = findViewById<View>(R.id.PROGRESS_BAR) as ProgressBar

        titleText!!.setText(DEFAULT_RECORDING_TITLE)
        maxRecordingLengthText!!.setText(DEFAULT_MAX_SAMPLING_TIME_MINUTES.toString())
        pressureSamplesPerSecondText!!.setText(DEFAULT_PRESSURE_SAMPLES_PER_SECOND.toString())
        speedSamplesPerSecondText!!.setText(DEFAULT_SPEED_SAMPLES_PER_SECOND.toString())
        recordingLabel!!.isEnabled = false
        progressBar!!.isEnabled = false

        startRecordingButton!!.setOnClickListener { doStartRecording() }
        startRecordingButton!!.isEnabled = true

        finishRecordingButton!!.setOnClickListener { doStopRecording() }
        finishRecordingButton!!.isEnabled = false
    }

    override fun onResume() {
        super.onResume()

        askForPermission(Manifest.permission.ACCESS_FINE_LOCATION, 1)
    }

    override fun onPause() {
        super.onPause()
    }

    private fun askForPermission(permission: String, requestCode: Int?) {
        if (baseContext.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, permission)) {

                Toast.makeText(this,
                        "$permission needed to obtain the speed data",
                        Toast.LENGTH_SHORT)
                        .show()

                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode!!)

            } else {

                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode!!)
            }
        }
    }

    private fun doStartRecording() {
        val maxRecordingLengthSec = ((maxRecordingLengthText!!.text.toString()).toFloat() * 60).roundToInt()
        val pressureSamplesPerSecond = (pressureSamplesPerSecondText!!.text.toString()).toFloat()
        val speedSamplesPerSecond = (speedSamplesPerSecondText!!.text.toString()).toFloat()

        recDetails = RecordingDetails(Date())
        val recStartTimeNanos = SystemClock.elapsedRealtimeNanos()

        wakeLock!!.acquire()

        pressureCollector = PressureCollectingListener(
                pressureSamplesPerSecond,
                maxRecordingLengthSec,
                recStartTimeNanos
        )

        sensorManager!!.registerListener(
                pressureCollector,
                sensorManager!!.getDefaultSensor(Sensor.TYPE_PRESSURE),
                SensorManager.SENSOR_DELAY_FASTEST)

        speedCollector = SpeedCollectingListener(
                speedSamplesPerSecond,
                maxRecordingLengthSec,
                recStartTimeNanos
        )

        try {
            locationManager!!.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    Math.round(1000f / speedSamplesPerSecond / 2f).toLong(),
                    0f,
                    speedCollector)
            speedAvailable = true
        } catch (e: SecurityException) {
            Log.e("init", "no permission to read GPS, so no velocity info")
            speedAvailable = false
        }

        maxRecordingLengthText!!.isEnabled = false
        pressureSamplesPerSecondText!!.isEnabled = false
        speedSamplesPerSecondText!!.isEnabled = false
        startRecordingButton!!.isEnabled = false
        finishRecordingButton!!.isEnabled = true
        progressBar!!.max = maxRecordingLengthSec
        recordingLabel!!.isEnabled = true
        progressBar!!.progress = 0
        progressBar!!.isEnabled = true

        val maxProgressMillis = maxRecordingLengthSec * 1000L

        countdownTimer = object: CountDownTimer(maxProgressMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                progressBar?.progress = ((maxProgressMillis - millisUntilFinished)/1000).toInt()
            }
            override fun onFinish() {
                doStopRecording()
            }
        }
        countdownTimer!!.start()

    }

    private fun doStopRecording() {
        wakeLock!!.release()

        sensorManager!!.unregisterListener(pressureCollector)
        if (speedAvailable) {
            locationManager!!.removeUpdates(speedCollector)
        }

        val title = titleText!!.text.toString()
        recDetails!!.title = title

        val exporter = Exporter(this@MainActivity)

        try {
            exporter.exportResults(
                    baseContext,
                    recDetails!!,
                    pressureCollector!!,
                    speedCollector!!
            )
        } catch (e: IOException) {
            Log.e("export", "can't export results", e)
            throw RuntimeException("can't export results", e)
        }

        pressureCollector = null
        speedCollector = null
        recDetails = null

        countdownTimer?.cancel()
        countdownTimer = null

        maxRecordingLengthText!!.isEnabled = true
        pressureSamplesPerSecondText!!.isEnabled = true
        speedSamplesPerSecondText!!.isEnabled = true
        startRecordingButton!!.isEnabled = true
        finishRecordingButton!!.isEnabled = false
        recordingLabel!!.isEnabled = false
        progressBar!!.progress = 0
        progressBar!!.isEnabled = false
    }

    companion object {
        private const val DEFAULT_RECORDING_TITLE = "driving around"
        private const val DEFAULT_MAX_SAMPLING_TIME_MINUTES = 60
        private const val DEFAULT_PRESSURE_SAMPLES_PER_SECOND = 60.0f
        private const val DEFAULT_SPEED_SAMPLES_PER_SECOND = 1.0f
    }
}
