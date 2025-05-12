package com.example.hw1

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.OverlayItem
import com.google.android.gms.maps.model.LatLng
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat

class MapsFragment : Fragment() {

    private lateinit var mapView: MapView
    private lateinit var markerOverlay: ItemizedIconOverlay<OverlayItem>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Initialize osmdroid configuration
        Configuration.getInstance().load(
            requireContext(),
            requireContext().getSharedPreferences("osmdroid", 0)
        )
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView = view.findViewById(R.id.map)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        // Initialize map with default location (NY , USA)
        val defaultLocation = GeoPoint(40.6975, -73.9795)
        mapView.controller.setZoom(9.0)
        mapView.controller.setCenter(defaultLocation)

        // Initialize marker overlay with a default marker
        val items = ArrayList<OverlayItem>()
        items.add(OverlayItem("Marker in NY USA", "", defaultLocation))
        val defaultMarker = ContextCompat.getDrawable(requireContext(), android.R.drawable.star_big_on)
        markerOverlay = ItemizedIconOverlay(
            items,
            defaultMarker,
            object : ItemizedIconOverlay.OnItemGestureListener<OverlayItem> {
                override fun onItemSingleTapUp(index: Int, item: OverlayItem?): Boolean {
                    return true
                }
                override fun onItemLongPress(index: Int, item: OverlayItem?): Boolean {
                    return true
                }
            },
            requireContext()
        )
        mapView.overlays.add(markerOverlay)

        Log.d("MapsFragment", "Map initialized with default location: $defaultLocation")
    }

    fun updateMap(location: LatLng) {
        val geoPoint = GeoPoint(location.latitude, location.longitude)
        markerOverlay.removeAllItems()
        val marker = OverlayItem("Highscore Location", "", geoPoint)
        markerOverlay.addItem(marker)
        mapView.controller.setCenter(geoPoint)
        mapView.controller.setZoom(10.0) // Match previous Google Maps zoom level
        mapView.invalidate() // Refresh the map
        Log.d("MapsFragment", "Map updated to location: $geoPoint")
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume() // Required for osmdroid lifecycle
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause() // Required for osmdroid lifecycle
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDetach() // Clean up osmdroid resources
    }
}