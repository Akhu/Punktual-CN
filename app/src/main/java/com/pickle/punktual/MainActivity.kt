package com.pickle.punktual

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.pickle.punktual.map.MapActivity
import com.pickle.punktual.network.NetworkService
import com.pickle.punktual.user.User
import com.pickle.punktual.user.UserRegister
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private var pushToken: String? = null

    private val callback = object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Timber.e("Exception $e")
        }
        override fun onResponse(call: Call, response: Response) {
            Timber.i("Received response : ${response.message}")
            val adapter = NetworkService.moshi.adapter(User::class.java)
            response.body?.let { responseBody ->
                adapter.fromJson(responseBody.string())?.let {user ->
                    GlobalScope.launch {
                        withContext(Dispatchers.Main){
                            openMapActivity(user)
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startButton.isEnabled = false

        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                //Save token to server when it's successful
                if (!task.isSuccessful) {
                    Timber.w( task.exception, "getInstanceId failed")
                    return@OnCompleteListener
                }

                // Get new Instance ID token
                val token = task.result?.let {token ->

                    startButton.isEnabled = true

                    pushToken = token.token
                    // Log and toast
                    val msg = token.token
                    Timber.d( msg)
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }

            })

        //Post for login
        //1. Handle login
        startButton.setOnClickListener {
            pushToken?.let {
                loginNetworkRequest(loginEditText.text.toString(), it)?.let { request ->
                    NetworkService.client.newCall(request).enqueue(callback)
                }
            }
        }

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

    private fun openMapActivity(user: User){
        PunktualApplication.repo.connectUser(user)
        //save response back to shared preference
        val intent = Intent(this, MapActivity::class.java)
        startActivity(intent)
    }

    private fun loginNetworkRequest(username: String, token : String) : Request? {
        val userRegister = UserRegister(username = username, pushToken = token)
        val adapter = NetworkService.moshi.adapter(UserRegister::class.java)
        adapter.toJson(userRegister)?.let{
            val requestBody = it.toRequestBody(NetworkService.mediaTypeJson)
            val url = with(HttpUrl.Builder()){
                scheme(NetworkService.baseUrl.scheme)
                port(NetworkService.baseUrl.port)
                host(NetworkService.baseUrl.host)
                addPathSegment("register")
                build()
            }
            return with(Request.Builder()) {
                post(requestBody)
                url(url)
                build()
            }
        }
        return null
    }
}
