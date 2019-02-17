package com.avyss.PressurePulsationsRecorder.exporting

import android.util.Log

import com.avyss.PressurePulsationsRecorder.acquisition.AbstractSampleCollector

import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.Closeable
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.Collections
import java.util.function.Consumer
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipPacker(
        containerDirectory: File,
        fileName: String
) : Closeable {

    companion object {
        private const val DATA_FILE_EXT = ".csv"
    }

    val zipFile: File = File(containerDirectory, fileName)
    private val zos: ZipOutputStream = ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile)))

    init {
        zipFile.deleteOnExit()
    }

    @Throws(IOException::class)
    override fun close() {
        zos.finish()
        zos.close()
        Log.d("results", "stored file " + zipFile.name)
    }

    @Throws(IOException::class)
    fun addSamples(collectorName: String, sampleCollector: AbstractSampleCollector) {
        val samplingRateLines = listOf(java.lang.Float.toString(sampleCollector.samplingRate))

        writePart(collectorName + "_fs", samplingRateLines.iterator())

        val samples = sampleCollector.collectedSamples
        val samplesLines = object : Iterator<String> {
            private var i = 0
            private val firstSampleTimeDelay = sampleCollector.firstSampleTimeDelay()

            override fun hasNext(): Boolean {
                return i < samples.size
            }

            override fun next(): String {
                val time = i.toFloat() / sampleCollector.samplingRate + firstSampleTimeDelay
                val value = samples[i]
                i++
                return java.lang.Float.toString(time) + "," + java.lang.Float.toString(value)
            }
        }

        writePart(collectorName + "_samples", samplesLines)
    }

    @Throws(IOException::class)
    private fun writePart(partName: String, linesIterator: Iterator<String>) {

        val entry = ZipEntry(partName + DATA_FILE_EXT)
        zos.putNextEntry(entry)

        try {

            val w = BufferedWriter(OutputStreamWriter(zos))
            try {

                while (linesIterator.hasNext()) {
                    w.write(linesIterator.next())
                    w.newLine()
                }

                w.flush()
            } finally {
                // don't call the w.close(), as it will cause the zip stream to close too early
            }
        } catch (e: IOException) {
            Log.e("results", "can't store $partName", e)
            throw e
        } finally {
            Log.d("results", "stored part $partName")
        }

    }


}
