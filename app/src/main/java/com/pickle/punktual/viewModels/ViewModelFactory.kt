package com.pickle.punktual.viewModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.pickle.punktual.map.MapViewModel
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
