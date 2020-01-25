package com.pickle.punktual.position

import android.location.Location
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import org.joda.time.DateTime
import java.lang.Exception


enum class LocationType {
    PAPETERIE,
    CAMPUS_NUMERIQUE,
    UNKNOWN
}

@JsonClass(generateAdapter = true)
data class LocationData(val location: Location? = null, val exception: Exception? = null)

@JsonClass(generateAdapter = true)
data class Position(val latitude: Double, val longitude: Double)

@JsonClass(generateAdapter = true)
data class PositionHistory(val date: DateTime,val position: Position)