package com.avyss.ppr.data

interface ExportableData {

    val rowsIterator: Iterator<FloatArray>

    fun withNames(columnNames: Array<String>): NamedExportableData {
        val rit = rowsIterator
        return object : NamedExportableData {

            override val columnNames: Array<String>
                get() = columnNames

            override val rowsIterator: Iterator<FloatArray>
                get() = rit
        }
    }
}
