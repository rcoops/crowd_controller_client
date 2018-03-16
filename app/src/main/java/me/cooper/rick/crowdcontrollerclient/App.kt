package me.cooper.rick.crowdcontrollerclient

import android.app.Application
import android.content.Context
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import me.cooper.rick.crowdcontrollerclient.activity.AppActivity

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        if (LeakCanary.isInAnalyzerProcess(this)) return
        refWatcher = LeakCanary.install(this)
        App.context = this
    }

    companion object {
        var currentActivity: AppActivity? = null

        lateinit var context: Context

        lateinit var refWatcher: RefWatcher
    }
}
