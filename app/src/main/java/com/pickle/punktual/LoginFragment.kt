package com.pickle.punktual


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.pickle.punktual.viewModels.LoginViewModel
import com.pickle.punktual.viewModels.ViewModelFactoryRepository
import kotlinx.android.synthetic.main.fragment_login.*

/**
 * A simple [Fragment] subclass.
 */
class LoginFragment : Fragment() {

    val viewModel : LoginViewModel by viewModels {
        ViewModelFactoryRepository(PunktualApplication.repo)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        val binding =inflater.inflate(R.layout.fragment_login, container, false)

        return binding.rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Post for login
        //1. Handle login
        startButton.setOnClickListener {
            /*pushToken?.let {
                loginNetworkRequest(loginEditText.text.toString(), it)?.let { request ->
                    NetworkService.client.newCall(request).enqueue(callback)
                }
            }*/
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


}
