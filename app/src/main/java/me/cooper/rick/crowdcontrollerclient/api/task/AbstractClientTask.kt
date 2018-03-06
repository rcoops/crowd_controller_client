package me.cooper.rick.crowdcontrollerclient.api.task

import android.content.Context
import android.os.AsyncTask
import me.cooper.rick.crowdcontrollerclient.App
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.api.util.handleConnectionException
import me.cooper.rick.crowdcontrollerclient.util.ServiceGenerator
import retrofit2.Call
import retrofit2.Response
import java.io.IOException
import kotlin.reflect.KClass

abstract class AbstractClientTask<in S : Any, T>(private val consumer: (T) -> Unit,
                                                 private val clazz: KClass<S>) : AsyncTask<Void, Void, Response<T>>() {

    protected abstract fun buildCall(client: S, id: Long): Call<T>

    override fun doInBackground(vararg params: Void): Response<T> {
        val pref = App.context!!.getSharedPreferences("details", Context.MODE_PRIVATE)

        val tokenKey = App.context!!.getString(R.string.token)
        val token = pref.getString(tokenKey, null)
        val client = ServiceGenerator.createService(clazz, token)

        return try {
            buildCall(client, pref.getLong(App.context!!.getString(R.string.user), -1L)).execute()
        } catch (e: IOException) {
            handleConnectionException(e)
        }
    }

    override fun onPostExecute(response: Response<T>) {
        App.currentActivity?.handleResponse(response, consumer)
    }

}
