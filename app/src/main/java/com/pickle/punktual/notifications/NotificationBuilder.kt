package com.pickle.punktual.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pickle.punktual.PunktualApplication
import com.pickle.punktual.R
import com.pickle.punktual.map.MapActivity

const val NOTIFICATION_INCOMING_SELF_PAPETERIE_ID = 1001
const val NOTIFICATION_INCOMING_TEAM_PAPETERIE_ID = 2001

fun buildImageNotification(context: Context, text: String, title: String, drawable: Int, channel: String = PunktualApplication.NOTIFICATION_CHANNEL_ID_USER) : Notification{
    val drawableImage = ContextCompat.getDrawable(context, drawable)!!
    val bitmap = (drawableImage as BitmapDrawable).bitmap

    //To understand more about Intents : https://developer.android.com/reference/android/content/Intent
    val intent = Intent(context, MapActivity::class.java).apply {
        //Flag documentation : https://developer.android.com/reference/android/content/Intent#FLAG_ACTIVITY_NEW_TASK
        //We want to start a new Activity from the click
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    //Assign our intent to a pending intent. Meaning that intent is "waiting" for triggering
    val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

    //We build our notification with the Compat component and attribute a channel ID
    return with(NotificationCompat.Builder(context, PunktualApplication.NOTIFICATION_CHANNEL_ID_USER)) {
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
        build()
    }
}

fun triggerNotification(context: Context, notification: Notification, notificationId: Int) {
    NotificationManagerCompat
        .from(context)
        .notify(notificationId, notification)
}