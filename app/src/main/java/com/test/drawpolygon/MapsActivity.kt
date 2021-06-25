package com.test.drawpolygon


import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.tbruyelle.rxpermissions3.RxPermissions
import com.test.drawpolygon.databinding.ActivityMapsBinding
import java.util.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {


    // Map a marker id to its corresponding list (represented by the root marker id)
    var markerToList = HashMap<String, String>()

    // A list of markers for each polygon (designated by the marker root).
    var polygonMarkers = HashMap<String, List<Marker>>()

    // A list of polygon points for each polygon (designed by the marker root).
    var polygonPoints = HashMap<String, List<LatLng>>()

    // List of polygons (designated by marker root).
    var polygons = HashMap<String, Polygon>()

    // The active polygon (designated by marker root) - polygon added to.
    var markerListKey: String? = null

    // Flag used to record when the 'New Polygon' button is pressed.  Next map
    // click starts a new polygon.
    var newPolygon = false

    private lateinit var mMap: GoogleMap
    private var points: MutableList<LatLng> = ArrayList()
    private var markerList: MutableList<Marker> = ArrayList()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null

    private lateinit var binding: ActivityMapsBinding
    private val rxPermissions = RxPermissions(this)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getLocationPermission()
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

    }


    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val sydney = LatLng(-34.0, 151.0)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 16f))
        with(binding) {
            btnStart.setOnClickListener {
                newPolygon = true
            }
            btnUndo.setOnClickListener {
                if (points.size > 0) {
                    val marker = markerList[markerList.size - 1]
                    markerToList[marker.id] = markerListKey!!
                    marker.remove()
                    markerList.remove(marker)
                    points.removeAt(points.size - 1)
                    if (points.size > 0) {
                        drawPolygon(markerListKey, points)
                    }
                }
            }
        }

        mMap.setOnMapClickListener { latLng ->
            val marker = mMap.addMarker(MarkerOptions().position(latLng).draggable(true))
            marker.tag = latLng


            // Special case for very first marker.
            if (polygonMarkers.size == 0) {
                polygonMarkers[marker.id] = ArrayList()
                // only 0 or 1 polygons so just add it to new one or existing one.
                markerList = ArrayList()
                points = ArrayList()
                polygonMarkers[marker.id] = markerList
                polygonPoints[marker.id] = points
                markerListKey = marker.id
            }

            if (newPolygon) {
                newPolygon = false
                markerList = ArrayList()
                points = ArrayList()
                polygonMarkers[marker.id] = markerList
                polygonPoints[marker.id] = points
                markerListKey = marker.id
            }

            markerList.add(marker)
            points.add(latLng)
            markerToList[marker.id] = markerListKey!!

            drawPolygon(markerListKey, points)
        }
        mMap.setOnMarkerDragListener(object : OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {}
            override fun onMarkerDrag(marker: Marker) {
                updateMarkerLocation(marker)
            }

            override fun onMarkerDragEnd(marker: Marker) {
                updateMarkerLocation(marker)
            }
        })

        mMap.isMyLocationEnabled = true

        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location != null) {
                currentLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
            }
        }
    }


    private fun getLocationPermission() {
        rxPermissions
            .request(Manifest.permission.ACCESS_FINE_LOCATION)
            .subscribe { granted ->
                if (granted) { // Always true pre-M
                    getLastKnownLocation()
                } else {
                    // Oups permission denied
                }
            }
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    currentLocation = location
                    //moveMapToCenter(location)
                    val current = LatLng(location.latitude, location.longitude)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 16f))
                }

            }

    }


    private fun updateMarkerLocation(marker: Marker) {

        // Use the marker to figure out which polygon list to use...
        val pts = polygonPoints.get(markerToList.get(marker.id))?.toMutableList()

        // This is much the same except use the retrieved point list.
        val latLng = marker.tag as LatLng
        val position = pts!!.indexOf(latLng)
        pts[position] = marker.position
        marker.tag = marker.position
        drawPolygon(markerToList[marker.id], pts)
    }


    private fun drawPolygon(mKey: String?, latLngList: List<LatLng>?) {
        // Use the existing polygon (if any) for the root marker.
        var polygon = polygons[mKey]
        polygon?.remove()

        val polygonOptions = PolygonOptions()
        polygonOptions.fillColor(R.color.design_default_color_secondary)
        polygonOptions.strokeColor(R.color.design_default_color_primary)
        polygonOptions.strokeWidth(2f)
        polygonOptions.addAll(latLngList)
        polygon = mMap.addPolygon(polygonOptions)

        // And update the list for the root marker.
        polygons[mKey!!] = polygon
    }


}