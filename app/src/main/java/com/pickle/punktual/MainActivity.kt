package com.pickle.punktual

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Timber.w( task.exception, "getInstanceId failed")
                    return@OnCompleteListener
                }

                // Get new Instance ID token
                val token = task.result?.token

                // Log and toast
                val msg = token
                Timber.d( msg)
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            })

        //1. Handle login
        //2. Ask for location permissions
        //3. Handle location settings
        //4. Create geofence
        //5. Start to listen for location changes
        //6. Create Notification when entering the fence
        //6. Implement google map to better see things
        //7. Put marker on Map for the papeterie and current user
        //8. Communicate with server to log arrival date time + user
        //9. Handle external notification from FCM
        //10. Improvments : Google Login with profile picture
        //11. Improvments : Some statistics with Android Chart
        //12. Improvments : Detecting the type of transport user for a user + send to server

    }
}
