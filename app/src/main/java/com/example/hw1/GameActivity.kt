package com.example.hw1

import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.widget.*
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class GameActivity : AppCompatActivity() {

    private lateinit var car: ImageView
    private lateinit var leftBtn: Button
    private lateinit var rightBtn: Button
    private lateinit var obstacles: List<ImageView>
    private lateinit var obstaclesInitPlaces: List<IntArray>
    private val isObstacleFalling = MutableList(3) { false }
    private lateinit var hearts: List<ImageView>
    private var carPosition = 1 // 0 = left, 1 = center, 2 = right
    private var lives = 3
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var gameLoop: Runnable
    private lateinit var dropRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;

        onBackPressedDispatcher.addCallback(this) {
            onPause()
        }

        car = findViewById(R.id.car)
        leftBtn = findViewById(R.id.leftBtn)
        rightBtn = findViewById(R.id.rightBtn)

        obstacles = listOf(
            findViewById(R.id.obstacle1),
            findViewById(R.id.obstacle2),
            findViewById(R.id.obstacle3)
        )

        hearts = listOf(
            findViewById(R.id.heart1),
            findViewById(R.id.heart2),
            findViewById(R.id.heart3)
        )

        leftBtn.setOnClickListener {
            if (carPosition > 0) {
                carPosition--
                updateCarPosition(carPosition)
            }
        }

        rightBtn.setOnClickListener {
            if (carPosition < 2) {
                carPosition++
                updateCarPosition(carPosition)
            }
        }

        val rootView = findViewById<View>(R.id.linearLayoutObstacles)
        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val obstacle1Pos = IntArray(2)
            val obstacle2Pos = IntArray(2)
            val obstacle3Pos = IntArray(2)
            obstacles[0].getLocationOnScreen(obstacle1Pos)
            obstacles[1].getLocationOnScreen(obstacle2Pos)
            obstacles[2].getLocationOnScreen(obstacle3Pos)
            obstacle1Pos[1] = 40
            obstacle2Pos[1] = 40
            obstacle3Pos[1] = 40
            obstaclesInitPlaces = listOf(
                obstacle1Pos,
                obstacle2Pos,
                obstacle3Pos
            )
        }

        startGame()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(gameLoop)
        handler.removeCallbacks(dropRunnable)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(gameLoop)
        handler.removeCallbacks(dropRunnable)
        finish()
    }

    private fun startGame() {
        gameLoop = object : Runnable {
            override fun run() {
                val obstacleIndex = Random.nextInt(0, 3)
                if (!isObstacleFalling[obstacleIndex])
                    dropObstacle(obstacleIndex)
                handler.postDelayed(this, 300)
            }
        }
        handler.post(gameLoop)
    }

    private fun updateCarPosition(carPosition: Int) {
        car.x = obstaclesInitPlaces[carPosition][0].toFloat() + 100
    }

    private fun dropObstacle(index: Int) {
        val obstacle = obstacles[index]
        isObstacleFalling[index] = true
        dropRunnable = object : Runnable {
            override fun run() {
                // Move obstacle down
                val newY = obstacle.y + 20f
                obstacle.y = newY

                // Get global positions
                val obstaclePos = IntArray(2)
                val carPos = IntArray(2)
                obstacle.getLocationOnScreen(obstaclePos)
                car.getLocationOnScreen(carPos)

                // Compare global Y and X (with tolerance)
                val collisionThresholdY = 50
                val collisionThresholdX = 110
                val xDiff = kotlin.math.abs(obstaclePos[0] - carPos[0])
                val yDiff = kotlin.math.abs(obstaclePos[1] - carPos[1])

                if (xDiff < collisionThresholdX && yDiff < collisionThresholdY) {
                    loseLife()
                    obstacle.visibility = View.INVISIBLE
                    resetObstacle(index)
                    return
                }

                if (yDiff < collisionThresholdY) {
                    obstacle.visibility = View.INVISIBLE
                    resetObstacle(index)
                    return
                }

                handler.postDelayed(this, 15)
            }
        }

        handler.post(dropRunnable)
    }

    // Helper function to reset the obstacle to the top at a random lane
    private fun resetObstacle(index: Int) {
        val obstacle = obstacles[index]
        isObstacleFalling[index] = false
        obstacle.x = obstaclesInitPlaces[index][0].toFloat()
        obstacle.y = obstaclesInitPlaces[index][1].toFloat()
        obstacle.visibility = View.VISIBLE
    }

    private fun loseLife() {
        if (lives > 0) {
            lives--
            hearts[lives].visibility = View.INVISIBLE
        }
        Toast.makeText(this, "Crashed!", Toast.LENGTH_LONG).show()
        vibrate(500)
        if (lives == 0) {
            endGame()
        }
    }

    private fun vibrate(durationMillis: Long = 2000) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                // API 31 and above: VibratorManager
                val vibratorManager =
                    this.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.cancel()
                vibrator.vibrate(
                    VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                // API 26 to 30: Vibrator with VibrationEffect
                val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.cancel()
                vibrator.vibrate(
                    VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }

            else -> {
                // Below API 26: Deprecated vibrate method without VibrationEffect
                val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.cancel()
                @Suppress("DEPRECATION")
                vibrator.vibrate(durationMillis)
            }
        }
    }

    private fun endGame() {
        lives = 3
        hearts[0].visibility = View.VISIBLE
        hearts[1].visibility = View.VISIBLE
        hearts[2].visibility = View.VISIBLE
    }
}
