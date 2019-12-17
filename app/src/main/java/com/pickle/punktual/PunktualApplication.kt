package com.pickle.punktual

import android.app.Application
import com.pickle.punktual.user.UserRepository
import timber.log.Timber

class PunktualApplication : Application(){

    companion object {
        const val GEOFENCE_PAPETERIE_ID = "papeterie"
        val repo = UserRepository()
    }

    override fun onCreate() {
        super.onCreate()



        Timber.plant(Timber.DebugTree())
    }
}