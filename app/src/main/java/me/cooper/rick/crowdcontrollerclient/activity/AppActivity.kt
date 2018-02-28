package me.cooper.rick.crowdcontrollerclient.activity

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import me.cooper.rick.crowdcontrollerapi.dto.error.APIErrorDto
import me.cooper.rick.crowdcontrollerclient.App
import me.cooper.rick.crowdcontrollerclient.api.util.parseError
import me.cooper.rick.crowdcontrollerclient.constants.HttpStatus
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

    fun <T> handleResponse(response: Response<T>, responseConsumer: (T) -> Unit,
                           errorConsumer: ((APIErrorDto) -> Unit)? = null) {
        when (response.code()) {
            in HttpStatus.OK until HttpStatus.BAD_REQUEST -> responseConsumer(response.body()!!)
            else -> {
                val apiError = parseError(response)
                showAPIErrorPopup(apiError, errorConsumer)
            }
        }
    }

    protected fun startActivity(clazz: KClass<out Any>, vararg extras: Pair<String, Long>) {
        destroyTasks()
        startActivity(Intent(this, clazz.java)
                .apply { extras.forEach { putExtra(it.first, it.second) } })
    }

    protected fun showDismissiblePopup(title: String, message: String, onClickListener: DialogInterface.OnClickListener) {
        buildBasePopup(title, message)
                .setNegativeButton(getString(android.R.string.ok), onClickListener)
                .show()
    }

    protected fun showDismissiblePopup(title: String, message: String, apiError: APIError? = null) {
        buildBasePopup(title, message)
                .setNegativeButton(getString(android.R.string.ok),
                        { _, _ -> apiError?.call() })
                .show()
    }

    protected abstract fun destroyTasks()

    private fun showAPIErrorPopup(apiErrorDto: APIErrorDto,
                                  errorConsumer: ((APIErrorDto) -> Unit)?) {
        showDismissiblePopup(apiErrorDto.error, apiErrorDto.errorDescription, APIError(apiErrorDto, errorConsumer))
    }

    private fun buildBasePopup(title: String, message: String): AlertDialog.Builder {
        return AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
    }

    private fun getTag(clazz: KClass<out Any>): String = clazz.java.simpleName

    protected fun debug(message: String) {
        Log.d(getTag(this::class), message)
    }

    private fun clearReferences() {
        if (this == App.currentActivity) App.currentActivity = null
    }

    inner class APIError(private val apiErrorDto: APIErrorDto,
                         private val consumer: ((APIErrorDto) -> Unit)?) {

        fun call() = consumer?.let { it(apiErrorDto) }

    }

}

