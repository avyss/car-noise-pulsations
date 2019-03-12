package com.avyss.PressurePulsationsRecorder.settings

import android.content.Context
import android.preference.EditTextPreference
import android.util.AttributeSet

class RevealingEditTextPreference(context: Context?, attrs: AttributeSet?) : EditTextPreference(context, attrs) {

    override fun onDialogClosed(positiveResult: Boolean) {
        super.onDialogClosed(positiveResult)
        setSummary(summary)
    }

    override fun getSummary(): CharSequence {
        return this.text;
    }
}