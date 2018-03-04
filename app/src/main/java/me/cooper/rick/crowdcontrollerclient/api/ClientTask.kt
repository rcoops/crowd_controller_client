package me.cooper.rick.crowdcontrollerclient.api

import android.os.AsyncTask
import me.cooper.rick.crowdcontrollerclient.App
import me.cooper.rick.crowdcontrollerclient.api.util.handleConnectionException
import me.cooper.rick.crowdcontrollerclient.persistence.AppDatabase
import me.cooper.rick.crowdcontrollerclient.util.ServiceGenerator
import retrofit2.Call
import retrofit2.Response
import java.io.IOException
import kotlin.reflect.KClass

abstract class ClientTask<in S : Any, T>(private val consumer: (T) -> Unit,
                                         private val clazz: KClass<S>) : AsyncTask<Void, Void, Response<T>>() {

    protected abstract fun buildCall(client: S, id: Long): Call<T>

    override fun doInBackground(vararg params: Void): Response<T> {
        val db = AppDatabase.getInstance(App.currentActivity!!)
        val user = db.userDao().select()!!
        val client = ServiceGenerator.createService(clazz, user.token)

        return try {
            buildCall(client, user.id!!).execute()
        } catch (e: IOException) {
            handleConnectionException(e)
        }
    }

    override fun onPostExecute(response: Response<T>) {
        App.currentActivity?.handleResponse(response, consumer)
    }

}