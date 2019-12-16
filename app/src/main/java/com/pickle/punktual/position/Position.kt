package com.pickle.punktual.position

import android.location.Location
import org.joda.time.DateTime
import java.lang.Exception

data class LocationData(val location: Location? = null, val exception: Exception? = null)

data class Position(val latitude: Double, val longitude: Double)

data class PositionHistory(val date: DateTime,val position: Position)