package com.pickle.punktual.geofence

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.pickle.punktual.PunktualApplication
import com.pickle.punktual.position.Position
import timber.log.Timber

private const val GEOFENCE_RADIUS = 100F

/**
 * Based on the official documentation
 * https://developer.android.com/training/location/geofencing
 */
class GeofenceManager(context: Context) {
    /**
     * We often need the context, and only one context, ideally the app context to be activity independent
     */
    private val appContext = context.applicationContext

    /**
     * We let the Google service for location instanciate the Geofence Client
     */
    private val geofencingClient = LocationServices.getGeofencingClient(appContext)

    /**
     * We need to handle multiple geofence, so we create a list.
     */
    private val geofenceList = mutableListOf<Geofence>()

    fun setupGeoFence(poiFence: Position) {
        //We monitor only around papeterie, and only when device is Entering the zone
        val papetGeofence = with(Geofence.Builder()) {
            setRequestId(PunktualApplication.GEOFENCE_PAPETERIE_ID)
            setCircularRegion(
                poiFence.latitude, poiFence.longitude,
                GEOFENCE_RADIUS
            )
            setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            setExpirationDuration(Geofence.NEVER_EXPIRE)
            build()
        }

        val request = with(GeofencingRequest.Builder()) {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofence(papetGeofence)
            build()
        }


        geofencingClient.addGeofences(request, geofencePendingIntent)
            .addOnFailureListener {
                Timber.e(it, "Could not set up geofence !")
            }
            .addOnSuccessListener {
                Timber.d("Geofence set up successful")
            }
    }

    fun removeAllGeofences() {
        geofencingClient.removeGeofences(geofencePendingIntent)
        geofenceList.clear()
    }

    private fun getGeofencingRequest() : GeofencingRequest {
        return with(GeofencingRequest.Builder()) {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofenceList)
            build()
        }
    }

    /**
     * By lazy means that the lazy block will be called only once, and only when the var is called somewhere
     * It's like a one time computed variable
     * -----
     * Purpose:
     * Here we create a reccuring Intent that will be called each time the geofences are triggered.
     */
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(appContext, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            appContext, 0,
            intent,
            // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
            // addGeofences() and removeGeofences().
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}