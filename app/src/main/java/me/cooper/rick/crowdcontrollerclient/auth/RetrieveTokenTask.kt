package me.cooper.rick.crowdcontrollerclient.auth

import android.app.Activity
import android.arch.persistence.room.Room
import android.os.AsyncTask
import android.os.Build
import me.cooper.rick.crowdcontrollerclient.domain.AppDatabase
import me.cooper.rick.crowdcontrollerclient.domain.entity.TokenEntity
import java.lang.ref.WeakReference

class RetrieveTokenTask(activity: Activity,
                        private val function: (TokenEntity) -> Unit) :
        AsyncTask<Void, Void, TokenEntity?>() {

    private val weakActivity = WeakReference<Activity>(activity)

    override fun doInBackground(vararg params: Void): TokenEntity? {
        val activity = weakActivity.get()
        if (isInvalid(activity)) return null
        val db = AppDatabase.getInstance(activity!!.applicationContext)

        val dao = db.tokenDao()
        return dao.select()
    }

    override fun onPostExecute(result: TokenEntity?) {
        result?.let { function(result) }
    }

    private fun isInvalid(activity: Activity?): Boolean {
        return activity == null || activity.isFinishing ||
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed
    }

}