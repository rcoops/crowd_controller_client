package me.cooper.rick.crowdcontrollerclient.api

import android.app.Service
import android.content.Intent
import android.os.AsyncTask
import android.os.Binder
import android.os.IBinder
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.cooper.rick.crowdcontrollerapi.dto.GroupDto
import me.cooper.rick.crowdcontrollerapi.dto.UserDto
import me.cooper.rick.crowdcontrollerclient.api.client.UserClient
import me.cooper.rick.crowdcontrollerclient.persistence.AppDatabase
import me.cooper.rick.crowdcontrollerclient.persistence.model.UserEntity
import me.cooper.rick.crowdcontrollerclient.util.ServiceGenerator
import java.util.*
import java.util.concurrent.TimeUnit.SECONDS

class UpdateService : Service() {

    private val binder: IBinder = LocalBinder()
    private val listeners = mutableListOf<UpdateServiceListener>()

    private val random: Random = Random()
    private val timer = Timer(true)

    private var observable: Observable<UserDto>? = null
    private lateinit var userClient: UserClient

    private val loginSuccess: (token: UserEntity) -> Unit = {
        userClient = ServiceGenerator.createService(UserClient::class, it.token!!)
        timer.schedule({
            observable = userClient.userO(it.id!!)
            observable!!.subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { updateListeners(it)}
        }, 0L, SECONDS.toMillis(5))
        task?.let { it.cancel(true )
            task == null
        }
    }

    private var task: CheckTokenTask? = null
    init {
        task = CheckTokenTask().apply { execute() }
    }

    override fun onBind(intent: Intent?): IBinder = binder

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

    inner class CheckTokenTask : AsyncTask<Void, Void, UserEntity?>() {

        override fun doInBackground(vararg params: Void): UserEntity? {
            val db = AppDatabase.getInstance(this@UpdateService)
            val userDao = db.userDao()
            return userDao.select()
        }

        override fun onPostExecute(result: UserEntity?) {
            loginSuccess(result!!)
        }

    }

}
