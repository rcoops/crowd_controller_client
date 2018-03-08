package me.cooper.rick.crowdcontrollerclient.api

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.cooper.rick.crowdcontrollerapi.dto.GroupDto
import me.cooper.rick.crowdcontrollerapi.dto.LocationDto
import me.cooper.rick.crowdcontrollerapi.dto.UserDto
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.api.client.GroupClient
import me.cooper.rick.crowdcontrollerclient.api.client.UserClient
import me.cooper.rick.crowdcontrollerclient.api.task.user.UpdateLocation
import me.cooper.rick.crowdcontrollerclient.util.ServiceGenerator
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS

class UpdateService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val binder = LocalBinder()
    private var listener: UpdateServiceListener? = null
    private lateinit var pref: SharedPreferences

    private val userTimer = Timer(true)
    private var groupTimer: Timer? = null

    private var userClient: UserClient? = null
    private var groupClient: GroupClient? = null

    private var token: String? = null
    private var userId: Long = -1
    private var groupId: Long? = null

    private var fusedLocationProviderClient: FusedLocationProviderClient? = null

    private val locationRequest = LocationRequest.create().apply {
        interval = SECONDS.toMillis(10)
        fastestInterval = SECONDS.toMillis(10)
        priority = PRIORITY_HIGH_ACCURACY
    }

    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val lastLocation = locationResult.lastLocation
            sendLocation(LocationDto(userId, lastLocation.latitude, lastLocation.longitude))
        }
    }

    private val destroyTasks: (UserDto) -> Unit = {
        tasks.forEach { it.cancel(true) }
        tasks.clear()
    }

    private val tasks = mutableListOf<AsyncTask<Void, Void, out Any?>>()

    fun startTracking() {
        LocationServices.getSettingsClient(this)
                .checkLocationSettings(
                        LocationSettingsRequest.Builder()
                                .addLocationRequest(locationRequest)
                                .build()
                )
                .apply {
                    addOnSuccessListener { startLocationUpdates() }
                    addOnFailureListener { handleFailure(it) }
                }
    }

    private fun handleFailure(it: Throwable) {
        try {
            listener?.handleApiException(it)
        } catch (e: IntentSender.SendIntentException) {
            Log.d("", "Intent exception thrown", e) // wont happen
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        pref = getSharedPreferences(getString(R.string.user_details), Context.MODE_PRIVATE)
        pref.registerOnSharedPreferenceChangeListener(this)
        token = pref.getString(getString(R.string.token), null)
        userId = pref.getLong(getString(R.string.user_id), -1)

        userClient = ServiceGenerator.createService(UserClient::class, token)

        userTimer.schedule({ getUser() }, 0L, SECONDS.toMillis(5))

        return binder
    }

    fun registerListener(listener: UpdateServiceListener) {
        this.listener = listener
    }

    fun unregisterListener(listener: UpdateServiceListener) {
        if (this.listener == listener) this.listener = null
    }

    private fun getUser() {
        if (userId == -1L) return

        userClient?.user(userId)
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.doOnError { handleFailure(it) }
                ?.subscribe {
                    updateListeners(it)
                    adjustGroup(it)
                } // TODO error handling ??
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (pref.getBoolean(getString(R.string.location_permissions_granted), false)) {
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationProviderClient?.requestLocationUpdates(locationRequest, locationCallback, null)
        } else {
            listener?.requestPermissions()
        }
    }

    private fun sendLocation(dto: LocationDto) {
        tasks += UpdateLocation(dto, destroyTasks)
    }

    private fun getGroup() {
        groupId?.let {
            groupClient?.groupObservable(it)
                    ?.subscribeOn(Schedulers.io())
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.doOnError { handleFailure(it) }
                    ?.subscribe({ updateListeners(it) }) // TODO error handling
        }
    }

    private fun updateListeners(userDto: UserDto) {
        listener?.onUpdate(userDto)
    }

    private fun adjustGroup(userDto: UserDto) {
        if (userDto.group == null) {
            unScheduleGroupRequest()
        } else if (groupId == null) {
            scheduleGroupRequest()
        }
        groupId = userDto.group
    }

    private fun scheduleGroupRequest() {
        groupTimer = Timer(true)
        groupClient = ServiceGenerator.createService(GroupClient::class, token)
        groupTimer!!.schedule({ getGroup() }, 0L, SECONDS.toMillis(5))
        startTracking()
    }

    private fun unScheduleGroupRequest() {
        groupTimer?.cancel()
        groupTimer?.purge()
        groupTimer = null
        groupClient = null
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback)
    }

    private fun resetGroupSchedule() {
        unScheduleGroupRequest()
        scheduleGroupRequest()
    }

    private fun updateListeners(groupDto: GroupDto) {
        listener?.onUpdate(groupDto)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences?.let {
            when (key) {
                getString(R.string.token) -> {
                    token = it.getString(getString(R.string.token), null)

                    userClient = ServiceGenerator.createService(UserClient::class, token)

                    groupClient?.let { resetGroupSchedule() }
                }
                else -> userId = it.getLong(getString(R.string.user_id), -1)
            }
        }
    }

    inner class LocalBinder : Binder() {
        val service: UpdateService = this@UpdateService
    }

    interface UpdateServiceListener {
        fun onUpdate(userDto: UserDto)
        fun onUpdate(groupDto: GroupDto)
        fun requestPermissions()
        fun handleApiException(e: Throwable)
    }

    private fun Timer.schedule(task: () -> Unit, delay: Long, period: Long) {
        schedule(object : TimerTask() { override fun run() = task() }, delay, period)
    }

}
