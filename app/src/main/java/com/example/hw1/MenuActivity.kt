package com.example.hw1

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MenuActivity : AppCompatActivity() {

    private val GPS_PERMISSION_REQUEST_CODE = 100
    private val MAX_DENIAL_ATTEMPTS = 3
    private var denialCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)

        // Reset denial count on fresh launch
        denialCount = 0

        // Set up button listeners
        listOf(
            R.id.buttonRegularGame to GameActivity::class.java,
            R.id.buttonGameSensor to GameSensorActivity::class.java,
            R.id.buttonHighscore to HighscoreActivity::class.java
        ).forEach { (buttonId, activityClass) ->
            findViewById<View>(buttonId).setOnClickListener {
                if (hasGpsPermission()) {
                    startActivity(Intent(this, activityClass))
                } else {
                    requestGpsPermission()
                }
            }
        }

        // Handle window insets for edge-to-edge experience
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun hasGpsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestGpsPermission() {
        if (!hasGpsPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                GPS_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == GPS_PERMISSION_REQUEST_CODE) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "GPS Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                denialCount++
                if (denialCount >= MAX_DENIAL_ATTEMPTS) {
                    Toast.makeText(this, "Permission denied. Exiting...", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "GPS Permission Required", Toast.LENGTH_SHORT).show()
                    // Retry after a short delay
                    window.decorView.postDelayed({ requestGpsPermission() }, 1000)
                }
            }
        }
    }
}
