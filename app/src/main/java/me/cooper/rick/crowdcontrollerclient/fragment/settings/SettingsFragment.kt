package me.cooper.rick.crowdcontrollerclient.fragment.settings

import android.os.Bundle
import android.support.v7.preference.PreferenceFragmentCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import me.cooper.rick.crowdcontrollerclient.R

open class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view!!.setBackgroundColor(resources.getColor(R.color.cardview_light_background))
        return view
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings)
    }

    companion object {
        val TAG = "settings"
    }

}
