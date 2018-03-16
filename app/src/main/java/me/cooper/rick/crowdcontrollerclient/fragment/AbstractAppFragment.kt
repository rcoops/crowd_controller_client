package me.cooper.rick.crowdcontrollerclient.fragment

import android.support.v4.app.Fragment
import me.cooper.rick.crowdcontrollerclient.App

abstract class AbstractAppFragment: Fragment() {

    abstract fun getTitle(): String

    override fun onDestroy() {
        App.refWatcher.watch(this)
        super.onDestroy()
    }
}
