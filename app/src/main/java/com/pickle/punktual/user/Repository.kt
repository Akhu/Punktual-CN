package com.pickle.punktual.user

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class UserRepository {

    private val currentUser = MutableLiveData<User>()

    fun getCurrentUser() : LiveData<User> {
        return currentUser
    }

    fun connectUser(currentUser: User){
        this.currentUser.value = currentUser
    }
}