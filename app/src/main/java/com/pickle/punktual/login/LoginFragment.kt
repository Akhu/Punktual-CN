package com.pickle.punktual.login


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.pickle.punktual.PunktualApplication
import com.pickle.punktual.R
import com.pickle.punktual.RegisterDialogFragment
import com.pickle.punktual.ViewModelFactoryRepository
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


        viewModel.getUiState().observe(this, updateUi())
        //Post for login
        //1. Handle login
        startButton.setOnClickListener {
            if(loginEditText.text.toString() !== "") {
                viewModel.connectUser(loginEditText.text.toString())
            }
        }

        registerButton.setOnClickListener {
            showDialog {username ->
                    viewModel.registerUser(username)
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

    private fun updateUi() = Observer<LoginUiState> {
        when(it){
            is LoginUiState.Normal -> {
                loadingProgressBar.visibility = View.INVISIBLE
                loginEditText.isEnabled = true
                startButton.isEnabled = true
                registerButton.isEnabled = true
            }
            is LoginUiState.Loading -> {
                loadingProgressBar.visibility = View.VISIBLE
                loginEditText.isEnabled = false
                startButton.isEnabled = false
                registerButton.isEnabled = false
                responseCall.text = ""
            }
            is LoginUiState.LoginSuccessful -> {
                loadingProgressBar.visibility = View.INVISIBLE
                loginEditText.isEnabled = true
                startButton.isEnabled = true
                registerButton.isEnabled = true
                responseCall.text = "Successfully connected ${it.user.username}"
                findNavController().navigate(R.id.action_loginFragment_to_mapFragment)
            }
            is LoginUiState.Error -> {
                loadingProgressBar.visibility = View.INVISIBLE
                loginEditText.isEnabled = true
                startButton.isEnabled = true
                registerButton.isEnabled = true
                responseCall.text = it.errorMessage
            }
        }
    }


    // Usage
    private fun showDialog(callback: (result: String) -> Unit) {
        val dialog = RegisterDialogFragment()

        dialog.onPositiveClick = {
            callback(it)
        }
        activity?.supportFragmentManager?.let { dialog.show(it, "RegisterDialog") }
    }

}
