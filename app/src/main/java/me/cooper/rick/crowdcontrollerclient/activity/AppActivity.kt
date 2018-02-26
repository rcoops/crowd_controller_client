package me.cooper.rick.crowdcontrollerclient.activity

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import me.cooper.rick.crowdcontrollerclient.App
import me.cooper.rick.crowdcontrollerclient.api.util.parseError
import retrofit2.Response
import kotlin.reflect.KClass

abstract class AppActivity : AppCompatActivity() {

    protected var app: App? = null

    protected val destroyTasksOnClickListener = DialogInterface.OnClickListener { _, _ -> destroyTasks() }

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

    fun <T> handleResponse(response: Response<T>, consumer: (T) -> Unit) {
        when (response.code()) {
            in 200 until 400 -> consumer(response.body()!!)
            else -> {
                val apiError = parseError(response)
                showDismissiblePopup(apiError.error, apiError.errorDescription,
                        destroyTasksOnClickListener)
            }
        }
    }

    protected fun startActivity(clazz: KClass<out Any>) {
        destroyTasks()
        startActivity(Intent(this, clazz.java))
    }

    protected abstract fun destroyTasks()

    protected fun showDismissiblePopup(title: String, message: String, onClickListener: DialogInterface.OnClickListener) {
        AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setNegativeButton(getString(android.R.string.ok), onClickListener)
                .show()
    }

    private fun getTag(clazz: KClass<out Any>): String = clazz.java.simpleName

    protected fun debug(message: String) {
        Log.d(getTag(this::class), message)
    }

    private fun clearReferences() {
        if (this == App.currentActivity) App.currentActivity = null
    }

}
