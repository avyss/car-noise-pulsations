package com.avyss.PressurePulsationsRecorder.data

interface NamedExportableData : ExportableData {

    val columnNames: Array<String>?

}
