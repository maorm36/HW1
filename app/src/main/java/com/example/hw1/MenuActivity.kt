package com.example.hw1

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MenuActivity : AppCompatActivity() {
    // TODO fix the loop that was created from repeatedly denial of permission gps request
    private val requestLocationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            Log.d("MenuActivity", "Location permission granted")
            denialCount = 0 // Reset counter on grant
        } else {
            Log.w("MenuActivity", "Location permission denied")
            denialCount++
            if (denialCount >= MAX_DENIAL_ATTEMPTS) {
                Log.w("MenuActivity", "Maximum denial attempts reached, exiting")
                showMaxAttemptsDialog()
            } else {
                showDenialDialog()
            }
        }
    }

    private var denialCount = 0
    private val MAX_DENIAL_ATTEMPTS = 3
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)

        // Always request permission on launch
        if (shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            Log.d("MenuActivity", "Showing permission rationale")
            showRationaleDialog()
        } else {
            Log.d("MenuActivity", "Requesting location permission")
            requestLocationPermission.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

        findViewById<View>(R.id.buttonRegularGame).setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }
        findViewById<View>(R.id.buttonGameSensor).setOnClickListener {
            startActivity(Intent(this, GameSensorActivity::class.java))
        }
        findViewById<View>(R.id.buttonHighscore).setOnClickListener {
            startActivity(Intent(this, HighscoreActivity::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun showRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Location Permission Required")
            .setMessage("This app needs location access to save GPS-based highscores. Please grant permission to continue.")
            .setPositiveButton("Grant") { _, _ ->
                Log.d("MenuActivity", "User chose to grant permission from rationale")
                // Add delay to prevent rapid cycling
                handler.postDelayed({
                    requestLocationPermission.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                }, 500)
            }
            .setNegativeButton("Exit") { _, _ ->
                Log.w("MenuActivity", "User chose to exit from rationale")
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showDenialDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Denied")
            .setMessage("Location permission is required to play the game. Would you like to try again? (Attempt ${denialCount + 1}/$MAX_DENIAL_ATTEMPTS)")
            .setPositiveButton("Retry") { _, _ ->
                Log.d("MenuActivity", "User chose to retry permission")
                // Add delay to prevent rapid cycling
                handler.postDelayed({
                    requestLocationPermission.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                }, 500)
            }
            .setNegativeButton("Exit") { _, _ ->
                Log.w("MenuActivity", "User chose to exit after denial")
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showMaxAttemptsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("You have denied location permission multiple times. This permission is required to play the game. The app will now exit.")
            .setPositiveButton("OK") { _, _ ->
                Log.w("MenuActivity", "Exiting after max denial attempts")
                finish()
            }
            .setCancelable(false)
            .show()
    }
}