package me.cooper.rick.crowdcontrollerclient.api.task

import android.os.AsyncTask
import me.cooper.rick.crowdcontrollerclient.App
import me.cooper.rick.crowdcontrollerclient.api.client.UserClient
import me.cooper.rick.crowdcontrollerclient.api.util.handleConnectionException
import me.cooper.rick.crowdcontrollerclient.domain.AppDatabase
import me.cooper.rick.crowdcontrollerclient.util.ServiceGenerator
import retrofit2.Call
import retrofit2.Response
import java.io.IOException


abstract class UserTask<T>(private val consumer: (T) -> Unit) : AsyncTask<Void, Void, Response<T>>() {

    protected abstract fun buildCall(userClient: UserClient, id: Long): Call<T>

    override fun doInBackground(vararg params: Void): Response<T> {
        val db = AppDatabase.getInstance(App.currentActivity!!)
        val user = db.userDao().select()!!
        val userClient = ServiceGenerator.createService(UserClient::class, user.token)

        return try {
            buildCall(userClient, user.id!!).execute()
        } catch (e: IOException) {
            handleConnectionException(e)
        }
    }

    override fun onPostExecute(response: Response<T>) {
        App.currentActivity?.handleResponse(response, consumer)
    }

}
