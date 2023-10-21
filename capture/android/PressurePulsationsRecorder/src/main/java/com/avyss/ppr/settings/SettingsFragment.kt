package com.avyss.ppr.settings

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.avyss.ppr.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

}
