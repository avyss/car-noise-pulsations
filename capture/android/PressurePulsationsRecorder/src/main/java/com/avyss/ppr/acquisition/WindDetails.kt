package com.avyss.ppr.acquisition

import com.avyss.ppr.data.NamedExportableData
import com.avyss.ppr.data.NamedExportableValuesLine

class WindDetails {
    companion object {
        val WIND_COLUMNS_NAMES = arrayOf("time [sec]", "speed [m/s]", "direction [deg]")
    }

    fun exportable(): NamedExportableData {
        return NamedExportableValuesLine(WIND_COLUMNS_NAMES, floatArrayOf(Float.NaN, 0f, 0f))
    }

}
