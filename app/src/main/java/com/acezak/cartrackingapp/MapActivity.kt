package com.acezak.cartrackingapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.provider.ContactsContract.CommonDataKinds.Email
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    //Init map config
    private var map: GoogleMap? = null
    private var cameraPosition: CameraPosition? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    //Default loc
    private val defaultLocation = LatLng(0.0, 0.0)

    //Init permission as denied
    private var locationPermissionGranted = false

    //Last device loc variable
    private var lastKnownLocation: Location? = null

    //Send data to register flag
    var sendFlag = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Hide action bar in view
        supportActionBar?.hide()

        // set Loc and camera
        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION)
        }

        // Show content
        setContentView(R.layout.activity_map)

        //Capture prev vals
        val bundle = intent.extras
        val plate = bundle?.getString("plate")

        //Release vehicle
        val freeStatusButton = findViewById<Button>(R.id.freeStatusButton)
        freeStatusButton.setOnClickListener{
            //Set flag as False
            sendFlag = false
            //call release function with plate
            setFree(plate!!)
        }
        // get device location
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Graph element
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }

    //Save state
    override fun onSaveInstanceState(outState: Bundle) {
        map?.let { map ->
            outState.putParcelable(KEY_CAMERA_POSITION, map.cameraPosition)
            outState.putParcelable(KEY_LOCATION, lastKnownLocation)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onMapReady(map: GoogleMap) {
        this.map = map

        //try permissions for app use
        getLocationPermission()

        // Location access request
        updateLocationUI()

        //Delay vals
        val handler = Handler()
        val delay = 20000L

        //captute prev. vals
        val bundle = intent.extras
        val id = bundle?.getString("id")
        val plate = bundle?.getString("plate")

        //use device location function
        getDeviceLocation(plate!!, id!!)

        //Delayed call
        val runnable = object : java.lang.Runnable {
            override fun run() {
                getDeviceLocation(plate!!, id!!)
                handler.postDelayed(this, delay)
            }
        }
            handler.postDelayed(runnable, delay)
    }

    //Save register on firebase function
    @SuppressLint("MissingPermission")
    private fun getDeviceLocation(plate:String, id: String) {

        //Firebase database
        val db = Firebase.firestore
        //Firebase collection
        val vehiclesData = db.collection("vehicles").document(plate!!)

        //Permission verification to proceed
        try {
            if (locationPermissionGranted) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            //Set on view
                            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                LatLng(lastKnownLocation!!.latitude,
                                    lastKnownLocation!!.longitude), DEFAULT_ZOOM.toFloat()))
                        }

                        //If view is active to send data
                        if (sendFlag == true) {
                            val onDutyData = hashMapOf(
                                "driverId" to id,
                                "location" to arrayListOf(
                                    lastKnownLocation!!.latitude,
                                    lastKnownLocation!!.longitude
                                ),
                                "status" to "onDuty"
                            )
                            //update register values
                            vehiclesData.update(onDutyData as Map<String, Any>)
                        }else{
                            //If the send action is end
                            val onDutyData = hashMapOf(
                                "driverId" to "0",
                                "status" to "free"
                            )
                            //update register values
                            vehiclesData.update(onDutyData as Map<String, Any>)
                        }

                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, "Exception: %s", task.exception)
                        map?.moveCamera(CameraUpdateFactory
                            .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat()))
                        map?.uiSettings?.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    //Release vehicle function
    private fun setFree(plate: String){
        sendFlag = false
        //Go back
        finish()
    }

    //Permissions
    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.applicationContext,
                //Fine location permission
                Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        locationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                // If request is cancelled, the result arrays are empty
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true
                }
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
        updateLocationUI()
    }

    @SuppressLint("MissingPermission")
    private fun updateLocationUI() {
        if (map == null) {
            return
        }
        try {
            //Actions with permission
            if (locationPermissionGranted) {
                map?.isMyLocationEnabled = true
                map?.uiSettings?.isMyLocationButtonEnabled = true
            } else {
                map?.isMyLocationEnabled = false
                map?.uiSettings?.isMyLocationButtonEnabled = false
                lastKnownLocation = null
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    companion object {
        private val TAG = MapActivity::class.java.simpleName
        private const val DEFAULT_ZOOM = 15
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

        // Activity state
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_LOCATION = "location"
        private const val M_MAX_ENTRIES = 5
    }
}

    