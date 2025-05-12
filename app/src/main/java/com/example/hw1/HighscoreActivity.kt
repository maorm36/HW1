package com.example.hw1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.maps.model.LatLng

class HighscoreActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_highscore)
        if (savedInstanceState == null) {
            Log.d("HighscoreActivity", "Loading HighscoreListFragment and MapsFragment")
            val transaction = supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragmentContainer, HighscoreListFragment())
            transaction.replace(R.id.mapContainer, MapsFragment())
            transaction.commit()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Handle Intent from onCreate
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.let {
            val latitude = it.getDoubleExtra("SELECTED_LATITUDE", 0.0)
            val longitude = it.getDoubleExtra("SELECTED_LONGITUDE", 0.0)
            if (latitude != 0.0 || longitude != 0.0) { // Ignore default LatLng(0.0, 0.0)
                val location = LatLng(latitude, longitude)
                updateMapLocation(location)
            }
        }
    }

    fun updateMapLocation(location: LatLng) {
        Log.d("HighscoreActivity", "Received location: $location")
        val mapsFragment = supportFragmentManager.findFragmentById(R.id.mapContainer) as? MapsFragment
        if (mapsFragment != null) {
            mapsFragment.updateMap(location)
            Log.d("HighscoreActivity", "Called updateMap with location: $location")
        } else {
            Log.e("HighscoreActivity", "MapsFragment not found")
        }
    }
}