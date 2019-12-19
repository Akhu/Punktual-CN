package com.pickle.punktual.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pickle.punktual.PunktualApplication
import com.pickle.punktual.R
import com.pickle.punktual.user.UserRepository
import timber.log.Timber

class NotificationFCM : FirebaseMessagingService() {


    override fun onMessageReceived(remoteMessage: RemoteMessage) {

            // ...

            // TODO(developer): Handle FCM messages here, Build notification :)
            // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
            Timber.d("On message received From: %s", remoteMessage.from)

            // Check if message contains a data payload
            // Check if message contains a notification payload.
            Timber.d("Remote data received from notification : ${remoteMessage.data}")


            val userId = remoteMessage.data["userId"]
            val username = remoteMessage.data["username"]
            val messageToDisplay = remoteMessage.data["message"]

            if(userId == null && messageToDisplay == null && username == null){
                return
            }
            triggerNotification(context = this,
                notification = buildImageNotification(this,
                    "$username is arriving to Campus !",
                    "Someone is coming",
                    R.drawable.choo_choo,
                    PunktualApplication.NOTIFICATION_CHANNEL_ID_TEAM
                ),
                notificationId = NOTIFICATION_INCOMING_TEAM_PAPETERIE_ID
            )
    }

    override fun onNewToken(token: String) {
        PunktualApplication.repo.setCurrentUserPushToken(token)
        Timber.i("New Token received from FCM $token")
    }
}
