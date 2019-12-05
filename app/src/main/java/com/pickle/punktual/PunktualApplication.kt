package com.pickle.punktual

import android.app.Application
import timber.log.Timber

class PunktualApplication : Application(){

    override fun onCreate() {
        super.onCreate()

        Timber.plant(Timber.DebugTree())
    }
}