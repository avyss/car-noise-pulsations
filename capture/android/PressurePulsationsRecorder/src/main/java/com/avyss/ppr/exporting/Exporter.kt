package com.avyss.ppr.exporting

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import android.util.Log
import android.widget.Toast
import com.avyss.ppr.acquisition.PressureCollectingListener

import com.avyss.ppr.acquisition.RecordingDetails
import com.avyss.ppr.acquisition.LocationCollectingListener
import com.avyss.ppr.acquisition.WindDetails

import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Locale

class Exporter(private val parentActivity: Activity) {

    companion object {
        private const val FILE_SHARING_AUTH_NAME =
            "com.avyss.PressurePulsationsRecorder.recordingSharing"

        private const val FILE_SHARING_SUBDIR_NAME = "ppr-recordings"

        private val DATA_FORMAT_VERSION_COLUMNS = arrayOf("major", "minor")
        private val DATA_FORMAT_VERSION_VALUES = floatArrayOf(1f, 0f)
    }

    @Throws(IOException::class)
    fun exportResults(
        context: Context,
        recDetails: RecordingDetails,
        pressureCollector: PressureCollectingListener,
        locationCollector: LocationCollectingListener,
        windDetails: WindDetails
    ) {

        val zipFileName = generateFileName(recDetails)

        val zipDir = File(parentActivity.baseContext.cacheDir, FILE_SHARING_SUBDIR_NAME)
        if (zipDir.exists()) {
            if (!zipDir.isDirectory) {
                throw RuntimeException("temporary file location already exists but it is not a directory: " + zipDir.absolutePath)
            }
        } else {
            zipDir.mkdir()
        }

        ZipPacker(zipDir, zipFileName).use {

            it.put("format", DATA_FORMAT_VERSION_COLUMNS, DATA_FORMAT_VERSION_VALUES)

            it.put("pressure_samples", pressureCollector.exportablePressureSamples())
            it.put("pressure_fs", pressureCollector.exportableFs())

            it.put("speed_samples", locationCollector.exportableSpeedSamples())
            it.put("speed_fs", locationCollector.exportableFs())

            it.put("bearing_samples", locationCollector.exportableBearingSamples())
            it.put("bearing_fs", locationCollector.exportableFs())

            it.put("wind", windDetails.exportable())

            val zipFile = it.zipFile
            shareResults(context, listOf(zipFile))
        }
    }

    private fun generateFileName(recDetails: RecordingDetails): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val startTimestamp = dateFormat.format(recDetails.recordingStartTime)

        var fileName = startTimestamp
        if (!recDetails.title.isEmpty()) {
            fileName += " - " + recDetails.title
        }

        fileName += ".zip"

        return fileName
    }

    private fun shareResults(context: Context, resultFiles: List<File>) {

        val fileUris = ArrayList<Uri>()
        for (file in resultFiles) {
            Log.d("results", "sharing file " + file.name)
            val contentUri = FileProvider.getUriForFile(context, FILE_SHARING_AUTH_NAME, file)
            context.grantUriPermission(
                FILE_SHARING_AUTH_NAME,
                contentUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            fileUris.add(contentUri)
        }

        val intentShareFile = Intent(Intent.ACTION_SEND_MULTIPLE)
        intentShareFile.type = "application/binary"
        intentShareFile.putExtra(Intent.EXTRA_STREAM, fileUris)

        if (intentShareFile.resolveActivity(context.packageManager) != null) {
            parentActivity.startActivity(
                Intent.createChooser(
                    intentShareFile,
                    "Share the recording result"
                )
            )
        } else {
            Toast.makeText(
                context,
                "No apps available to share the recording result",
                Toast.LENGTH_SHORT
            )
                .show()
        }
    }

}
