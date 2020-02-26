package com.pickle.punktual.user

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.pickle.punktual.network.PunktualNetworkService
import com.pickle.punktual.position.Position
import retrofit2.HttpException
import timber.log.Timber

class UserRepository {

    private val currentUser = MutableLiveData<User>()

    fun getCurrentUser() : LiveData<User> {
        return currentUser
    }

    fun connectUser(currentUser: User){
        this.currentUser.value = currentUser
    }

    fun setCurrentUserLocation(location: Location) {
        currentUser.value?.let { currentUserValue ->
            currentUserValue.lastPosition = Position(location.latitude, location.longitude)
            currentUser.value = currentUserValue
        }
    }

    fun setCurrentUserPushToken(token: String) {
        currentUser.value?.let { currentUserValue ->
            currentUserValue.pushToken = token
            currentUser.value = currentUserValue
        }
    }


    suspend fun registerUser(login: String, pushToken: String?) {
        Timber.d("Login user request will launch with login=$login")

        val registeredUser = PunktualNetworkService.userService.registerUser(
            UserRegister(login, pushToken)
        )
        registeredUser?.let {
            if (it.isSuccessful) {
                currentUser.postValue(it.body())
            } else {
                throw HttpException(it)
            }
        }

    }

    /**
     *
     */
    suspend fun loginUser(userName: String, pushToken: String?) {
        pushToken?.let { pushToken  ->
            val user = UserLogin(userName, pushToken)
            val loginUser = PunktualNetworkService.userService.loginUser(user)
            if (loginUser.isSuccessful) {
                currentUser.postValue(loginUser.body())
            } else {
                throw HttpException(loginUser)
            }
        }?: run {
            throw Exception("Missing pushToken Exception !")
        }

    }
}