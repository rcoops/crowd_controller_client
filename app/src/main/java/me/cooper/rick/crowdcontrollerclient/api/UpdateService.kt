package me.cooper.rick.crowdcontrollerclient.api

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.cooper.rick.crowdcontrollerapi.dto.GroupDto
import me.cooper.rick.crowdcontrollerapi.dto.UserDto
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.api.client.UserClient
import me.cooper.rick.crowdcontrollerclient.util.ServiceGenerator
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS

class UpdateService : Service() {

    private val binder = LocalBinder()
    private val listeners = mutableListOf<UpdateServiceListener>()

    private val timer = Timer(true)

    private lateinit var userClient: UserClient

    override fun onBind(intent: Intent?): IBinder {
        val pref = getSharedPreferences("details", Context.MODE_PRIVATE)
        userClient = ServiceGenerator.createService(UserClient::class,
                pref.getString(getString(R.string.token), null))
        timer.schedule({
            userClient.user(pref.getLong(getString(R.string.user), -1L))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { updateListeners(it)} // TODO error handling
        }, 0L, SECONDS.toMillis(5))
        return binder
    }

    fun registerListener(listener: UpdateServiceListener) {
        listeners += listener
    }

    fun unregisterListener(listener: UpdateServiceListener) {
        listeners += listener
    }

    private fun updateListeners(userDto: UserDto) {
        listeners.forEach { it.onUpdate(userDto) }
    }

    private fun updateListeners(groupDto: GroupDto) {
        listeners.forEach { it.onUpdate(groupDto) }
    }

    inner class LocalBinder : Binder() {
        val service: UpdateService = this@UpdateService
    }

    interface UpdateServiceListener {
        fun onUpdate(userDto: UserDto)
        fun onUpdate(groupDto: GroupDto)
    }

    private fun Timer.schedule(task: () -> Unit, delay: Long, period: Long) {
        schedule(object: TimerTask() { override fun run() = task() }, delay, period)
    }

}
