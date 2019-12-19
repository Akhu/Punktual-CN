package com.pickle.punktual.user

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.pickle.punktual.position.Position

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
}