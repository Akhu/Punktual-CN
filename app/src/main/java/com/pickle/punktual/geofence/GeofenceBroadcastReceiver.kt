package com.pickle.punktual.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.pickle.punktual.PunktualApplication
import com.pickle.punktual.R
import com.pickle.punktual.notifications.NOTIFICATION_INCOMING_SELF_PAPETERIE_ID
import com.pickle.punktual.notifications.buildImageNotification
import com.pickle.punktual.notifications.triggerNotification
import com.pickle.punktual.position.Position
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import timber.log.Timber
import java.io.IOException
import java.net.URL




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
    ) {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(Position::class.java)
        val mediaTypeJson = "application/json; charset=utf-8".toMediaType()
        adapter.toJson(position)?.let {
            val client = OkHttpClient()
            val body = it.toRequestBody(mediaTypeJson)
            val url = with(HttpUrl.Builder()) {
                scheme("http")
                host("10.0.2.2")
                port(8080)
                addPathSegments("/position/register")
                addQueryParameter("userId", userId)
                build()
            }
            val request = with(Request.Builder()) {
                post(body)
                url(url)
                addHeader("Content-Type", "application/json")
                build()
            }

            client.newCall(request).enqueue(responseCallback = object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Timber.e("Exception when calling network ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    Timber.d("Server response : ${response.body}")
                }

            })


        }
    }
}
