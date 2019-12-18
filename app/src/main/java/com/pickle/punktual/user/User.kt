package com.pickle.punktual.user

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.pickle.punktual.position.Position
import com.pickle.punktual.position.PositionHistory
import org.joda.time.DateTime
import java.util.*
import kotlin.collections.ArrayList

enum class UserType {
    STUDENT, TEACHER
}

data class User(val id: String, val type: UserType = UserType.STUDENT, val username: String, val pushToken: String? = null, val imageUrl: String? = null) {
    //Joda time used : https://www.joda.org/joda-time/quickstart.html

    var lastPosition : Position? = null

    fun setCurrentPositionFromLocation(location: Location){
        lastPosition = Position(location.latitude, location.longitude)
    }
}

data class UserRegister(val username: String, val pushToken: String?)

data class UserLogin(val id: String, val username: String)