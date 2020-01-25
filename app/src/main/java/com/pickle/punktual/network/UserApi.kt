package com.pickle.punktual.network

import com.pickle.punktual.user.User
import com.pickle.punktual.user.UserLogin
import com.pickle.punktual.user.UserRegister
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface UserService {

    @Headers("Content-Type:application/json")
    @POST("/login")
    suspend fun loginUser(@Body userLogin: UserLogin): Response<User>


    @Headers("Content-Type:application/json")
    @POST("/register")
    suspend fun registerUser(@Body user: UserRegister): Response<User>
}
