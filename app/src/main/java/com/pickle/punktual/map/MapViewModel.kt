package com.pickle.punktual.map

import android.location.Location
import androidx.lifecycle.*
import com.pickle.punktual.network.PunktualNetworkService
import com.pickle.punktual.position.LocationType
import com.pickle.punktual.position.Position
import com.pickle.punktual.user.User
import com.pickle.punktual.user.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import retrofit2.HttpException
import timber.log.Timber

sealed class MapUiState {
    object Loading: MapUiState()
    data class Error(val errorMessage: String): MapUiState()
    data class Ready(
        val user: User,
        val papeteriePointOfInterest: Position
    ) : MapUiState()
}

private val papeteriePosition = Position(45.907888, 6.102780)

class MapViewModel(private val repository: UserRepository) : ViewModel() {

    private val viewModelJob = SupervisorJob()

    private val viewModelScope = CoroutineScope(viewModelJob + Dispatchers.Main)

    private val uiState = MutableLiveData<MapUiState>()

    private val checkInState = MutableLiveData<Boolean>()

    fun getUiState() : LiveData<MapUiState> = uiState

    fun loadMapData(location: Location){
        Timber.i("Loading data from location")

        if(!(location.latitude in - 90.0..90.0 && location.longitude in -180.0..180.0)){
            uiState.value = MapUiState.Error("Invalid GPS Coordinate received !")
            return
        }

        //Do loading stuff from network
        uiState.value = MapUiState.Loading

        repository.setCurrentUserLocation(location)

        uiState.value = MapUiState.Ready(
            getCurrentUser().value!!,
            papeteriePosition
        )
    }

    fun getCurrentUser() : LiveData<User>{
        return repository.getCurrentUser()
    }

    fun loadUserWithPosition(location: Location) {
        repository.setCurrentUserLocation(location)
    }

    fun checkInNcf(locationType: LocationType) {
        getCurrentUser().value?.let { user ->
            user.lastPosition?.let { position ->
                viewModelScope.launch {
                    val checkingInRequest = PunktualNetworkService.position.registerPosition(
                        LocationType.CAMPUS_NUMERIQUE.name,
                        user.id,
                        position
                        )
                    if(checkingInRequest.isSuccessful){
                        checkInState.value = true
                    } else {
                        uiState.value = MapUiState.Error(HttpException(checkingInRequest).message())
                        checkInState.value = false
                    }
                }
            }
        }

    }
}