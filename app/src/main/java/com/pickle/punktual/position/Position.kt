package com.pickle.punktual.position

import org.joda.time.DateTime

data class Position(val latitude: Double, val longitude: Double)

data class PositionHistory(val date: DateTime,val position: Position)