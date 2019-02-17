package com.avyss.PressurePulsationsRecorder.acquisition

import java.util.Date

data class RecordingDetails(
        val recordingStartTime: Date
) {
    var title: String = ""
}
