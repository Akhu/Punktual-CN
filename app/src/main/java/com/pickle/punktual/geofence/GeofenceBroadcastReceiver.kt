package com.pickle.punktual.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.pickle.punktual.PunktualApplication
import com.pickle.punktual.R
import com.pickle.punktual.network.PunktualNetworkService
import com.pickle.punktual.notifications.NOTIFICATION_INCOMING_SELF_PAPETERIE_ID
import com.pickle.punktual.notifications.buildImageNotification
import com.pickle.punktual.notifications.triggerNotification
import com.pickle.punktual.position.LocationType
import com.pickle.punktual.position.Position
import kotlinx.coroutines.*
import retrofit2.HttpException
import timber.log.Timber


class GeofenceBroadcastReceiver : BroadcastReceiver() {

    private val networkCoroutine = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
       if(intent.getStringExtra(PunktualApplication.PENDING_INTENT_EXTRA_USER_ID_KEY) == null) {
           Timber.e("Missing user ID from intent")
           return
       }

        val userId = intent.getStringExtra(PunktualApplication.PENDING_INTENT_EXTRA_USER_ID_KEY)!!

        Timber.i("User id received from intent $userId")

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
        val triggeringPosition = Position(geofencingEvent.triggeringLocation.latitude, geofencingEvent.triggeringLocation.longitude)
        triggeringGeofence.forEach {
            if(it.requestId == PunktualApplication.GEOFENCE_PAPETERIE_ID){

                //Send notification to the team via server
                savePositionToServerAndNotifyTeam(triggeringPosition, userId)
                Timber.i("You are entering papeterie image factory")
                triggerNotification(context, buildImageNotification(
                    context,
                    "Sending notification to the team",
                    "You are arriving to the Papeterie !",
                    R.drawable.papeterie_1
                ), NOTIFICATION_INCOMING_SELF_PAPETERIE_ID)
            }
        }
    }

    private fun savePositionToServerAndNotifyTeam(
        position: Position,
        userId: String
    ) = networkCoroutine.launch {
            Timber.w("Position to send =$position, userId=$userId, type=${LocationType.PAPETERIE.name}")
            val positionRegister = PunktualNetworkService.position.registerPosition(
                type = LocationType.PAPETERIE.name,
                userId = userId,
                position = position)
        Timber.w("RawResponse =${positionRegister.raw().body.toString()}")
        if (positionRegister.isSuccessful && positionRegister.code() == 202) {
            Timber.i("Successfully sent geofence event to the server")
        } else {
            Timber.e("Could not communicate with the server", HttpException(positionRegister))
        }
    }

}
