package com.avyss.PressurePulsationsRecorder.exporting

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.content.FileProvider
import android.util.Log

import com.avyss.PressurePulsationsRecorder.acquisition.RecordingDetails
import com.avyss.PressurePulsationsRecorder.acquisition.AbstractSampleCollector

import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Collections
import java.util.Locale

class Exporter(private val parentActivity: Activity) {

    companion object {
        private const val FILE_SHARING_AUTH_NAME = "com.avyss.PressurePulsationsRecorder.recordingSharing"
    }

    @Throws(IOException::class)
    fun exportResults(
            context: Context,
            recDetails: RecordingDetails,
            pressureCollector: AbstractSampleCollector,
            speedCollector: AbstractSampleCollector) {

        val zipFileName = generateFileName(recDetails)

        val zp = ZipPacker(parentActivity.baseContext.cacheDir, zipFileName)
        try {
            zp.addSamples("pressure", pressureCollector)
            zp.addSamples("speed", speedCollector)
        } finally {
            zp.close()
        }

        val zipFile = zp.zipFile

        shareResults(context, listOf(zipFile))

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
