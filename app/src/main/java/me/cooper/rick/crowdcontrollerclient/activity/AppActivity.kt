package me.cooper.rick.crowdcontrollerclient.activity

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.design.widget.Snackbar.LENGTH_INDEFINITE
import android.support.v4.app.ActivityCompat.requestPermissions
import android.support.v4.app.ActivityCompat.shouldShowRequestPermissionRationale
import android.support.v4.content.ContextCompat.checkSelfPermission
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import me.cooper.rick.crowdcontrollerapi.dto.error.APIErrorDto
import me.cooper.rick.crowdcontrollerclient.App
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.api.util.parseError
import me.cooper.rick.crowdcontrollerclient.constants.HttpStatus
import retrofit2.Response
import kotlin.reflect.KClass

abstract class AppActivity : AppCompatActivity() {

    protected var app: App? = null

    private val dialogs = mutableListOf<AlertDialog>()
    private val tasks = mutableListOf<AsyncTask<Void, Void, out Any?>>()

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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    protected fun showProgress(show: Boolean, contentView: View, progressBar: ProgressBar) {
        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        contentView.visibility = if (show) View.GONE else View.VISIBLE
        contentView.animate()
                .setDuration(shortAnimTime)
                .alpha((if (show) 0 else 1).toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        contentView.visibility = if (show) View.GONE else View.VISIBLE
                    }
                })

        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        progressBar.animate()
                .setDuration(shortAnimTime)
                .alpha((if (show) 1 else 0).toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        progressBar.visibility = if (show) View.VISIBLE else View.GONE
                    }
                })
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

    protected fun refresh() {
        destroyTasks()
        dismissDialogs()
    }

    protected fun isTaskOfTypeRunning(clazz: KClass<out Any>): Boolean {
        return tasks.any { it::class == clazz }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        fun isGranted(grantResults: IntArray): Boolean {
            return grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED
        }
        when (requestCode) {
            REQUEST_FINE_LOCATION -> editAppDetails {
                putBoolean(getString(R.string.location_permissions_granted), isGranted(grantResults))
            }
        }
    }

    private fun mayUseLocationServices(): Boolean {
        if (hasLocationPermission()) return true

        if (shouldShowRequestPermissionRationale(this, ACCESS_FINE_LOCATION)) {
            Snackbar.make(findViewById(R.id.content),
                    R.string.location_permission_rationale, LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, { makePermissionRequest() })
                    .show()
        } else {
            makePermissionRequest()
        }

        return false
    }

    protected fun requestLocationPermissions() {
        editAppDetails {
            putBoolean(getString(R.string.location_permissions_granted), mayUseLocationServices())
        }
    }

    private fun makePermissionRequest() {
        requestPermissions(this, arrayOf(ACCESS_FINE_LOCATION), REQUEST_FINE_LOCATION)
    }

    private fun hasLocationPermission(): Boolean {
        return checkSelfPermission(this, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED
    }

    private fun dismissDialogs() {
        dialogs.forEach { if (it.isShowing) it.dismiss() }
        dialogs.clear()
    }

    private fun destroyTasks() {
        tasks.forEach { it.cancel(true) }
        tasks.clear()
    }

    protected fun addDialog(dialog: AlertDialog): AlertDialog {
        dialogs += dialog
        return dialog
    }

    protected fun addTask(task: AsyncTask<Void, Void, out Any?>): AsyncTask<Void, Void, out Any?> {
        tasks += task
        return task
    }

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

    @SuppressLint("CommitPrefEdits")
    protected fun editAppDetails(changes: SharedPreferences.Editor.() -> Unit) {
        appDetails().edit().apply { changes(); commit() }
    }

    private fun appDetails(): SharedPreferences {
        return getSharedPreferences(getString(R.string.user_details), Context.MODE_PRIVATE)
    }

    protected fun getToken(): String {
        return appDetails().getString(getString(R.string.token), "")
    }

    protected fun getUserId(): Long {
        return appDetails().getLong(getString(R.string.user_id), -1L)
    }

    private fun clearReferences() {
        destroyTasks()
        if (this == App.currentActivity) App.currentActivity = null
    }

    inner class APIError(private val apiErrorDto: APIErrorDto,
                         private val consumer: ((APIErrorDto) -> Unit)?) {

        fun call() = consumer?.let { it(apiErrorDto) }

    }

    companion object {
        private const val REQUEST_FINE_LOCATION = 2
    }

}

