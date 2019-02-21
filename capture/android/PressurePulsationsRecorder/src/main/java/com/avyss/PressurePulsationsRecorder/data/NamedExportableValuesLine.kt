package com.avyss.PressurePulsationsRecorder.data

class NamedExportableValuesLine(
   private val valuesNames: Array<String>,
   private val valuesLine: FloatArray
): NamedExportableData {

    override val columnNames: Array<String>
        get() = valuesNames

    override val rowsIterator: Iterator<FloatArray>
        get() = listOf(valuesLine).listIterator()

}
