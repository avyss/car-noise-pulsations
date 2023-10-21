package com.avyss.ppr.data

interface NamedExportableData : ExportableData {

    val columnNames: Array<String>?

}
