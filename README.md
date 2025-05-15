Android Sensor-Based obstacle avoiding Game

Overview:
This Android application is a sensor-based racing game where players control a car using the device's accelerometer to navigate through five lanes, 
avoiding obstacles and collecting coins. The game tracks score, distance, and lives, with highscores saved using SharedPreferences and GPS location data. 
The game features sound effects, vibration feedback, and a simple UI built with Android's ImageView and LinearLayout.

The steering mechanism in the onSensorChanged method has been updated to use a range-based approach for mapping accelerometer X-values to the car's lane position,
replacing the previous threshold-based method. This change improves the smoothness and precision of car control, 
allowing players to tilt the device to directly position the car in one of five lanes.

Purpose:
The original threshold-based steering required the device to return to a neutral position between lane changes and used timing constraints to limit movement frequency. 
This made the controls feel less responsive and less intuitive. 

The new range-based approach:
Maps the car's position (0 to 4, corresponding to five lanes) directly to specific ranges of filtered accelerometer X-values.
Provides smoother and more precise control by continuously reflecting the degree of tilt.
Simplifies the logic by removing complex threshold and timing checks.

==============
|Changes Made|
==============
Updated onSensorChanged:
Removed Variables: Eliminated tiltReset, lastTiltTime, tiltThreshold, neutralThreshold, and minTimeBetweenMoves as they were specific to the threshold-based logic.
Retained Smoothing: Kept filterFactor = 0.06f and tiltSensitivity = 0.4f to ensure smooth accelerometer input.
Range-Based Logic: Implemented a when block to map the filtered X-value (filteredX) to the car's lane position (carPosition):
-10.0f to -2.0f: Position 4 (rightmost lane, strong right tilt)
-2.0f to -0.6f: Position 3 (right-middle lane)
-0.6f to 0.6f: Position 2 (center lane, neutral)
0.6f to 2.0f: Position 1 (left-middle lane)
2.0f to 10.0f: Position 0 (leftmost lane, strong left tilt)
else: Maintains current carPosition for extreme values

Efficient Updates: 
The car’s position is updated only when the new position differs from the current carPosition, reducing unnecessary UI updates.

Removed Class Properties:
Deleted tiltReset and lastTiltTime from the class, as they are no longer used.

Unchanged Functionality:
All other game mechanics (obstacle and coin spawning, collision detection, lives, score, distance, highscore saving, vibration, and sound effects) remain unchanged.
The updateCarPosition method continues to position the car in the correct lane based on carPosition.

Benefits:
Smoother Control: The car’s position directly corresponds to the device’s tilt, eliminating the need to return to neutral between moves.
More Precise: Each lane is tied to a specific accelerometer range, allowing fine-grained control based on tilt intensity.
Simpler Code: The removal of threshold and timing logic makes the steering mechanism easier to maintain and tune.

Testing Recommendations:
To ensure the updated steering mechanism works as intended:

Range Tuning: Test the accelerometer ranges (-10.0f to -2.0f, -2.0f to -0.5f, etc.) on a physical Android device. Adjust the boundaries if the car is too sensitive (e.g., narrow the neutral range -0.5f to 0.5f) or not responsive enough (e.g., widen the ranges).
Smoothing Adjustments: If the steering feels jittery, increase filterFactor (e.g., to 0.1f) for more smoothing. If it’s sluggish, decrease filterFactor (e.g., to 0.03f) or increase tiltSensitivity (e.g., to 0.5f).
Device Variability: Test on multiple Android devices, as accelerometer sensitivity varies. Consider adding a calibration screen for users to adjust sensitivity if needed.
Edge Cases: Verify that extreme tilts (beyond ±10.0f) don’t cause issues, as the else clause maintains the current position.
Gameplay Impact: Playtest to ensure the new controls feel intuitive and don’t negatively affect the ability to avoid obstacles or collect coins.

Project Structure:
Main File: GameSensorActivity.kt
Handles game logic, accelerometer input, UI updates, and highscore management.


Layout: res/layout/activity_game_sensor.xml (not modified)
Defines the game UI with a car, obstacles, coins, hearts, and text views for score and distance.


Resources:
R.raw.crash: Sound file for crash events.
Image resources for the car, obstacles, coins, and hearts (referenced in the layout).


Dependencies:
Android SDK
Google Play Services (for FusedLocationProviderClient to save highscores with GPS location)
Standard Android libraries (SensorManager, MediaPlayer, Vibrator, etc.)

How to Run:
Clone the repository or import the project into Android Studio.
Ensure the necessary permissions are declared in AndroidManifest.xml:
ACCESS_FINE_LOCATION (for highscore GPS data)
VIBRATE (for feedback)

Build and run the app on a physical Android device with an accelerometer.
Tilt the device left or right to steer the car, avoiding obstacles and collecting coins.

