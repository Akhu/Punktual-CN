package com.pickle.punktual.map

import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.pickle.punktual.position.Position
import com.pickle.punktual.user.User

//45.901592, 6.117491

fun GoogleMap.addUserMarker(user: User, position: Position){
    val options = MarkerOptions()
        .position(LatLng(position.latitude, position.longitude))
        .title(user.username)
        .snippet(user.type.toString())

    val marker = this.addMarker(options)
    marker.tag = user
}