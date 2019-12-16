package com.pickle.punktual.user

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations

class UserRepository {

    private val currentUser = MutableLiveData<User>()

    fun getCurrentUser() : LiveData<User> {
        return currentUser
    }

    fun connectUser(currentUser: User){
        this.currentUser.value = currentUser
    }

    fun setCurrentUserLocation(location: Location) {
        Transformations.map(currentUser) {
            it.setCurrentPositionFromLocation(location)
        }
    }
}