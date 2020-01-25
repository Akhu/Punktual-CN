package com.pickle.punktual.map


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.pickle.nfcboilerplateapp.NFCData
import com.pickle.nfcboilerplateapp.NFCScanActivity
import com.pickle.punktual.PunktualApplication
import com.pickle.punktual.R
import com.pickle.punktual.geofence.GeofenceBroadcastReceiver
import com.pickle.punktual.position.LocationData
import com.pickle.punktual.position.LocationType
import com.pickle.punktual.position.Position
import com.pickle.punktual.ViewModelFactoryRepository
import kotlinx.android.synthetic.main.fragment_map.*
import timber.log.Timber


private const val REQUEST_PERMISSION_LOCATION_START_UPDATE = 101
private const val REQUEST_CHECK_FOR_SETTINGS = 200
private const val REQUEST_NFC = 1001

private const val MAP_DEFAULT_ZOOM = 8f

private const val GEOFENCE_RADIUS = 100F
/**
 * A simple [Fragment] subclass.
 */
class MapFragment : Fragment() {

    private var firstLoading: Boolean = true
    private lateinit var map: GoogleMap
    private lateinit var parentActivity : AppCompatActivity

    private lateinit var binding : View

    val viewModel : MapViewModel by viewModels {
        ViewModelFactoryRepository(PunktualApplication.repo)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = inflater.inflate(R.layout.fragment_map, container, false)

        //I want my app crash if I don't have my Activity here.
        parentActivity = (this.context as AppCompatActivity)

        return binding.rootView
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getUiState().observe(this, Observer {
            updateUi(it)
        })

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(parentActivity)

        geofencingClient = LocationServices.getGeofencingClient(parentActivity)

        val mapFragment = SupportMapFragment.newInstance(mapOptions)
        mapFragment.getMapAsync {
            map = it
            loadingProgressBar.hide()
        }

        (activity)?.supportFragmentManager?.also {
            it
                .beginTransaction()
                .replace(R.id.mapFrame, mapFragment)
                .commit()
        }

        buttonCheckin.setOnClickListener {
            val intent = Intent(parentActivity, NFCScanActivity::class.java)
            intent.putExtra(NFCScanActivity.EXTRA_NFC_MODE, NFCScanActivity.NFC_MODE_READ)
            startActivityForResult(intent, REQUEST_NFC)
        }

        requestLastLocation()
        requestLocation()
    }



    private lateinit var geofencingClient: GeofencingClient

    private val geoFencingPendingIntent : PendingIntent by lazy {
        val intent = Intent(this.context, GeofenceBroadcastReceiver::class.java)
        viewModel.getCurrentUser().value?.let {
            intent.putExtra(PunktualApplication.PENDING_INTENT_EXTRA_USER_ID_KEY, it.id.toString())
        }

        PendingIntent.getBroadcast(this.context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
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
            REQUEST_NFC -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.getParcelableExtra<NFCData>("data")?.let {
                        Timber.d("Received data from NFC Tag :${it.data}")
                        val locationType = LocationType.valueOf(it.data.toUpperCase())
                        if (locationType == LocationType.CAMPUS_NUMERIQUE) {
                            Toast.makeText(
                                parentActivity,
                                "Tag recognized ! Checked In at ${it.data}",
                                Toast.LENGTH_SHORT
                            ).show()
                            viewModel.checkInNcf(locationType)
                        } else {
                            Toast.makeText(
                                parentActivity,
                                "Tag not recognized ${it.data}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            REQUEST_CHECK_FOR_SETTINGS -> startRequestLocation()
        }
    }

    /**
     * Starting to send request location
     */
    private fun startRequestLocation() {
        val task = LocationServices
            .getSettingsClient(parentActivity)
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
            is ResolvableApiException -> exception.startResolutionForResult(parentActivity,
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
                pseudoTextView.text = "${state.user.username} you are connected :)"
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
            if(ContextCompat.checkSelfPermission(parentActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(parentActivity, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat
                    .requestPermissions(
                        parentActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                        requestCode)
                return false
            }
            return true
        }
        else {
            if (ContextCompat.checkSelfPermission(
                    this.context!!,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat
                    .requestPermissions(
                        parentActivity,
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
