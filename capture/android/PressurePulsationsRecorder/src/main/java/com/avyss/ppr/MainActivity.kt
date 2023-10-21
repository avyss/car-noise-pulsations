package com.avyss.ppr

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.*
import android.os.PowerManager.WakeLock
import androidx.preference.PreferenceManager
import androidx.core.app.ActivityCompat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.avyss.ppr.R

import com.avyss.ppr.acquisition.PressureCollectingListener
import com.avyss.ppr.acquisition.RecordingDetails
import com.avyss.ppr.acquisition.LocationCollectingListener
import com.avyss.ppr.acquisition.WindDetails
import com.avyss.ppr.exporting.Exporter
import com.avyss.ppr.settings.SettingsActivity

import java.io.IOException
import java.text.NumberFormat
import java.util.Date
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private var inRecording: Boolean = false

    private var optionsMenu: Menu? = null

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
    private var progressPressureDataCount: TextView? = null
    private var progressGpsDataCount: TextView? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.v("PPR-Main", "main activity created")

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false)

        setContentView(R.layout.layout_main)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock =
            powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PressureRecorder:recording")

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        titleText = findViewById(R.id.TITLE)
        startRecordingButton = findViewById(R.id.START_RECORDING)
        finishRecordingButton = findViewById(R.id.FINISH_RECORDING)
        recordingLabel = findViewById(R.id.RECORDING_LABEL)
        progressBar = findViewById(R.id.PROGRESS_BAR)
        progressPressureDataCount = findViewById(R.id.PRESSURE_SAMPLES_CNT)
        progressGpsDataCount = findViewById(R.id.GPS_SAMPLES_CNT)

        titleText!!.setText(DEFAULT_RECORDING_TITLE)

        updateVisibility(R.id.PROGRESS_INDICATORS_VIEW, false)

        startRecordingButton!!.setOnClickListener { doStartRecording() }
        startRecordingButton!!.isEnabled = true

        finishRecordingButton!!.setOnClickListener { doStopRecording() }
        finishRecordingButton!!.isEnabled = false
        finishRecordingButton!!.alpha = 0f

    }

    private fun updateVisibility(componentId: Int, visible: Boolean) {
        val component = findViewById<View>(componentId)
        component.isEnabled = visible
        component.alpha = if (visible) 1.0f else 0.0f
    }

    override fun onResume() {
        super.onResume()

        if (prefsSaysUtilizeGps()) {
            if (supportsRuntimePermissions()) {
                ensurePermission()
            }
        }

        Log.v("PPR-Main", "main activity resumed")
    }

    override fun onPause() {
        super.onPause()
        Log.v("PPR-Main", "main activity paused")
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun ensurePermission() {
        val permission = android.Manifest.permission.ACCESS_FINE_LOCATION
        val requestCode = 1
        if (baseContext.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@MainActivity,
                    permission
                )
            ) {
                // todo: ask asynchronously...
                ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
            }
        }
    }

    private fun supportsRuntimePermissions(): Boolean {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            // GPS permission granted
        } else {
            // GPS permission denied
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
            sharedPrefs.edit().putBoolean(PREF_UTILIZE_GPS, false).apply()

            val explanationText =
                "Re-enable the GPS-Provided Data in the Settings to record the Speed and Bearing data"
            Toast.makeText(this, explanationText, Toast.LENGTH_LONG).show()
        }
    }

    private fun doStartRecording() {

        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        val maxRecordingLengthSec =
            (sharedPrefs.getString(PREF_MAX_RECORDING_TIME, DEFAULT_MAX_RECORDING_TIME_MINUTES)!!
                .toFloat() * 60).roundToInt()
        val pressureSamplesPerSecond =
            sharedPrefs.getString(PREF_PRESSURE_SAMPLING_HZ, DEFAULT_PRESSURE_SAMPLES_PER_SECOND)!!
                .toFloat()
        val speedSamplesPerSecond =
            sharedPrefs.getString(PREF_SPEED_SAMPING_HZ, DEFAULT_SPEED_SAMPLES_PER_SECOND)!!
                .toFloat()

        recDetails = RecordingDetails(Date())
        val recStartTimeNanos = SystemClock.elapsedRealtimeNanos()

        wakeLock!!.acquire((maxRecordingLengthSec + 1) * 1000L)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        pressureCollector = PressureCollectingListener(
            pressureSamplesPerSecond,
            maxRecordingLengthSec,
            recStartTimeNanos
        )

        sensorManager!!.registerListener(
            pressureCollector,
            sensorManager!!.getDefaultSensor(Sensor.TYPE_PRESSURE),
            SensorManager.SENSOR_DELAY_FASTEST
        )

        locationCollector = LocationCollectingListener(
            speedSamplesPerSecond,
            maxRecordingLengthSec,
            recStartTimeNanos
        )

        windDetails = WindDetails()

        locationAvailable = false
        if (prefsSaysUtilizeGps()) {
            try {
                locationManager!!.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    (1000f / speedSamplesPerSecond / 2f).roundToInt().toLong(),
                    0f,
                    locationCollector!!
                )
                locationAvailable = true
            } catch (e: SecurityException) {
                Log.v(
                    "PPR-Main",
                    "the app has no permission to obtain the speed and bearing data through GPS"
                )
            }
        }

        startRecordingButton!!.isEnabled = false
        startRecordingButton!!.alpha = 0f
        finishRecordingButton!!.isEnabled = true
        finishRecordingButton!!.alpha = 1.0f
        progressBar!!.max = maxRecordingLengthSec
        recordingLabel!!.isEnabled = true
        recordingLabel!!.alpha = 1.0f

        updateVisibility(R.id.PROGRESS_INDICATORS_VIEW, true)

        val maxProgressMillis = maxRecordingLengthSec * 1000L

        countdownTimer = object : CountDownTimer(maxProgressMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                progressBar?.progress = ((maxProgressMillis - millisUntilFinished) / 1000).toInt()
                progressPressureDataCount?.text = formatNumber(pressureCollector!!.dataCount)
                progressGpsDataCount?.text = formatNumber(locationCollector!!.dataCount)
            }

            override fun onFinish() {
                doStopRecording()
            }

            fun formatNumber(n: Int): String {
                return NumberFormat.getIntegerInstance().format(n)
            }
        }
        countdownTimer!!.start()

        inRecording = true
        invalidateOptionsMenu()
        Log.v("PPR-Main", "recording started")
    }

    private fun prefsSaysUtilizeGps(): Boolean {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        return sharedPrefs.getBoolean(PREF_UTILIZE_GPS, DEFAULT_UTILIZE_GPS_DATA)
    }

    private fun doStopRecording() {
        wakeLock!!.release()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        sensorManager!!.unregisterListener(pressureCollector)
        if (locationAvailable) {
            locationManager!!.removeUpdates(locationCollector!!)
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
            Log.e("PPR-Main", "can't export results", e)
            throw RuntimeException("can't export results", e)
        }

        pressureCollector = null
        locationCollector = null
        windDetails = null
        recDetails = null

        countdownTimer?.cancel()
        countdownTimer = null

        startRecordingButton!!.isEnabled = true
        startRecordingButton!!.alpha = 1.0f
        finishRecordingButton!!.isEnabled = false
        finishRecordingButton!!.alpha = 0f
        recordingLabel!!.isEnabled = false
        recordingLabel!!.alpha = 0f

        progressBar!!.progress = 0
        updateVisibility(R.id.PROGRESS_INDICATORS_VIEW, false)

        inRecording = false
        invalidateOptionsMenu()

        Log.v("PPR-Main", "recording stopped")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        optionsMenu = menu
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val settingsMenuItem = optionsMenu?.findItem(R.id.action_settings)

        if (inRecording) {
            // disabled & shaded
            settingsMenuItem?.isEnabled = false
            settingsMenuItem?.icon?.alpha = 127
        } else {
            // enabled & visible
            settingsMenuItem?.isEnabled = true
            settingsMenuItem?.icon?.alpha = 255
        }

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    companion object {
        private const val DEFAULT_RECORDING_TITLE = "regular conditions"

        private const val PREF_MAX_RECORDING_TIME = "pref_MaxRecordingTimeMinutes"
        private const val DEFAULT_MAX_RECORDING_TIME_MINUTES = "60"

        private const val PREF_PRESSURE_SAMPLING_HZ = "pref_PressureSamplesPerSecondHz"
        private const val DEFAULT_PRESSURE_SAMPLES_PER_SECOND = "60.0"

        private const val PREF_UTILIZE_GPS = "pref_UtilizeGpsData"
        private const val DEFAULT_UTILIZE_GPS_DATA = true

        private const val PREF_SPEED_SAMPING_HZ = "pref_SpeedSamplesPerSecondHz"
        private const val DEFAULT_SPEED_SAMPLES_PER_SECOND = "1.0"

    }
}
