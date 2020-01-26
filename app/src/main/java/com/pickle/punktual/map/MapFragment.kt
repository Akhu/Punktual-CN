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
import com.pickle.punktual.nfc.NFCScanActivity
import com.pickle.punktual.PunktualApplication
import com.pickle.punktual.R
import com.pickle.punktual.geofence.GeofenceBroadcastReceiver
import com.pickle.punktual.position.LocationData
import com.pickle.punktual.position.LocationType
import com.pickle.punktual.position.Position
import com.pickle.punktual.ViewModelFactoryRepository
import com.pickle.punktual.geofence.GeofenceManager
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

    private lateinit var locationLiveData: LocationLiveData
    private lateinit var geofenceManager: GeofenceManager
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

        locationLiveData = LocationLiveData(parentActivity.applicationContext)
        locationLiveData.observe(this, Observer { handleLocationData(it!!) })

        return binding.rootView
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getUiState().observe(this, Observer {
            updateUi(it)
        })

        geofenceManager = GeofenceManager(parentActivity.applicationContext)

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
    }

    private var userMarker: Marker? = null

    private val mapOptions by lazy {
        with(GoogleMapOptions()) {
            mapType(GoogleMap.MAP_TYPE_NORMAL)
            zoomControlsEnabled(true)
            zoomGesturesEnabled(true)
            compassEnabled(true)
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
            REQUEST_CHECK_FOR_SETTINGS -> locationLiveData.startRequestLocation()
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
            REQUEST_PERMISSION_LOCATION_START_UPDATE -> locationLiveData.startRequestLocation()
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

                geofenceManager.setupGeoFence(state.papeteriePointOfInterest)
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
