package me.cooper.rick.crowdcontrollerclient.api

import android.annotation.SuppressLint
import android.app.Service
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.AsyncTask
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
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
import me.cooper.rick.crowdcontrollerclient.api.task.group.GetGroup
import me.cooper.rick.crowdcontrollerclient.api.task.user.GetUser
import me.cooper.rick.crowdcontrollerclient.api.task.user.UpdateLocation
import me.cooper.rick.crowdcontrollerclient.api.util.destroyTaskType
import me.cooper.rick.crowdcontrollerclient.util.ServiceGenerator.createService
import okhttp3.WebSocket
import ua.naiksoftware.stomp.LifecycleEvent.Type.*
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.client.StompClient
import ua.naiksoftware.stomp.client.StompMessage
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.reflect.KClass


class UpdateService : Service(), OnSharedPreferenceChangeListener {

    private val binder = LocalBinder()
    private var listener: UpdateServiceListener? = null
    private lateinit var pref: SharedPreferences

    private val userTimer = Timer(true)
    private var groupTimer: Timer? = null

    private var userClient: UserClient? = null
    private var groupClient: GroupClient? = null
    private lateinit var mStompClient: StompClient

    private var token: String? = null
    private var userId: Long = -1
    private var groupId: Long? = null

    private var fusedLocationProviderClient: FusedLocationProviderClient? = null

    private val tasks = mutableListOf<AsyncTask<Void, Void, out Any?>>()

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

    fun registerListener(listener: UpdateServiceListener) {
        this.listener = listener
    }

    fun unregisterListener(listener: UpdateServiceListener) {
        if (this.listener == listener) this.listener = null
    }

    override fun onBind(intent: Intent?): IBinder {
        pref = getSharedPreferences(getString(R.string.user_details), Context.MODE_PRIVATE)
        pref.registerOnSharedPreferenceChangeListener(this)
        token = pref.getString(getString(R.string.token), null)
        userId = pref.getLong(getString(R.string.user_id), -1)

        userClient = createService(UserClient::class, token)
        mStompClient = Stomp.over(WebSocket::class.java,
                "ws://${getString(R.string.base_uri)}/chat/websocket")

        userTimer.schedule({ getUser() }, 0L, SECONDS.toMillis(5))

        return binder
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences?.let {
            when (key) {
                getString(R.string.token) -> {
                    token = it.getString(getString(R.string.token), null)

                    userClient = createService(UserClient::class, token)
                }
                else -> userId = it.getLong(getString(R.string.user_id), -1)
            }
        }
    }

    private fun handleFailure(it: Throwable) {
        try {
            listener?.handleApiException(it)
        } catch (e: IntentSender.SendIntentException) {
            Log.d("", "Intent exception thrown", e) // wont happen
        }
    }

    private fun getUser() {
        if (userId == -1L) return
        tasks += GetUser({ updateListeners(it); adjustGroup(it); destroyTaskType(tasks, GetUser::class) })
    }

    private fun destroyTaskType(taskClass: KClass<out Any>) = destroyTaskType(tasks, taskClass)

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
        tasks += UpdateLocation(dto, { destroyTaskType(UpdateLocation::class) })
    }

    private fun getGroup() {
        groupId?.let {
            tasks += GetGroup(it, { updateListeners(it); destroyTaskType(GetGroup::class) })
        }
    }

    private fun updateListeners(userDto: UserDto) {
        listener?.onUpdate(userDto)
    }

    private fun adjustGroup(userDto: UserDto) {
        if (userDto.group == null) {
            unScheduleGroupRequest()
        } else if (groupId == null) {
            scheduleGroupRequest(userDto.group!!)
        }
        groupId = userDto.group
    }

    private fun scheduleGroupRequest(groupId: Long) {
        if (mStompClient.isConnected) return
        mStompClient.connect()

        mStompClient.lifecycle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { lifecycleEvent ->
                    when (lifecycleEvent.type!!) {
                        OPENED -> Log.d("opened", "Stomp connection opened")
                        ERROR -> Log.e(TAG, "Stomp connection error", lifecycleEvent.exception) // TODO - error handling?
                        CLOSED -> Log.d("closed", "Stomp connection closed")
                    }
                }
        mStompClient.topic("/topic/greetings/$groupId")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { topicMessage ->
                    Log.d(TAG, "Received " + topicMessage.payload)
                    updateListeners(jackson.readValue(topicMessage, GroupDto::class))
                }
        startTracking()
    }

    private fun unScheduleGroupRequest() {
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback)
        mStompClient.disconnect()
    }
//TODO if groupId != userDto.group reset
//    private fun resetGroupSchedule() {
//        unScheduleGroupRequest()
//        scheduleGroupRequest()
//    }

    private fun updateListeners(groupDto: GroupDto) {
        listener?.onUpdate(groupDto)
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

    companion object {
        val jackson = ObjectMapper()
    }

    fun <T : Any> ObjectMapper.readValue(value: StompMessage, clazz: KClass<T>): T {
        return readValue<T>(value.payload, clazz.java)
    }

}
