package com.pickle.punktual.network

import com.pickle.punktual.position.Position
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.http2.Header
import timber.log.Timber
import java.io.IOException
import java.util.*

class NetworkService {

    companion object {
        val baseUrl = with(HttpUrl.Builder()) {
            scheme("http")
            host("10.0.2.2")
            port(8080)
        }
        val mediaTypeJson = "application/json; charset=utf-8".toMediaType()
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val client : OkHttpClient by lazy { OkHttpClient() }

        val jsonHeader =  Header("Content-Type", "application/json")
    }

    fun sendRequest(request: Request, callback : (Unit) -> (Callback)) {

            client.newCall(request).enqueue(responseCallback = object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Timber.e("Exception when calling network ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    Timber.d("Server response : ${response.body}")
                }

            })
        }
}