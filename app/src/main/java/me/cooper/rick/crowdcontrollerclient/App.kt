package me.cooper.rick.crowdcontrollerclient

import android.app.Application
import android.content.Context
import me.cooper.rick.crowdcontrollerclient.activity.AppActivity

/**
 * Created by rick on 22/02/18.
 */
class App: Application() {

    override fun onCreate() {
        super.onCreate()
        App.context = this
    }

    companion object {
        var currentActivity: AppActivity? = null
        var context: Context? = null
    }
}