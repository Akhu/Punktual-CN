package com.pickle.punktual.user

import com.pickle.punktual.position.Position
import com.pickle.punktual.position.PositionHistory
import org.joda.time.DateTime
import java.util.*
import kotlin.collections.ArrayList

enum class UserType {
    STUDENT, TEACHER
}

data class User(val id: UUID = UUID.randomUUID(), val type: UserType = UserType.STUDENT, val username: String, val pushToken: String? = null, val imageUrl: String? = null) {
    //Joda time used : https://www.joda.org/joda-time/quickstart.html
    //Each user will get a position list, but we remember only a few of it's declared positions
    private var positionList: ArrayList<PositionHistory> = ArrayList()

    val positionHistory : List<PositionHistory>
        get() = positionList

    fun addPosition(position: Position){
        if(positionList.size >= 5){
            positionList = positionList.dropLast(1) as ArrayList<PositionHistory>
        }
        positionList.add(PositionHistory(DateTime.now(), position))
    }
}

data class UserLogin(val id: UUID, val username: String)