package com.pickle.punktual.user

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import com.pickle.punktual.position.Position
import com.pickle.punktual.position.PositionHistory
import com.squareup.moshi.JsonClass
import org.joda.time.DateTime
import java.util.*
import kotlin.collections.ArrayList

enum class UserType {
    STUDENT, TEACHER
}

@JsonClass(generateAdapter = true)
data class User(val id: String, val type: UserType = UserType.STUDENT, val username: String, var pushToken: String? = null, val imageUrl: String? = null, var isLogged: Boolean = false) {
    //Joda time used : https://www.joda.org/joda-time/quickstart.html

    var lastPosition : Position? = null

    fun setCurrentPositionFromLocation(location: Location){
        lastPosition = Position(location.latitude, location.longitude)
    }
}
@JsonClass(generateAdapter = true)
data class UserRegister(val username: String, val pushToken: String?)

@JsonClass(generateAdapter = true)
data class UserLogin(val username: String, val pushToken: String)