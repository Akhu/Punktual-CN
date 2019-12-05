package com.pickle.punktual

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import timber.log.Timber

class NotificationFCM : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage?) {

            // ...

            // TODO(developer): Handle FCM messages here, Build notification :)
            // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
            Timber.d("On message receveid From: %s", remoteMessage?.from)

        // Check if message contains a data payload
            // Check if message contains a notification payload.
            /*if (remoteMessage.getNotification() != null) {
                Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            }*/


            // Also if you intend on generating your own notifications as a result of a received FCM
            // message, here is where that should be initiated. See sendNotification method below.
    }

    override fun onNewToken(token: String?) {
        Timber.i("New Token received from FCM $token")
    }
}
