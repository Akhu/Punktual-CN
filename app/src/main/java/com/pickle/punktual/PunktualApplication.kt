package com.pickle.punktual

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.pickle.punktual.user.UserRepository
import timber.log.Timber

class PunktualApplication : Application() {

    companion object {
        const val GEOFENCE_PAPETERIE_ID = "papeterie"
        const val NOTIFICATION_CHANNEL_ID_USER = "UserPosition"
        const val NOTIFICATION_CHANNEL_ID_TEAM = "TeamPosition"
        val repo = UserRepository()
    }

    override fun onCreate() {
        super.onCreate()

        createNotificationChannels()

        Timber.plant(Timber.DebugTree())
    }

    private fun createNotificationChannels() {
        // We don't handle notification channel if version is older than Oreo
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val userChannel = userChannel()
        val teamChannel = teamChannel()
        //Adding our channel inside the settings of the app
        //We tell the OS
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(userChannel)
        notificationManager.createNotificationChannel(teamChannel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun userChannel(): NotificationChannel {
        val name = "Punktual notification channel - User"
        val descriptionText = "Be notified when you are arriving Papeterie"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        return NotificationChannel(NOTIFICATION_CHANNEL_ID_USER, name, importance).apply {
            description = descriptionText
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun teamChannel(): NotificationChannel {
        val name = "Punktual notification channel - Team"
        val descriptionText = "Be notified when a team mate is arriving Papeterie"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        return NotificationChannel(NOTIFICATION_CHANNEL_ID_TEAM, name, importance).apply {
            description = descriptionText
        }
    }
}

