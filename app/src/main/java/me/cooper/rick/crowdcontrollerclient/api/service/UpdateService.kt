package me.cooper.rick.crowdcontrollerclient.api.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.net.ConnectivityManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.maps.model.LatLng
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
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.geofence
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.geofenceCentre
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.geofenceLimit
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.getGroup
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.group
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.lastLocation
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
import java.net.UnknownHostException
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS
import javax.net.ssl.SSLHandshakeException
import kotlin.reflect.KClass

class UpdateService : Service(), OnSharedPreferenceChangeListener {

    private val binder = LocalBinder()

    private var listener: UpdateServiceListener? = null
    private lateinit var pref: SharedPreferences

    private var userClient: UserClient? = null
    private lateinit var stompClient: StompClient
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null

    private var latestGroupId: Long? = null

    private var reconnectTimer: Timer? = null

    private val locationRequest = LocationRequest.create().apply {
        interval = SECONDS.toMillis(10)
        fastestInterval = SECONDS.toMillis(10)
        priority = PRIORITY_HIGH_ACCURACY
    }

    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            lastLocation = LatLng(
                    locationResult.lastLocation.latitude,
                    locationResult.lastLocation.longitude
            )
            listener?.updateMapSelfLocation(lastLocation!!)
            val userId = getUserId()
            if (userId != -1L && isConnected()) {
                tryHttpRequest({
                    sendLocation(createLocationDto(userId, lastLocation!!))
                })
            }
        }
    }

    private fun createLocationDto(userId: Long, lastLocation: LatLng): LocationDto {
        return LocationDto(userId, lastLocation.latitude, lastLocation.longitude)
    }

    override fun onBind(intent: Intent?): IBinder {
        pref = getSharedPreferences(getString(R.string.user_details), MODE_PRIVATE)
        pref.registerOnSharedPreferenceChangeListener(this)

        userClient = createService(UserClient::class, getToken())
        stompClient = Stomp.over(WebSocket::class.java, "$BASE_WS_URL/chat/websocket")
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

    fun cancelLocationUpdates() {
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback)
    }

    private fun getUserId() = pref.getLong(getString(R.string.pref_user_id), -1)

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

        reconnectWebsocket(userId)
    }

    private fun reconnectWebsocket(userId: Long) {
        if (!stompClient.isConnected && !stompClient.isConnecting) connectStompClient()
        subscribeToUserUpdates(userId)
    }

    private fun getUser(userId: Long) {
        if (isConnected()) {
            tryHttpRequest({
                userClient!!.find(userId).call({
                    Log.d("GET USER", it.toString())
                    listener?.setHeader(it)
                    onUserUpdate(it)
                    adjustGroup(it)
                })
            })
        }
    }

    private fun tryHttpRequest(consumer: () -> Unit, errorConsumer: (() -> Unit)? = null) {
        try {
            consumer()
        } catch (e: Exception) {
            when (e) {
                is SSLHandshakeException, is UnknownHostException -> {
                    errorConsumer?.invoke()
                }
                else -> throw e
            }
        }
    }

    private fun isConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

    @SuppressLint("CommitPrefEdits")
    private fun onUserUpdate(userDto: UserDto) {
        pref.edit().apply {
            putLong(getString(R.string.pref_user_id), userDto.id)
            commit()
        }
        updateFriends(userDto.friends)
        if (userDto.group != null && userDto.group != group?.id) {
            userDto.group.let { getGroup(it) }
        }
        listener?.updateNavMenu(userDto.group != null, userDto.groupAccepted)
    }

    private fun subscribeToGroupUpdates(groupId: Long) {
        stompClient.topic("/topic/group/$groupId")
                .takeUntil { latestGroupId == null }
                .customSubscribe({
                    Log.d(TAG, "Received group update: " + it.payload)
                    try {
                        val groupDto = jackson.readValue(it, GroupDto::class)
                        refreshGroupDetails(groupDto)
                        createGeofence(groupDto)
                    } catch (e: UnrecognizedPropertyException) {
                        val dto = jackson.readValue(it, APIErrorDto::class)
                        ApiService.refreshGroupDetails(null)
                        listener?.notifyOfGroupExpiry(dto)
                    }
                })
    }

    fun ensureGeofenceExists() {
        if (geofence == null) {
            latestGroupId?.let { ApiService.getGroup(it, { createGeofence(it) }) }
        }
    }

    private fun createGeofence(groupDto: GroupDto) {
        groupDto.location?.let { location ->
            groupDto.settings?.let { settings ->
                listener?.run {
                    geofenceLimit = settings.minClusterRadius
                    geofenceCentre = LatLng(location.latitude!!, location.longitude!!)
                    setGeofence(geofenceCentre, geofenceLimit!!.toFloat())
                    removeGeofence()
                    addGeofence()
                }
            }
        }
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

    private fun resubscribe() {
        if (reconnectTimer != null) return
        reconnectTimer = Timer(true)
        reconnectTimer?.schedule(
                { connectStompClient() },
                SECONDS.toMillis(3),
                SECONDS.toMillis(3)
        )
    }

    private fun cancelTimer() {
        if (reconnectTimer == null) return
        reconnectTimer?.cancel()
        reconnectTimer?.purge()
        reconnectTimer = null
    }

    private fun connectStompClient() {
        stompClient.disconnect()
        stompClient = Stomp.over(WebSocket::class.java, "$BASE_WS_URL/chat/websocket")

        if (!isConnected()) {
            resubscribe(); return
        }
        cancelTimer()

        tryHttpRequest({
            stompClient.connect(true)

            stompClient.lifecycle()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { lifecycleEvent ->
                        when (lifecycleEvent.type!!) {
                            OPENED -> Log.d("opened", "Stomp connection opened")
                            ERROR -> {
                                Log.e(TAG, "Stomp connection error", lifecycleEvent.exception)
                                resubscribe()
                            }
                            CLOSED -> {
                                Log.d("closed", "Stomp connection closed")
                                resubscribe()
                            }
                        }
                    }
        }, { resubscribe() })
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
            cancelLocationUpdates()
            latestGroupId?.let { listener?.removeGeofence() }
            unscheduleLocationUpdates()
        } else if (latestGroupId != userDto.group && userDto.groupAccepted) {
            scheduleGroupRequest(userDto.group!!)
        }
        if (userDto.group != null && !userDto.groupAccepted) {
            listener?.notifyUserOfGroupInvite(userDto.group!!, userDto.groupAdmin!!)
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
        fun updateNavMenu(isGrouped: Boolean, hasAccepted: Boolean)
        fun requestPermissions()
        fun notifyUserOfGroupInvite(groupId: Long, groupAdmin: String)
        fun handleApiException(e: Throwable)
        fun notifyOfGroupExpiry(dto: APIErrorDto)
        fun updateMapSelfLocation(latLng: LatLng)
        fun setGeofence(centre: LatLng, radius: Float)
        fun addGeofence()
        fun removeGeofence()
        fun setHeader(userDto: UserDto)
    }

    companion object {
        private val jackson = ObjectMapper()
    }

    private fun Timer.schedule(task: () -> Unit, delay: Long, period: Long) {
        schedule(object : TimerTask() {
            override fun run() = task()
        }, delay, period)
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
