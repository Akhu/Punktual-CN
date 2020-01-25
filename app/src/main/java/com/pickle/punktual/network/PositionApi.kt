package com.pickle.punktual.network

import com.pickle.punktual.position.Position
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*


interface PositionService {
    @Headers("Content-Type:application/json")
    @POST("/position/register/{type}")
    suspend fun registerPosition(
        @Path("type") type: String,
        @Query("userId") userId: String,
        @Body position: Position

    ): Response<ResponseBody>
}
