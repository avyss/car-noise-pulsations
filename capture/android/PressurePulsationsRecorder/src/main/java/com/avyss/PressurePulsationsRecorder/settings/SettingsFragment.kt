package com.avyss.PressurePulsationsRecorder.settings

import android.os.Bundle
import android.preference.PreferenceFragment
import com.avyss.PressurePulsationsRecorder.R

class SettingsFragment: PreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)
    }

}
