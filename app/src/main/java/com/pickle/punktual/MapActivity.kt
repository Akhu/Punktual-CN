package com.pickle.punktual

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.pickle.punktual.position.LocationData
import com.pickle.punktual.user.User
import kotlinx.android.synthetic.main.activity_map.*
import timber.log.Timber

private const val REQUEST_PERMISSION_LOCATION_START_UPDATE = 101
private const val REQUEST_CHECK_FOR_SETTINGS = 200

private const val MAP_DEFAULT_ZOOM = 8f

class MapActivity : AppCompatActivity() {

    private val locationRequest: LocationRequest = LocationRequest.create().apply {
        interval = 10000
        fastestInterval = 5000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }
    private lateinit var fusedLocationClient : FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        PunktualApplication.repo.getCurrentUser().observe(this, Observer { updateUi(it) })


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        requestLastLocation()
        requestLocation()
        //Get Location (Big Part)
        //Map fragment
        //Display POIs
    }

    /**
     * Override of Activity function, will be useful when you start an activity to expect a result from it.
     * You must attribute a request code when call the external activity. This activity will respond with the provided request code,
     * so you can act following the request code received.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CHECK_FOR_SETTINGS -> startRequestLocation()
        }
    }

    /**
     * Starting to send request location
     */
    private fun startRequestLocation() {
        val task = LocationServices
            .getSettingsClient(this)
            .checkLocationSettings(
                LocationSettingsRequest.Builder().apply {
                    addLocationRequest(locationRequest)
                }.build()
            )

        task.addOnSuccessListener { locationSettingsResponse ->
            Timber.i("Location settings satisfied. Init location request here")
            requestLocation()
        }

        task.addOnFailureListener {
            Timber.e("Failed to modify location settings")
            handleLocationData(LocationData(exception = it))
        }

    }

    private fun handleLocationData(locationData: LocationData){
        locationData.exception?.let {
            handleLocationException(it)
            return
        }
    }

    /**
     * Location exception
     */
    private fun handleLocationException(exception: Exception?): Boolean {
        exception ?: return false
        Timber.e(exception, "Handling Location Exception ...")

        when (exception) {
            is SecurityException -> checkLocationPermission(REQUEST_PERMISSION_LOCATION_START_UPDATE)
            is ResolvableApiException -> exception.startResolutionForResult(this, REQUEST_CHECK_FOR_SETTINGS)
        }

        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED){
            return
        }
        when(requestCode) {
            REQUEST_PERMISSION_LOCATION_START_UPDATE -> requestLastLocation()
        }
    }



    /**
     * Gather last location
     * Not meaning it will request a new location
     */
    private fun requestLocation() {
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {
                    locationResult ?: return
                    locationResult.locations.forEach { location ->
                        handleLocationData(locationData = LocationData(location))
                    }
                }
            }, Looper.getMainLooper())
        } catch (exception: SecurityException){
            handleLocationData(LocationData(exception = exception))
        }
    }

    private fun requestLastLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener {
                Timber.i("Received location $it")
                handleLocationData(locationData = LocationData(it))
            }

            fusedLocationClient.lastLocation.addOnFailureListener {
                handleLocationData(locationData = LocationData(exception = it))
                Timber.d("Received Exception From Location Service $it")
            }
        } catch (e: SecurityException){
            handleLocationData(LocationData(exception = e))
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUi(user: User) {
        pseudoTextView.text = "${user.username} is connected with id: ${user.id}"
    }

    private fun checkLocationPermission(requestCode: Int) : Boolean {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat
                    .requestPermissions(
                        this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                        requestCode)
                return false
            }
            return true
        }
        else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat
                    .requestPermissions(
                        this,
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ),
                        requestCode
                    )
                return false
            }
            return true
        }

    }
}
