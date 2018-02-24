package me.cooper.rick.crowdcontrollerclient.activity.group

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import me.cooper.rick.crowdcontrollerclient.R

import kotlinx.android.synthetic.main.activity_location.*

class LocationActivity : AppCompatActivity(), OnMapReadyCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)
        setSupportActionBar(toolbar)

        val mapFragment: SupportMapFragment = supportFragmentManager
                .findFragmentById(R.id.fragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
        var params = mapFragment.view?.layoutParams
        params?.let {
            it.height = 900
            it.width = 900
            mapFragment.view?.layoutParams = it
        }

    }

    override fun onMapReady(map: GoogleMap?) {
        val here = LatLng(53.480618, -2.251104)
        map?.addMarker(MarkerOptions().position(here))
        map?.moveCamera(CameraUpdateFactory.newLatLng(here))
        map?.animateCamera(CameraUpdateFactory.zoomTo(17.0f))
    }
}
