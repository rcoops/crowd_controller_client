package me.cooper.rick.crowdcontrollerclient.auth

import android.os.AsyncTask
import me.cooper.rick.crowdcontrollerclient.App
import me.cooper.rick.crowdcontrollerclient.persistence.AppDatabase


class DestroyTokenTask(private val func: () -> Unit) :
        AsyncTask<Void, Void, Void?>() {
    override fun doInBackground(vararg params: Void?): Void? {
        val db = AppDatabase.getInstance(App.currentActivity!!.applicationContext)
        val userDao = db.userDao()
        userDao.clear()
        return null
    }

    override fun onPostExecute(result: Void?) = func()

}