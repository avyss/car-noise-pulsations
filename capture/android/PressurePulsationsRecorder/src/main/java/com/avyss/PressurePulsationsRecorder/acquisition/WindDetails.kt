package com.avyss.PressurePulsationsRecorder.acquisition

import com.avyss.PressurePulsationsRecorder.data.NamedExportableData
import com.avyss.PressurePulsationsRecorder.data.NamedExportableValuesLine

class WindDetails {
    companion object {
        val WIND_COLUMNS_NAMES = arrayOf("time [sec]", "speed [m/s]", "direction [deg]")
    }

    fun exportable(): NamedExportableData {
        return NamedExportableValuesLine(WIND_COLUMNS_NAMES, floatArrayOf(Float.NaN, 0f, 0f))
    }

}
