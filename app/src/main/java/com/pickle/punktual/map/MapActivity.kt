package com.pickle.punktual.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.pickle.punktual.PunktualApplication
import com.pickle.punktual.R
import com.pickle.punktual.geofence.GeofenceBroadcastReceiver
import com.pickle.punktual.position.LocationData
import com.pickle.punktual.position.Position
import com.pickle.punktual.viewModels.ViewModelFactoryRepository
import kotlinx.android.synthetic.main.activity_map.*
import timber.log.Timber

private const val REQUEST_PERMISSION_LOCATION_START_UPDATE = 101
private const val REQUEST_CHECK_FOR_SETTINGS = 200

private const val MAP_DEFAULT_ZOOM = 8f

private const val GEOFENCE_RADIUS = 100F


class MapActivity : AppCompatActivity() {

    private var firstLoading: Boolean = true
    private lateinit var map: GoogleMap

    private val viewModel by viewModels<MapViewModel> {
        ViewModelFactoryRepository(PunktualApplication.repo)
    }

    private lateinit var geofencingClient: GeofencingClient

    private val geoFencingPendingIntent : PendingIntent by lazy {
        val intent = Intent(this, GeofenceBroadcastReceiver::class.java)
        viewModel.getCurrentUser().value?.let {
            intent.putExtra(PunktualApplication.PENDING_INTENT_EXTRA_USER_ID_KEY, it.id.toString())
        }

        PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private var userMarker: Marker? = null

    private val locationRequest: LocationRequest = LocationRequest.create().apply {
        interval = 10000
        fastestInterval = 5000
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }
    private lateinit var fusedLocationClient : FusedLocationProviderClient

    private val mapOptions by lazy {
        with(GoogleMapOptions()) {
            mapType(GoogleMap.MAP_TYPE_NORMAL)
            zoomControlsEnabled(true)
            zoomGesturesEnabled(true)
            compassEnabled(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        viewModel.getUiState().observe(this, Observer {
            updateUi(it)
        })

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        geofencingClient = LocationServices.getGeofencingClient(this)

        val mapFragment = SupportMapFragment.newInstance(mapOptions)
        mapFragment.getMapAsync {
            map = it
            loadingProgressBar.hide()
        }

        supportFragmentManager
            .beginTransaction().replace(R.id.mapFrame, mapFragment)
            .commit()

        requestLastLocation()
        requestLocation()
        //Get Location (Big Part) - DONE
        //Map fragment -
        //Display POIs
        //GeoFence
    }

    private fun setupGeoFence(poiFence: Position) {
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

        geofencingClient.addGeofences(request, geoFencingPendingIntent)
            .addOnFailureListener {
                Timber.e(it, "Could not set up geofence !")
            }
            .addOnSuccessListener {
                Timber.d("Geofence set up successful")
            }
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

    /**
     *
     */
    private fun handleLocationData(locationData: LocationData){
        locationData.exception?.let {
            handleLocationException(it)
            return
        }

        locationData.location?.let { location ->
            val latLng = LatLng(location.latitude, location.longitude)

            userMarker?.let {
                it.position = latLng
            } ?: run {
                if(::map.isInitialized){
                    if(firstLoading) {
                        map.moveCamera(
                            CameraUpdateFactory
                                .newLatLngZoom(latLng,
                                    MAP_DEFAULT_ZOOM
                                )
                        )
                        firstLoading = false
                    }

                    Timber.d("Update current user with gathered location $location")
                    viewModel.loadMapData(location)
                }
            }
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
            is ResolvableApiException -> exception.startResolutionForResult(this,
                REQUEST_CHECK_FOR_SETTINGS
            )
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
    private fun updateUi(state: MapUiState) {
        when(state) {
            is MapUiState.Loading -> {

            }
            is MapUiState.Ready -> {
                pseudoTextView.text = "${state.user.username} is connected with id: ${state.user.id}"
                state.user.lastPosition?.let { positionObject ->
                    userMarker = map.addUserMarkerWithPosition(state.user, positionObject)
                    Timber.d("Update current user with gathered location ${state.user.lastPosition}")
                }

                setupGeoFence(state.papeteriePointOfInterest)
            }
            is MapUiState.Error -> {
                pseudoTextView.text = state.errorMessage
            }
        }
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
