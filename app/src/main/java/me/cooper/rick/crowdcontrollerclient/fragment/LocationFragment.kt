package me.cooper.rick.crowdcontrollerclient.fragment

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory.*
import kotlinx.android.synthetic.main.fragment_location.*
import kotlinx.android.synthetic.main.fragment_location.view.*
import me.cooper.rick.crowdcontrollerapi.dto.group.GroupDto
import me.cooper.rick.crowdcontrollerapi.dto.group.LocationDto
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.destination
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.geofence
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.geofenceCentre
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.geofenceLimit
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.group
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.lastLocation


class LocationFragment : AbstractAppFragment(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null

    private lateinit var root: View

    private var destinationMarker: Marker? = null
    private var locationMarker: Marker? = null
    private var geoFenceLimits: Circle? = null

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_location, container, false)
        root = view
        view.map.onCreate(savedInstanceState)//.getMapAsync(this)
        (view.map as MapView).getMapAsync(this)

        try {
            MapsInitializer.initialize(this.activity)
        } catch (e: GooglePlayServicesNotAvailableException) {
            e.printStackTrace()
        }

        view.btn_zoom_to_target.setOnClickListener { btnZoom(destination) }
        view.btn_zoom_to_me.setOnClickListener { btnZoom(lastLocation) }

        return view
    }

    private fun btnZoom(target: LatLng?) {
        fragmentListener?.playClick()
        target?.let { zoomToLocation(it) }
    }

    override fun onResume() {
        super.onResume()
        root.map.onResume()
    }

    override fun onDestroy() {
        root.map.onDestroy()
        super.onDestroy()
    }

    override fun onLowMemory() {
        root.map.onLowMemory()
        super.onLowMemory()
    }

    override fun onMapReady(map: GoogleMap?) {
        googleMap = map
        updateView(group)
        group?.location?.let {
            zoomToLocation(LatLng(it.latitude!!, it.longitude!!))
        }
    }

    private fun zoomToLocation(target: LatLng) {
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 17.0f))
    }

    override fun getTitle(): String = getString(R.string.action_location_view)

    fun updateView(groupDto: GroupDto?) {
        groupDto?.location?.let { locationDto ->
            if (locationDto.hasLocation()) {
                locationDto.address?.let { txt_address.text = parseAddress(it) }
                googleMap?.let { updateMap(locationDto) }
            }
        }
    }

    private fun parseAddress(address: String): String {
        return address.replace(", ", ",\n")
    }

    private fun updateMap(locationDto: LocationDto) {
        googleMap?.let { drawGeofence(it) }
        destination = LatLng(locationDto.latitude!!, locationDto.longitude!!)
        destinationMarker?.remove()
        destinationMarker = googleMap?.addMarker(MarkerOptions().position(destination!!))
        val mapParams = root.map.layoutParams
        mapParams.height = root.map.measuredWidth
        root.map.layoutParams = mapParams
        root.map.invalidate()
    }

    fun drawLocationMarker(lastLocation: LatLng) {
        locationMarker?.remove()
        locationMarker = googleMap?.addMarker(
                MarkerOptions().position(lastLocation)
                        .icon(defaultMarker(HUE_YELLOW))
        )
    }
    private fun drawGeofence(map: GoogleMap?) {
        geofence?.let {
            Log.d(TAG, "drawGeofence()")

            if (geoFenceLimits != null)
                geoFenceLimits!!.remove()

            val circleOptions = CircleOptions()
                    .center(geofenceCentre)
                    .strokeColor(Color.argb(50, 70, 70, 70))
                    .fillColor(Color.argb(100, 150, 150, 150))
                    .radius(geofenceLimit!!)
            geoFenceLimits = map?.addCircle(circleOptions)
        }
    }

    companion object {
        private const val TAG = "Location"
    }

}
