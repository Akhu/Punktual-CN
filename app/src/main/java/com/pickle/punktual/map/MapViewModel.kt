package com.pickle.punktual.map

import android.location.Location
import androidx.lifecycle.*
import com.pickle.punktual.user.User
import com.pickle.punktual.user.UserRepository
import java.lang.IllegalArgumentException

@Suppress("UNCHECKED_CAST")
class ViewModelFactoryRepository(private val userRepo: UserRepository) : ViewModelProvider.NewInstanceFactory(){
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(MapViewModel::class.java) -> MapViewModel(
                userRepo
            )
            else -> throw IllegalArgumentException("Unexpected model class $modelClass")
        } as T
    }
}

class MapViewModel(private val repository: UserRepository) : ViewModel() {

    fun getCurrentUser() : LiveData<User>{
        return repository.getCurrentUser()
    }

    fun loadUserWithPosition(location: Location) {
        repository.setCurrentUserLocation(location)
    }
}