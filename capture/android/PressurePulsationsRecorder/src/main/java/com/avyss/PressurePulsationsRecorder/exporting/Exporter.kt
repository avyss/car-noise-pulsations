package com.avyss.PressurePulsationsRecorder.exporting

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.content.FileProvider
import android.util.Log
import com.avyss.PressurePulsationsRecorder.acquisition.PressureCollectingListener

import com.avyss.PressurePulsationsRecorder.acquisition.RecordingDetails
import com.avyss.PressurePulsationsRecorder.acquisition.SpeedCollectingListener

import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Locale

class Exporter(private val parentActivity: Activity) {

    companion object {
        private const val FILE_SHARING_AUTH_NAME = "com.avyss.PressurePulsationsRecorder.recordingSharing"

        private val DATA_FORMAT_VERSION_COLUMNS = arrayOf("major", "minor")
        private val DATA_FORMAT_VERSION_VALUES = floatArrayOf(1f, 0f)
    }

    @Throws(IOException::class)
    fun exportResults(
            context: Context,
            recDetails: RecordingDetails,
            pressureCollector: PressureCollectingListener,
            speedCollector: SpeedCollectingListener) {

        val zipFileName = generateFileName(recDetails)

        val zp = ZipPacker(parentActivity.baseContext.cacheDir, zipFileName).use{

            it.put("format", DATA_FORMAT_VERSION_COLUMNS, DATA_FORMAT_VERSION_VALUES)

            it.put("pressure_samples", pressureCollector.exportableSamples())
            it.put("pressure_fs",      pressureCollector.exportableFs())

            it.put("speed_samples", speedCollector.exportableSamples())
            it.put("speed_fs",      speedCollector.exportableFs())

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
            context.grantUriPermission(FILE_SHARING_AUTH_NAME, contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            fileUris.add(contentUri)
        }

        val intentShareFile = Intent(Intent.ACTION_SEND_MULTIPLE)
        intentShareFile.type = "application/binary"
        intentShareFile.putExtra(Intent.EXTRA_STREAM, fileUris)

        parentActivity.startActivity(Intent.createChooser(intentShareFile, "Share the recording result"))
    }

}
