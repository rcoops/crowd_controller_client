package me.cooper.rick.crowdcontrollerclient.api

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.IBinder
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.cooper.rick.crowdcontrollerapi.dto.GroupDto
import me.cooper.rick.crowdcontrollerapi.dto.UserDto
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.api.client.GroupClient
import me.cooper.rick.crowdcontrollerclient.api.client.UserClient
import me.cooper.rick.crowdcontrollerclient.util.ServiceGenerator
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS

class UpdateService : Service(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val binder = LocalBinder()
    private val listeners = mutableListOf<UpdateServiceListener>()

    private val userTimer = Timer(true)
    private var groupTimer: Timer? = null

    private var userClient: UserClient? = null
    private var groupClient: GroupClient? = null

    private var token: String? = null
    private var userId: Long = -1
    private var groupId: Long? = null

    override fun onBind(intent: Intent?): IBinder {
        val pref = getSharedPreferences(getString(R.string.user_details), Context.MODE_PRIVATE)
        pref.registerOnSharedPreferenceChangeListener(this)
        token = pref.getString(getString(R.string.token), null)
        userId = pref.getLong(getString(R.string.user_id), -1)

        userClient = ServiceGenerator.createService(UserClient::class, token)

        userTimer.schedule({ getUser() }, 0L, SECONDS.toMillis(5))

        return binder
    }

    fun registerListener(listener: UpdateServiceListener) {
        listeners += listener
    }

    fun unregisterListener(listener: UpdateServiceListener) {
        listeners += listener
    }

    private fun getUser() {
        if (userId == -1L) return

        userClient?.user(userId)
                ?.subscribeOn(Schedulers.io())
                ?.observeOn(AndroidSchedulers.mainThread())
                ?.subscribe { updateListeners(it) } // TODO error handling
    }

    private fun getGroup() {
        groupId?.let {
            groupClient?.groupObservable(it)
                    ?.subscribeOn(Schedulers.io())
                    ?.observeOn(AndroidSchedulers.mainThread())
                    ?.subscribe { updateListeners(it) } // TODO error handling
        }
    }

    private fun updateListeners(userDto: UserDto) {
        adjustGroup(userDto)
        listeners.forEach { it.onUpdate(userDto) }
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
    }

    private fun unScheduleGroupRequest() {
        groupTimer?.cancel()
        groupTimer?.purge()
        groupTimer = null
        groupClient = null
    }

    private fun resetGroupSchedule() {
        unScheduleGroupRequest()
        scheduleGroupRequest()
    }

    private fun updateListeners(groupDto: GroupDto) {
        listeners.forEach { it.onUpdate(groupDto) }
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
    }

    private fun Timer.schedule(task: () -> Unit, delay: Long, period: Long) {
        schedule(object : TimerTask() { override fun run() = task() }, delay, period)
    }

}
