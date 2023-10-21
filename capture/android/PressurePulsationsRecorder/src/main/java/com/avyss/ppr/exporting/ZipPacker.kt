package com.avyss.ppr.exporting

import android.util.Log

import com.avyss.ppr.data.NamedExportableData
import com.avyss.ppr.data.NamedExportableValuesLine

import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipPacker(
    containerDirectory: File,
    fileName: String
) : Closeable {

    companion object {
        private const val DATA_FILE_EXT = ".csv"
        private const val COMMENT_LINE_START = "# "
        private const val FIELD_SEPARATOR = ", "
    }

    val zipFile: File = File(containerDirectory, fileName)
    private val zos: ZipOutputStream =
        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile)))

    init {
        zipFile.deleteOnExit()
    }

    @Throws(IOException::class)
    override fun close() {
        zos.finish()
        zos.close()
        Log.d("results", "stored file " + zipFile.name)
    }

    fun put(partName: String, valuesNames: Array<String>, valuesLine: FloatArray) {
        put(partName, NamedExportableValuesLine(valuesNames, valuesLine))
    }

    fun put(partName: String, namedExportableData: NamedExportableData) {
        writePart(partName, StringGeneratingIterator(namedExportableData))
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


    private inner class StringGeneratingIterator(
        namedExportableData: NamedExportableData
    ) : Iterator<String> {

        private val columnNames = namedExportableData.columnNames
        private val rowsIterator = namedExportableData.rowsIterator

        private var writeColumnNamesLine: Boolean = (columnNames != null)

        override fun hasNext(): Boolean {
            if (writeColumnNamesLine) {
                return true
            } else {
                return rowsIterator.hasNext()
            }
        }

        override fun next(): String {
            if (writeColumnNamesLine) {
                writeColumnNamesLine = false
                return COMMENT_LINE_START + columnNames!!.joinToString(FIELD_SEPARATOR)
            } else {
                return rowsIterator.next().joinToString(FIELD_SEPARATOR)
            }
        }
    }

}
