package com.example.hw1

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlin.random.Random

class GameSensorActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var car: ImageView
    private lateinit var obstacles: List<ImageView>
    private lateinit var coins: List<ImageView>
    private lateinit var hearts: List<ImageView>
    private lateinit var scoreText: TextView
    private lateinit var distanceText: TextView
    private lateinit var obstacleLayout: LinearLayout
    private lateinit var coinLayout: LinearLayout
    private var filteredX: Float? = null
    private var tiltReset = true  // To ensure the device returns to neutral before another move
    private var lastTiltTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val isObstacleFalling = MutableList(5) { false }
    private val isCoinFalling = MutableList(5) { false }
    private var carPosition = 2 // 0 = leftmost, 4 = rightmost, 5 lanes
    private var lives = 3
    private var score = 0
    private var distance = 0
    private var initialY = 0f
    private var obstacleLayoutTop = 0 // Store the top position of the layout in screen coordinates
    private var obstacleLayoutBottom =
        0 // Store the bottom position of the layout in screen coordinates
    private var coinLayoutBottom =
        0 // Store the bottom position of the coin layout in screen coordinates
    private var gameRunnable: Runnable? = null // Store the game loop runnable
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_game_sensor)

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Initialize sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            ?: throw IllegalStateException("Accelerometer not available on this device")

        // Initialize UI elements
        car = findViewById(R.id.car)
        distanceText = findViewById(R.id.distanceText)
        scoreText = findViewById(R.id.score)
        obstacleLayout = findViewById(R.id.linearLayoutObstacles)
        coinLayout = findViewById(R.id.linearLayoutCoins)

        obstacles = listOf(
            findViewById(R.id.obstacle1),
            findViewById(R.id.obstacle2),
            findViewById(R.id.obstacle3),
            findViewById(R.id.obstacle4),
            findViewById(R.id.obstacle5)
        )

        coins = listOf(
            findViewById(R.id.coin1),
            findViewById(R.id.coin2),
            findViewById(R.id.coin3),
            findViewById(R.id.coin4),
            findViewById(R.id.coin5)
        )

        hearts = listOf(
            findViewById(R.id.heart1),
            findViewById(R.id.heart2),
            findViewById(R.id.heart3)
        )

        // Initialize positions after layout is measured
        val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Get the top and bottom positions of the layouts in screen coordinates
                val layoutPos = IntArray(2)
                obstacleLayout.getLocationOnScreen(layoutPos)
                obstacleLayoutTop = layoutPos[1]
                obstacleLayoutBottom = obstacleLayoutTop + obstacleLayout.height

                coinLayout.getLocationOnScreen(layoutPos)
                coinLayoutBottom = layoutPos[1] + coinLayout.height

                // Set initial positions above the screen
                initialY = obstacleLayoutTop - obstacles[0].height.toFloat()
                for (i in 0..4) {
                    // Position relative to the layout
                    obstacles[i].y = initialY - obstacleLayoutTop
                    coins[i].y = initialY - obstacleLayoutTop - 100
                    obstacles[i].visibility = View.VISIBLE
                    coins[i].visibility = View.VISIBLE
                }
                obstacleLayout.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        }
        obstacleLayout.viewTreeObserver.addOnGlobalLayoutListener(listener)

        // Start the game loop
        startGame()

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        startGame() // Restart the game loop
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        stopGame() // Stop the game loop
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null) // Clean up all callbacks
        gameRunnable = null // Clear reference
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            // This controls the smoothing of the tilt input over time.
            // A lower value makes the response faster and more jittery, while a higher value makes it slower but smoother.
            val filterFactor = 0.06f  // Faster response, less smoothing

            // This controls how much the tilt input is amplified to move the car.
            // A higher value will make the car move more with less tilt (more sensitive).
            val tiltSensitivity = 0.4f  // More responsive to slight tilts

            // This is the sensitivity threshold for the tilt detection.
            // If the tilt input exceeds this value, the car will move in the corresponding direction (left or right).
            val tiltThreshold = 0.4f  // Slight tilt for faster movement

            // This is the neutral zone that determines when the device is considered "level" or in its neutral position
            // (neither tilted left nor right). If the tilt value is within this range, it’s treated as neutral.
            val neutralThreshold = 0.35555f  // Lower neutral zone for quicker reset

            // This is the minimum amount of time (msec) that must pass between consecutive movements of the car.
            // It helps to prevent the car from moving too quickly or too erratically.
            val minTimeBetweenMoves = 100L

            // Initialize filteredX once
            if (filteredX == null) filteredX = it.values[0]

            // Apply filter for faster but smoother response
            filteredX = filteredX!! + (it.values[0] * tiltSensitivity - filteredX!!) * filterFactor

            // Get current time
            val currentTime = System.currentTimeMillis()

            // Allow new tilt movement only if the device has returned to neutral
            if (Math.abs(filteredX!!) < neutralThreshold) {
                tiltReset = true  // Reset the tilt state immediately when within neutral threshold
            }

            // Move car if enough time has passed and the device has reset to neutral
            if (tiltReset && currentTime - lastTiltTime > minTimeBetweenMoves) {
                if (filteredX!! < -tiltThreshold && carPosition < 4) {
                    carPosition++
                    tiltReset = false  // Lock movement until device returns to neutral
                    lastTiltTime = currentTime
                } else if (filteredX!! > tiltThreshold && carPosition > 0) {
                    carPosition--
                    tiltReset = false  // Lock movement until device returns to neutral
                    lastTiltTime = currentTime
                }

                updateCarPosition(carPosition)
            }
        }
    }

    private fun startGame() {
        if (gameRunnable == null) {
            gameRunnable = object : Runnable {
                override fun run() {
                    distance++
                    distanceText.text = "Distance: $distance"

                    // Randomly drop coins or obstacles
                    val itemIndex = Random.nextInt(0, 10)
                    if (itemIndex < 5 && !isObstacleFalling[itemIndex % 5]) {
                        dropObstacle(itemIndex % 5)
                    } else if (itemIndex >= 5 && !isCoinFalling[itemIndex % 5]) {
                        dropCoin(itemIndex % 5)
                    }

                    handler.postDelayed(this, 300)
                }
            }
            gameRunnable?.let { handler.post(it) }
        }
    }

    private fun stopGame() {
        gameRunnable?.let { handler.removeCallbacks(it) }
        // Stop all obstacle and coin animations
        for (i in 0 until 5) {
            if (isObstacleFalling[i]) {
                resetObstacle(i)
            }
            if (isCoinFalling[i]) {
                resetCoin(i)
            }
        }
    }

    private fun updateCarPosition(position: Int) {
        val laneWidth = obstacleLayout.width / 5
        car.x = (position * laneWidth + laneWidth / 2 - car.width / 2).toFloat()
    }

    private fun dropObstacle(index: Int) {
        isObstacleFalling[index] = true
        val obstacle = obstacles[index]
        obstacle.visibility = View.VISIBLE // Ensure visibility
        handler.post(object : Runnable {
            override fun run() {
                // Move the obstacle down by adjusting its y position relative to the layout
                obstacle.y += 20f

                // Get the real screen location
                val location = IntArray(2)
                obstacle.getLocationOnScreen(location)
                val screenY = location[1]

                // Check collision using real screen coordinates
                if (checkCollision(obstacle)) {
                    loseLife()
                    Toast.makeText(this@GameSensorActivity, "Crashed!", Toast.LENGTH_SHORT).show()
                    resetObstacle(index)
                    return
                }

                // Reset if out of screen using real screen coordinates
                if (screenY > obstacleLayoutBottom) {
                    resetObstacle(index)
                    return
                }

                handler.postDelayed(this, 15)
            }
        })
    }

    private fun dropCoin(index: Int) {
        isCoinFalling[index] = true
        val coin = coins[index]
        coin.visibility = View.VISIBLE // Ensure visibility
        handler.post(object : Runnable {
            override fun run() {
                // Move the coin down by adjusting its y position relative to the layout
                coin.y += 20f

                // Get the real screen location
                val location = IntArray(2)
                coin.getLocationOnScreen(location)
                val screenY = location[1]

                // Check collision using real screen coordinates
                if (checkCollision(coin)) {
                    coin.visibility = View.INVISIBLE
                    score++
                    scoreText.text = "Score: $score"
                    resetCoin(index)
                    return
                }

                // Reset if out of screen using real screen coordinates
                if (screenY > coinLayoutBottom) {
                    resetCoin(index)
                    return
                }

                handler.postDelayed(this, 15)
            }
        })
    }

    private fun checkCollision(item: View): Boolean {
        val itemPos = IntArray(2)
        val carPos = IntArray(2)
        item.getLocationOnScreen(itemPos)
        car.getLocationOnScreen(carPos)

        val collisionThresholdY = 50
        val collisionThresholdX = 80
        val xDiff = kotlin.math.abs(itemPos[0] - carPos[0])
        val yDiff = kotlin.math.abs(itemPos[1] - carPos[1])

        return xDiff < collisionThresholdX && yDiff < collisionThresholdY
    }

    private fun resetObstacle(index: Int) {
        isObstacleFalling[index] = false
        val obstacle = obstacles[index]
        obstacle.y = initialY - obstacleLayoutTop // Reset relative to the layout
        obstacle.visibility = View.VISIBLE
    }

    private fun resetCoin(index: Int) {
        isCoinFalling[index] = false
        val coin = coins[index]
        coin.y = initialY - obstacleLayoutTop - 100 // Reset relative to the layout
        coin.visibility = View.VISIBLE
    }

    private fun loseLife() {
        if (lives > 0) {
            lives--
            hearts[lives].visibility = View.INVISIBLE
            if (lives == 0) {
                endGame()
            }
        }
        vibrate()
    }

    private fun vibrate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator.vibrate(
                VibrationEffect.createOneShot(
                    500,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(500)
        }
    }

    private fun saveHighscore(score: Int) {
        val sharedPreferences = getSharedPreferences("Highscores", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        // Load existing highscores
        val highscores = mutableListOf<Triple<String, Int, LatLng>>()
        for (i in 0 until 10) {
            val name = sharedPreferences.getString("name_$i", null)
            val scoreValue = sharedPreferences.getInt("score_$i", 0)
            val lat = sharedPreferences.getFloat("lat_$i", 0f)
            val lng = sharedPreferences.getFloat("lng_$i", 0f)
            if (name != null) {
                highscores.add(Triple(name, scoreValue, LatLng(lat.toDouble(), lng.toDouble())))
            }
        }
        android.util.Log.d("Highscore", "GameSensorActivity: Loaded highscores: $highscores")

        // Add new score with real GPS location
        val playerName = "Player"
        var location = LatLng(40.6975, -73.9795) // Fallback: NY
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    location = LatLng(loc.latitude, loc.longitude)
                    android.util.Log.d(
                        "Highscore",
                        "GameSensorActivity: Got real location: $location"
                    )
                } else {
                    android.util.Log.w(
                        "Highscore",
                        "GameSensorActivity: Location unavailable, using fallback"
                    )
                }
                // Add highscore after getting location
                highscores.add(Triple(playerName, score, location))
                android.util.Log.d(
                    "Highscore",
                    "GameSensorActivity: Added score: $playerName - $score at $location"
                )
                saveHighscoresToPreferences(highscores, editor)
            }.addOnFailureListener { e ->
                android.util.Log.w(
                    "Highscore",
                    "GameSensorActivity: Failed to get location: ${e.message}, using fallback"
                )
                highscores.add(Triple(playerName, score, location))
                android.util.Log.d(
                    "Highscore",
                    "GameSensorActivity: Added score: $playerName - $score at $location"
                )
                saveHighscoresToPreferences(highscores, editor)
            }
        } else {
            highscores.add(Triple(playerName, score, location))
            android.util.Log.d(
                "Highscore",
                "GameSensorActivity: Added score: $playerName - $score at $location (no permission)"
            )
            saveHighscoresToPreferences(highscores, editor)
        }
    }

    private fun saveHighscoresToPreferences(
        highscores: MutableList<Triple<String, Int, LatLng>>,
        editor: android.content.SharedPreferences.Editor
    ) {
        // Sort by score (descending) and keep top 10
        highscores.sortByDescending { it.second }
        if (highscores.size > 10) {
            highscores.subList(10, highscores.size).clear()
        }
        android.util.Log.d("Highscore", "GameSensorActivity: Sorted highscores: $highscores")

        // Save back to SharedPreferences
        try {
            editor.clear()
            highscores.forEachIndexed { index, (name, score, location) ->
                editor.putString("name_$index", name)
                editor.putInt("score_$index", score)
                editor.putFloat("lat_$index", location.latitude.toFloat())
                editor.putFloat("lng_$index", location.longitude.toFloat())
            }
            editor.apply()
            android.util.Log.d("Highscore", "GameSensorActivity: Saved highscores: $highscores")
        } catch (e: Exception) {
            android.util.Log.e(
                "Highscore",
                "GameSensorActivity: Failed to save highscores: ${e.message}"
            )
        }
    }

    private fun endGame() {
        saveHighscore(score) // Save score before resetting
        lives = 3
        hearts.forEach { it.visibility = View.VISIBLE }
        distance = 0
        score = 0
        scoreText.text = "Score: 0"
        distanceText.text = "Distance: 0"
        Toast.makeText(this, "Game Over!", Toast.LENGTH_SHORT).show()
    }
}