package me.cooper.rick.crowdcontrollerclient.api.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.cooper.rick.crowdcontrollerapi.dto.error.APIErrorDto
import me.cooper.rick.crowdcontrollerapi.dto.group.GroupDto
import me.cooper.rick.crowdcontrollerapi.dto.group.LocationDto
import me.cooper.rick.crowdcontrollerapi.dto.user.UserDto
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.api.client.UserClient
import me.cooper.rick.crowdcontrollerclient.api.constants.BASE_WS_URL
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.getGroup
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.group
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.refreshGroupDetails
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.updateFriends
import me.cooper.rick.crowdcontrollerclient.util.ServiceGenerator.createService
import me.cooper.rick.crowdcontrollerclient.util.call
import me.cooper.rick.crowdcontrollerclient.util.subscribeWithConsumers
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

    private val reconnectTimer = Timer(true)

    private var userClient: UserClient? = null
    private lateinit var stompClient: StompClient
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null

    private var latestGroupId: Long? = null

    var hasPendingGroupInvite = false

    private val locationRequest = LocationRequest.create().apply {
        interval = SECONDS.toMillis(10)
        fastestInterval = SECONDS.toMillis(10)
        priority = PRIORITY_HIGH_ACCURACY
    }

    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val lastLocation = locationResult.lastLocation
            sendLocation(LocationDto(getUserId(), lastLocation.latitude, lastLocation.longitude))
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        pref = getSharedPreferences(getString(R.string.user_details), MODE_PRIVATE)
        pref.registerOnSharedPreferenceChangeListener(this)

        userClient = createService(UserClient::class, getToken())
        stompClient = Stomp.over(WebSocket::class.java,
                "$BASE_WS_URL/chat/websocket")
        openUserSocket()

        return binder
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        sharedPreferences?.let {
            when (key) {
                getString(R.string.token) -> userClient = createService(UserClient::class, getToken())
            }
        }
    }

    fun subscribeToLocationUpdates() {
        if (fusedLocationProviderClient != null) return
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

    private fun getUserId() = pref.getLong(getString(R.string.user_id), -1)

    private fun getToken() = pref.getString(getString(R.string.token), null)

    private fun handleFailure(it: Throwable) {
        try {
            listener?.handleApiException(it)
        } catch (e: IntentSender.SendIntentException) {
            Log.d("", "Intent exception thrown", e) // wont happen
        }
    }

    private fun openUserSocket() {
        val userId = getUserId()

        if (userId == -1L) return

        getUser(userId)

        if (!stompClient.isConnected && !stompClient.isConnecting) connectStompClient()

        clearReconnectTimer()
        subscribeToUserUpdates(userId)
    }

    private fun clearReconnectTimer() {
        reconnectTimer.cancel()
        reconnectTimer.purge()
    }

    private fun getUser(userId: Long) {
        userClient!!.find(userId).call({
            Log.d("GET USER", it.toString())
            onUserUpdate(it)
            adjustGroup(it)
        })
    }

    @SuppressLint("CommitPrefEdits")
    private fun onUserUpdate(userDto: UserDto) {
        pref.edit().apply {
            putLong(getString(R.string.user_id), userDto.id)
            commit()
        }
        updateFriends(userDto.friends)
        if (userDto.group != null && userDto.group != group?.id) {
            userDto.group.let { getGroup(it) }
        }
        listener?.updateNavMenu(userDto.group != null)
    }

    private fun subscribeToGroupUpdates(groupId: Long) {
        stompClient.topic("/topic/group/$groupId")
                .takeUntil { latestGroupId == null }
                .customSubscribe({
                    Log.d(TAG, "Received group update: " + it.payload)
                    try {
                        refreshGroupDetails(jackson.readValue(it, GroupDto::class))
                    } catch (e: UnrecognizedPropertyException) {
                        val dto = jackson.readValue(it, APIErrorDto::class)
                        ApiService.refreshGroupDetails(null)
                        listener?.notifyOfGroupExpiry(dto)
                    }
                })
    }

    private fun subscribeToUserUpdates(userId: Long) {
        stompClient.topic("/topic/user/$userId")
                .customSubscribe({
                    Log.d(TAG, "Received " + it.payload)
                    val userDto = jackson.readValue(it, UserDto::class)
                    onUserUpdate(userDto)
                    adjustGroup(userDto)
                })
    }

    private fun connectStompClient() {
        stompClient.connect(true)

        stompClient.lifecycle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { lifecycleEvent ->
                    when (lifecycleEvent.type!!) {
                        OPENED -> Log.d("opened", "Stomp connection opened")
                        ERROR -> Log.e(TAG, "Stomp connection error", lifecycleEvent.exception) // TODO - error handling?
                        CLOSED -> {
                            Log.d("closed", "Stomp connection closed")
                            resubscribe()
                        }
                    }
                }
    }

    private fun resubscribe() {
        reconnectTimer.schedule({ openUserSocket() }, SECONDS.toMillis(1), SECONDS.toMillis(3))
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
        userClient!!.updateLocation(dto.id!!, dto).call({ Log.d("POST LOC", it.toString()) })
    }

    private fun adjustGroup(userDto: UserDto) {
        if (userDto.group == null) {
            unscheduleLocationUpdates()
        } else if (latestGroupId != userDto.group) {
            scheduleGroupRequest(userDto.group!!)
        }
        if (userDto.group != null && !userDto.groupAccepted && !hasPendingGroupInvite) {
            listener?.notifyUserOfGroupInvite(userDto.group!!, userDto.groupAdmin!!)
            hasPendingGroupInvite = true
        }
        latestGroupId = userDto.group
    }

    private fun scheduleGroupRequest(groupId: Long) {
        subscribeToGroupUpdates(groupId)
        subscribeToLocationUpdates()
    }

    private fun unscheduleLocationUpdates() {
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback)
        fusedLocationProviderClient = null
    }

    inner class LocalBinder : Binder() {
        val service: UpdateService = this@UpdateService
    }

    interface UpdateServiceListener {
        fun updateNavMenu(isGrouped: Boolean)
        fun requestPermissions()
        fun notifyUserOfGroupInvite(groupId: Long, groupAdmin: String)
        fun handleApiException(e: Throwable)
        fun notifyOfGroupExpiry(dto: APIErrorDto)
    }

    companion object {
        private val jackson = ObjectMapper()
    }

    private fun Timer.schedule(task: () -> Unit, delay: Long, period: Long) {
        schedule(object : TimerTask() { override fun run() = task() }, delay, period)
    }

    private fun <T : Any> ObjectMapper.readValue(value: StompMessage, clazz: KClass<T>): T {
        return readValue<T>(value.payload, clazz.java)
    }

    private fun <T> Flowable<T>.customSubscribe(successConsumer: (T) -> Unit) {
        subscribeWithConsumers(successConsumer, { handleFailure(it) })
    }

    private fun <T> Observable<T>.call(successConsumer: (T) -> Unit) {
        call(successConsumer, { handleFailure(it) })
    }

}
