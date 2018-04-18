package me.cooper.rick.crowdcontrollerclient.api.service.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.activity.MainActivity
import me.cooper.rick.crowdcontrollerclient.api.service.GeofenceTransitionsIntentService

// https://code.tutsplus.com/tutorials/android-fundamentals-intentservice-basics--mobile-6183
class ResponseReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        (context as? MainActivity)?.notifyGeofenceTransition(
                intent.getIntExtra(
                        context.getString(R.string.geofence_transition),
                        GeofenceTransitionsIntentService.UNINTERESTING_TRANSITION
                )
        )
    }

    companion object {
        const val ACTION = "ACTION_RESPONSE"
    }
}
