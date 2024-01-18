package com.example.maptask

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.maptask.databinding.ActivityMapsBinding
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import java.io.IOException
import java.util.Locale

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var autoCompleteFragment: AutocompleteSupportFragment
    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var googleMap: GoogleMap
    private var selectedMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapFragment = SupportMapFragment()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val currentLocation = LatLng(it.latitude, it.longitude)
                        showCurrentLocation(currentLocation)
                    }
                }
        }

        Places.initialize(applicationContext, getString(R.string.api_key))
        autoCompleteFragment =
            supportFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment
        autoCompleteFragment.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
            )
        )
        autoCompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onError(p0: Status) {
                Toast.makeText(this@MapsActivity, "$p0", Toast.LENGTH_SHORT).show()
            }

            override fun onPlaceSelected(p0: Place) {
                val latLng = p0.latLng
                zoom(latLng!!)
            }

        })

        val mapFragment =
            supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun zoom(lat: LatLng) {
        val newLat = CameraUpdateFactory.newLatLngZoom(lat, 12f)
        mMap.animateCamera(newLat)
    }

    private fun showCurrentLocation(currentLocation: LatLng) {
        selectedMarker = googleMap.addMarker(
            MarkerOptions().position(currentLocation)
                .title("Selected location or current location")
        )
        googleMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                currentLocation,
                15f
            )
        )
    }

    override fun onMapReady(gMap: GoogleMap) {
        googleMap = gMap

        googleMap.setOnMapClickListener { latLng ->
            moveMarker(latLng)
        }
        googleMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {
            }

            override fun onMarkerDrag(marker: Marker) {
            }

            override fun onMarkerDragEnd(marker: Marker) {
                moveMarker(marker.position)
            }
        })

    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location: Location? ->
                        location?.let {
                            val currentLocation = LatLng(it.latitude, it.longitude)
                            showCurrentLocation(currentLocation)
                        }
                    }
            } else {
                Toast.makeText(this@MapsActivity, "Permission is denied", Toast.LENGTH_SHORT).show()
                finishAffinity()
            }
        }
    }

    private fun moveMarker(latLng: LatLng) {
        selectedMarker?.let {
            it.position = latLng
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng))
            getAddress(latLng)
        }
    }

    private fun getAddress(latLng: LatLng) {
        val geocoder = Geocoder(this, Locale.getDefault())

        try {
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (addresses!!.isNotEmpty()) {
                val address = addresses[0]
                val addressString = address!!.getAddressLine(0) // Get the first address line
                Toast.makeText(this@MapsActivity, "Address : $addressString", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

}