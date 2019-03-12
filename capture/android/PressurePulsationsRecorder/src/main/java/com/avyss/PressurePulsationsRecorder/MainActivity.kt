package com.avyss.PressurePulsationsRecorder

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.*
import android.os.PowerManager.WakeLock
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

import com.avyss.PressurePulsationsRecorder.acquisition.PressureCollectingListener
import com.avyss.PressurePulsationsRecorder.acquisition.RecordingDetails
import com.avyss.PressurePulsationsRecorder.acquisition.LocationCollectingListener
import com.avyss.PressurePulsationsRecorder.acquisition.WindDetails
import com.avyss.PressurePulsationsRecorder.exporting.Exporter
import com.avyss.PressurePulsationsRecorder.settings.SettingsActivity

import java.io.IOException
import java.util.Date
import kotlin.math.roundToInt

class MainActivity : Activity() {

    private var sensorManager: SensorManager? = null
    private var locationManager: LocationManager? = null
    private var wakeLock: WakeLock? = null
    private var recDetails: RecordingDetails? = null

    private var pressureCollector: PressureCollectingListener? = null
    private var locationCollector: LocationCollectingListener? = null
    private var locationAvailable: Boolean = false
    private var windDetails: WindDetails? = null

    private var titleText: EditText? = null
    private var startRecordingButton: Button? = null
    private var finishRecordingButton: Button? = null
    private var recordingLabel: TextView? = null
    private var countdownTimer: CountDownTimer? = null
    private var progressBar: ProgressBar? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.v("init", "main activity created")

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        setContentView(R.layout.layout_main)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, javaClass.name)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        titleText = findViewById<View>(R.id.TITLE) as EditText
        startRecordingButton = findViewById<View>(R.id.START_RECORDING) as Button
        finishRecordingButton = findViewById<View>(R.id.FINISH_RECORDING) as Button
        recordingLabel = findViewById<View>(R.id.RECORDING_LABEL) as TextView
        progressBar = findViewById<View>(R.id.PROGRESS_BAR) as ProgressBar

        titleText!!.setText(DEFAULT_RECORDING_TITLE)
        recordingLabel!!.isEnabled = false
        progressBar!!.isEnabled = false

        startRecordingButton!!.setOnClickListener { doStartRecording() }
        startRecordingButton!!.isEnabled = true

        finishRecordingButton!!.setOnClickListener { doStopRecording() }
        finishRecordingButton!!.isEnabled = false

    }

    override fun onResume() {
        super.onResume()
        Log.v("init", "main activity resumed")
    }

    override fun onPause() {
        super.onPause()
        Log.v("init", "main activity paused")
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun ensurePermission(permission: String, requestCode: Int?) {
        if (baseContext.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, permission)) {

                Toast.makeText(this,
                        "$permission needed to obtain the speed and bearing data",
                        Toast.LENGTH_SHORT)
                        .show()

                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode!!)

            } else {

                ActivityCompat.requestPermissions(this@MainActivity, arrayOf(permission), requestCode!!)
            }
        }
    }

    private fun supportsRuntimePermissions(): Boolean {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
    }

    private fun doStartRecording() {

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val maxRecordingLengthSec = (sharedPrefs.getString(PREF_MAX_RECORDING_TIME, DEFAULT_MAX_RECORDING_TIME_MINUTES).toFloat() * 60).roundToInt();
        val pressureSamplesPerSecond = sharedPrefs.getString(PREF_PRESSURE_SAMPLING_HZ, DEFAULT_PRESSURE_SAMPLES_PER_SECOND).toFloat();
        val utilizeGps = sharedPrefs.getBoolean(PREF_UTILIZE_GPS, DEFAULT_UTILIZE_GPS_DATA);
        val speedSamplesPerSecond = sharedPrefs.getString(PREF_SPEED_SAMPING_HZ, DEFAULT_SPEED_SAMPLES_PER_SECOND).toFloat();

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

        locationCollector = LocationCollectingListener(
                speedSamplesPerSecond,
                maxRecordingLengthSec,
                recStartTimeNanos
        )

        windDetails = WindDetails();

        locationAvailable = false
        if (utilizeGps) {
            if (supportsRuntimePermissions()) {
                ensurePermission(Manifest.permission.ACCESS_FINE_LOCATION, 1)
            }

            try {
                locationManager!!.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        Math.round(1000f / speedSamplesPerSecond / 2f).toLong(),
                        0f,
                        locationCollector)
                locationAvailable = true
            } catch (e: SecurityException) {
                Log.v("init", "The app has no permissions obtain the speed and bearing data through GPS")
                Toast.makeText(this,
                        "The app has no permissions obtain the speed and bearing data through GPS",
                        Toast.LENGTH_LONG)
                        .show()
            }
        }

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
        if (locationAvailable) {
            locationManager!!.removeUpdates(locationCollector)
        }

        val title = titleText!!.text.toString()
        recDetails!!.title = title

        val exporter = Exporter(this@MainActivity)

        try {
            exporter.exportResults(
                    baseContext,
                    recDetails!!,
                    pressureCollector!!,
                    locationCollector!!,
                    windDetails!!
            )
        } catch (e: IOException) {
            Log.e("export", "can't export results", e)
            throw RuntimeException("can't export results", e)
        }

        pressureCollector = null
        locationCollector = null
        windDetails = null
        recDetails = null

        countdownTimer?.cancel()
        countdownTimer = null

        startRecordingButton!!.isEnabled = true
        finishRecordingButton!!.isEnabled = false
        recordingLabel!!.isEnabled = false
        progressBar!!.progress = 0
        progressBar!!.isEnabled = false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.action_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            else -> return super.onOptionsItemSelected(item)
        }
        return true;
    }

    companion object {
        private const val DEFAULT_RECORDING_TITLE = "regular conditions"

        private const val PREF_MAX_RECORDING_TIME = "pref_MaxRecordingTimeMinutes"
        private const val DEFAULT_MAX_RECORDING_TIME_MINUTES = "120"

        private const val PREF_PRESSURE_SAMPLING_HZ = "pref_PressureSamplesPerSecondHz"
        private const val DEFAULT_PRESSURE_SAMPLES_PER_SECOND = "60.0"

        private const val PREF_UTILIZE_GPS = "pref_UtilizeGpsData"
        private const val DEFAULT_UTILIZE_GPS_DATA = true

        private const val PREF_SPEED_SAMPING_HZ = "pref_SpeedSamplesPerSecondHz"
        private const val DEFAULT_SPEED_SAMPLES_PER_SECOND = "1.0"

    }
}
