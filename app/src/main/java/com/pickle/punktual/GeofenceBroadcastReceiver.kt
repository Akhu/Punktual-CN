package com.pickle.punktual

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import timber.log.Timber

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if(geofencingEvent.hasError()){
            Timber.e( "Error getting geofencing event from broadcast receiver, code=${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        if(geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER){
            Timber.w("This transition is not handled $geofenceTransition")
            return
        }

        val triggeringGeofence = geofencingEvent.triggeringGeofences

        triggeringGeofence.forEach {
            if(it.requestId == PunktualApplication.GEOFENCE_PAPETERIE_ID){
                Timber.i("You are entering papeterie image factory")
            }
        }

    }
}
