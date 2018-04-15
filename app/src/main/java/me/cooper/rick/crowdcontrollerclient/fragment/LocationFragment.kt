package me.cooper.rick.crowdcontrollerclient.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.fragment_location.*
import kotlinx.android.synthetic.main.fragment_location.view.*
import me.cooper.rick.crowdcontrollerapi.dto.group.GroupDto
import me.cooper.rick.crowdcontrollerapi.dto.group.LocationDto
import me.cooper.rick.crowdcontrollerclient.R
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.destination
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.group
import me.cooper.rick.crowdcontrollerclient.api.service.ApiService.lastLocation

class LocationFragment : AbstractAppFragment(), OnMapReadyCallback {

    private var listener: OnFragmentInteractionListener? = null

    private var googleMap: GoogleMap? = null

    private lateinit var root: View

    private var tempMarker: Marker? = null

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

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        listener = (context as? OnFragmentInteractionListener)
                ?: throw RuntimeException("${context!!} must implement OnFragmentInteractionListener")
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onMapReady(map: GoogleMap?) {
        googleMap = map
        updateView(group)
        group?.location?.let { zoomToLocation(LatLng(it.latitude!!, it.longitude!!)) }
    }

    private fun zoomToLocation(target: LatLng) {
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 17.0f))
    }

    override fun getTitle(): String = TITLE

    fun updateView(groupDto: GroupDto?) {
        groupDto?.location?.let { locationDto ->
            if (locationDto.hasLocation()) {
                locationDto.address?.let { txt_address.text = parseAddress(it) }
                googleMap?.let { updateMap(locationDto, groupDto.settings?.clustering ?: false) }
            }
        }
    }

    private fun parseAddress(address: String): String {
        return address.replace(", ", ",\n")
    }

    private fun updateMap(locationDto: LocationDto, isClustered: Boolean) {
        destination = LatLng(locationDto.latitude!!, locationDto.longitude!!)
        if (lastLocation != null) drawStartMarker(lastLocation!!)
        tempMarker?.remove()
        val bitmapDescriptor = defaultMarker(if (!isClustered) HUE_RED else HUE_GREEN)
        tempMarker = googleMap?.addMarker(
                MarkerOptions().position(destination!!)
                        .icon(bitmapDescriptor)
        )
        val mapParams = root.map.layoutParams
        mapParams.height = root.map.measuredWidth
        root.map.layoutParams = mapParams
        root.map.invalidate()
    }

    private fun drawStartMarker(lastLocation: LatLng) {
        googleMap?.addMarker(
                MarkerOptions().position(lastLocation)
                        .icon(defaultMarker(HUE_YELLOW))
        )
    }

    interface OnFragmentInteractionListener

    companion object {
        private const val TITLE = "Location"
    }

}
