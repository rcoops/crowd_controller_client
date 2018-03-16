package me.cooper.rick.crowdcontrollerclient.fragment

import android.content.Context
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.fragment_location.*
import kotlinx.android.synthetic.main.fragment_location.view.*
import me.cooper.rick.crowdcontrollerapi.dto.group.LocationDto
import me.cooper.rick.crowdcontrollerclient.R

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

        return view
    }

    override fun onResume() {
        val mapParams = root.map.layoutParams
        mapParams.height = mapParams.width
        root.map.layoutParams = mapParams
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
        listener = if (context is OnFragmentInteractionListener) {
            context
        } else {
            throw RuntimeException("${context!!} must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onMapReady(map: GoogleMap?) {
        googleMap = map
    }

    override fun getTitle(): String = TITLE

    fun updateView(locationDto: LocationDto?) {
        locationDto?.let {
            if (locationDto.hasLocation()) {
                it.latitude?.let { txt_latitude?.text = "$it" }
                it.longitude?.let { txt_longitude?.text = "$it" }
                googleMap?.let { updateMap(locationDto) }
            }
        }
    }

    private fun updateMap(locationDto: LocationDto) {
        val here = LatLng(locationDto.latitude!!, locationDto.longitude!!)
        tempMarker?.remove()
        tempMarker = googleMap?.addMarker(MarkerOptions().position(here))
        googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(here, 17.0f))
    }

    interface OnFragmentInteractionListener

    companion object {
        private const val TITLE = "Location"
    }

}
