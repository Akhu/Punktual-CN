package com.pickle.punktual.geofence

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.pickle.punktual.PunktualApplication
import com.pickle.punktual.R
import com.pickle.punktual.map.MapActivity
import timber.log.Timber

const val NOTIFICATION_PAPETERIE_ID = 1001

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
                triggerNotification(context, geofenceTransition)
            }
        }

    }

    private fun triggerNotification(context: Context, geofenceTransition: Int) {

        val text = "Sending a notification to the team !"
        val title = "Entering: Papeterie Image factory"
        val drawable = ContextCompat.getDrawable(context, R.drawable.papeterie_1)!!
        val bitmap = (drawable as BitmapDrawable).bitmap

        //To understand more about Intents : https://developer.android.com/reference/android/content/Intent
        val intent = Intent(context, MapActivity::class.java).apply {
            //Flag documentation : https://developer.android.com/reference/android/content/Intent#FLAG_ACTIVITY_NEW_TASK
            //We want to start a new Activity from the click
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        //Assign our intent to a pending intent. Meaning that intent is "waiting" for triggering
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

        //We build our notification with the Compat component and attribute a channel ID
        val notificationBuilder = with(NotificationCompat.Builder(context, PunktualApplication.NOTIFICATION_CHANNEL_ID_USER)) {
            setContentText(text)
            setLargeIcon(bitmap)
            setSmallIcon(R.drawable.ic_place_white_24dp)
            setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null))
            //We assign our action intent to the notification.
            setContentIntent(pendingIntent)
            setAutoCancel(true)
            setContentTitle(title)
        }

        //You can launch notification by calling .notify and finishing building your notification
        NotificationManagerCompat.from(context).notify(NOTIFICATION_PAPETERIE_ID, notificationBuilder.build())
    }
}
