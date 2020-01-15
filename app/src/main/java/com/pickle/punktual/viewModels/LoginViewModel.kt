package com.pickle.punktual.viewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.pickle.punktual.user.User
import com.pickle.punktual.user.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import retrofit2.HttpException
import timber.log.Timber


sealed class LoginUiState {
    object Loading : LoginUiState()
    data class LoginSuccessful(val user: User) : LoginUiState()
    data class Error(val errorMessage: String, val httpException: HttpException? = null) :
        LoginUiState()
}

class LoginViewModel(userRepository: UserRepository) : ViewModel() {

    private val pushTokenDevice = MutableLiveData<String>()

    private val viewModelJob = SupervisorJob()

    private val viewModelScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    fun getUiState(): LiveData<LoginUiState> = uiState

    private val uiState = MediatorLiveData<LoginUiState>()

    fun getPushTokenDevice() : LiveData<String> = pushTokenDevice

    init {

        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                //Save token to server when it's successful
                if (!task.isSuccessful) {
                    Timber.w( task.exception, "getInstanceId failed")
                    return@OnCompleteListener
                }

                // Get new Instance ID token
                val token = task.result?.let { token ->
                    pushTokenDevice.value = token.token
                }
            })
    }

    fun registerUser(username: String) {


        /**
         * Updating our UI according to our background stuff
         */
        uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                //Launch Request
                //Get value and save it to repo

                userRepository.registerUser(username)
            } catch (httpException: HttpException) {
                if (httpException.code() == 401) {
                    //Logic to make user register instead of login
                    uiState.value = LoginUiState.Error(
                        "You already exists in this universe, please Login instead with your Astronaut Profile before",
                        httpException
                    )
                } else {
                    uiState.value = LoginUiState.Error(
                        "Bad request nor exists reason=${httpException.response()?.raw()}",
                        httpException
                    )
                }

                Timber.e("Error during request =${httpException.response()?.errorBody().toString()}")
                throw Exception("Unhandled status code=${httpException.code()}, server maybe have some issue ? Received=${httpException.message()}")

            } catch (exception: Exception) {
                // Updating UI  when error occurs
                uiState.value =
                    LoginUiState.Error("Exception occurred during request tentative reason=$exception")
                Timber.e(exception)
            }
        }
    }

    fun connectUser(userName: String) {
        uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                userRepository.loginUser(userName)
            } catch (httpException: HttpException) {
                if (httpException.code() == 404) {
                    //Logic to make user register instead of login
                    uiState.value = LoginUiState.Error(
                        "You are unknown to this universe, register your Astronaut Profile before",
                        httpException
                    )
                } else {
                    uiState.value = LoginUiState.Error(
                        "Bad request nor exists reason=${httpException.response()?.raw()}",
                        httpException
                    )
                    Timber.e("Error during request =${httpException.response()}")
                    throw Exception("Unhandled status code=${httpException.code()}, server maybe have some issue ? Received=${httpException.message()}")
                }
            } catch (exception: Exception) {
                // Updating UI  when error occurs
                uiState.value =
                    LoginUiState.Error("Exception occurred during request tentative reason=$exception")
                Timber.e(exception)
            }

        }
    }
}
