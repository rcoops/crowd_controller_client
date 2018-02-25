package me.cooper.rick.crowdcontrollerclient.activity

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import me.cooper.rick.crowdcontrollerclient.App
import me.cooper.rick.crowdcontrollerclient.R

abstract class AppActivity: AppCompatActivity() {

    protected var app: App? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = applicationContext as App
    }

    override fun onResume() {
        super.onResume()
        App.currentActivity = this
    }

    override fun onPause() {
        clearReferences()
        super.onPause()
    }

    override fun onDestroy() {
        clearReferences()
        super.onDestroy()
    }

    fun showDismissablePopup(title: String, message: String) {
        AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(getString(R.string.action_ok), { _, _ ->  })
                .show()
    }

    private fun clearReferences() {
        if (this == App.currentActivity) App.currentActivity = null
    }

}
