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
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.support.design.widget.Snackbar
import android.support.design.widget.Snackbar.LENGTH_INDEFINITE
import android.support.v4.app.ActivityCompat.requestPermissions
import android.support.v4.app.ActivityCompat.shouldShowRequestPermissionRationale
import android.support.v4.content.ContextCompat.checkSelfPermission
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.preference.PreferenceManager
import android.support.v7.preference.PreferenceManager.getDefaultSharedPreferences
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import me.cooper.rick.crowdcontrollerapi.dto.error.APIErrorDto
import me.cooper.rick.crowdcontrollerapi.dto.group.GroupSettingsDto
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.api.client.GroupClient
import me.cooper.rick.crowdcontrollerclient.api.client.UserClient
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService
import me.cooper.rick.crowdcontrollerclient.api.util.parseError
import me.cooper.rick.crowdcontrollerclient.api.constants.HttpStatus
import me.cooper.rick.crowdcontrollerclient.constant.VibratePattern
import me.cooper.rick.crowdcontrollerclient.fragment.AbstractAppFragment
import me.cooper.rick.crowdcontrollerclient.util.ServiceGenerator.createService
import retrofit2.Response
import kotlin.reflect.KClass

abstract class AppActivity : AppCompatActivity(),
        SharedPreferences.OnSharedPreferenceChangeListener,
        AbstractAppFragment.FragmentListenerInterface {

    private val dialogs = mutableListOf<AlertDialog>()

    protected var userClient: UserClient? = null
    protected var groupClient: GroupClient? = null

    private lateinit var soundPool: SoundPool

    private var shouldVibrate: Boolean = false

    private val sounds = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initClients()

        initPreferences()
    }

    private fun initPreferences() {
        PreferenceManager.setDefaultValues(this, R.xml.settings, false)

        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        shouldVibrate = preferences.getBoolean(PREF_TOG_VIBRATE, false)
        preferences.registerOnSharedPreferenceChangeListener(this)

        soundPool = if (isVersionOrGreater(Build.VERSION_CODES.LOLLIPOP)) buildSoundPoolLollipop() else buildSoundPoolBase()
        addSound(SOUND_DING, R.raw.ding)
        addSound(SOUND_CLICK, R.raw.click)
        addSound(SOUND_NEGATIVE, R.raw.negative)
    }

    override fun onResume() {
        super.onResume()
        ApiService.setActivity(this)
    }

    override fun onPause() {
        clearReferences()
        super.onPause()
    }

    override fun onDestroy() {
        clearReferences()

        super.onDestroy()
    }

    protected fun addSound(key: String, rawSoundId: Int) {
        sounds[key] = soundPool.load(this, rawSoundId, 1)
    }

    protected fun isVersionOrGreater(versionCode: Int): Boolean {
        return Build.VERSION.SDK_INT >= versionCode
    }

    override fun playClick() {
        playSound(SOUND_CLICK)
        vibrate(VibratePattern.CLICK)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private fun buildSoundPoolLollipop(): SoundPool {
        return SoundPool.Builder()
                .setAudioAttributes(AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build())
                .setMaxStreams(1)
                .build()
    }


    @TargetApi(Build.VERSION_CODES.BASE)
    private fun buildSoundPoolBase(): SoundPool {
        return SoundPool(1, AudioManager.STREAM_MUSIC, 0)
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

    protected fun startActivity(activityClass: KClass<out AppActivity>,
                                vararg extras: Pair<String, Long>) {
        startActivity(Intent(this, activityClass.java)
                .apply { extras.forEach { putExtra(it.first, it.second) } })
    }

    protected fun showDismissiblePopup(title: String, message: String, onClickListener: DialogInterface.OnClickListener) {
        buildBasePopup(title, message)
                .setNegativeButton(getString(android.R.string.ok), onClickListener)
                .show()
    }

    protected fun showDismissiblePopup(title: String, message: String, apiError: APIError? = null) {
        buildBasePopup(title, message)
                .setNegativeButton(getString(android.R.string.ok), { _, _ -> apiError?.call() })
                .show()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences?.let {
            when (key) {
                getString(R.string.token) -> initClients()
                PREF_VOL_EFFECTS -> {
                    setSoundVolume(getVolumeSetting(sharedPreferences, key))
                    playSound(SOUND_DING)
                }
                PREF_TOG_VIBRATE -> {
                    shouldVibrate = sharedPreferences.getBoolean(PREF_TOG_VIBRATE, false)
                    vibrate(VibratePattern.CLICK)
                }
                PREF_TOG_CLUSTERING, PREF_CLUSER_MIN_PERCENT, PREF_CLUSER_MIN_RADIUS -> {
                    updateGroupSettings(sharedPreferences)
                }
            }
        }
    }

    private fun updateGroupSettings(sharedPreferences: SharedPreferences) {
        val settings = GroupSettingsDto(
                sharedPreferences.getBoolean(PREF_TOG_CLUSTERING, true),
                sharedPreferences.getInt(PREF_CLUSER_MIN_RADIUS, 50).toDouble(),
                sharedPreferences.getInt(PREF_CLUSER_MIN_PERCENT, 50) / 100.0
        )
        ApiService.updateGroupSettings(settings)
    }

    protected fun playSound(key: String) {
        val volume = getVolumeSetting(getDefaultSharedPreferences(this), PREF_VOL_EFFECTS)
        sounds[key]?.let { soundPool.play(it, volume, volume, 1, 0, 1f) }
    }

    private fun getVolumeSetting(preferences: SharedPreferences, tag: String): Float {
        return preferences.getInt(tag, DEFAULT_VOLUME) / 10.0f
    }

    private fun setSoundVolume(volume: Float) {
        sounds.keys.forEach { setEffectVolume(it, volume) }
    }

    private fun setEffectVolume(tag: String, volume: Float) {
        sounds[tag]?.let { soundPool.setVolume(it, volume, volume) }
    }

    protected fun vibrate(pattern: VibratePattern) {
        if (shouldVibrate) {
            (getSystemService(VIBRATOR_SERVICE) as? Vibrator)?.vibrate(pattern.pattern, -1)
        }
    }

    private fun initClients() {
        userClient = createService(UserClient::class, getToken())
        groupClient = createService(GroupClient::class, getToken())
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

    protected fun dismissDialogs() {
        dialogs.forEach { if (it.isShowing) it.dismiss() }
        dialogs.clear()
    }

    protected fun addDialog(dialog: AlertDialog): AlertDialog {
        dialogs += dialog
        return dialog
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
        if (ApiService.currentActivity.get() == this) {
            ApiService.setActivity(null)
        }
    }

    inner class APIError(private val apiErrorDto: APIErrorDto,
                         private val consumer: ((APIErrorDto) -> Unit)?) {

        fun call() = consumer?.let { it(apiErrorDto) }

    }

    companion object {
        private const val REQUEST_FINE_LOCATION = 2
        private const val PREF_VOL_EFFECTS = "pref_volume_effects"
        private const val PREF_TOG_VIBRATE = "pref_toggle_vibrate"
        private const val PREF_TOG_CLUSTERING = "pref_grp_clustering_toggle"
        private const val PREF_CLUSER_MIN_PERCENT = "pref_grp_clustering_min_percentage"
        private const val PREF_CLUSER_MIN_RADIUS = "pref_grp_clustering_min_distance"
        private const val DEFAULT_VOLUME = 5
        const val SOUND_DING = "ding"
        const val SOUND_CLICK = "click"
        const val SOUND_NEGATIVE = "negative"
    }

}

