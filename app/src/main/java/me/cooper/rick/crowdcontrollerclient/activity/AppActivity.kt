package me.cooper.rick.crowdcontrollerclient.activity

import android.content.Context
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import me.cooper.rick.crowdcontrollerclient.R

/**
 * Created by rick on 23/02/18.
 */
abstract class AppActivity: AppCompatActivity() {

    fun showDismissablePopup(title: String, message: String) {
        AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(getString(R.string.action_ok), { _, _ ->  })
                .show()
    }

}