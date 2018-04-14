package me.cooper.rick.crowdcontrollerclient.fragment.settings

import android.os.Bundle
import android.support.v7.preference.PreferenceFragmentCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import me.cooper.rick.crowdcontrollerclient.R

class GroupSettingsFragment : SettingsFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.group_settings)
    }

    companion object {
        val TAG = "group_settings"
    }

}
