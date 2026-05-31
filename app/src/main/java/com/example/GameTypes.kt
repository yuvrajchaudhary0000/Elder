package com.example

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.sqrt

// 3D Vector Representation
data class Vector3D(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Vector3D) = Vector3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3D) = Vector3D(x - other.x, y - other.y, z - other.z)
    operator fun times(factor: Float) = Vector3D(x * factor, y * factor, z * factor)
    
    fun length(): Float = sqrt(x * x + y * y + z * z)
    
    fun normalized(): Vector3D {
        val len = length()
        return if (len > 0f) Vector3D(x / len, y / len, z / len) else Vector3D(0f, 1f, 0f)
    }
    
    fun dot(other: Vector3D): Float = x * other.x + y * other.y + z * other.z
    
    fun cross(other: Vector3D): Vector3D {
        return Vector3D(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x
        )
    }
}

// 3D Face representation for Painter's Algorithm rendering
data class Face3D(
    val vertices: List<Vector3D>,
    val color: Color,
    val showOutline: Boolean = false,
    val outlineColor: Color = Color.Black,
    val isLightSensitive: Boolean = true,
    var forceDrawFirst: Boolean = false
) {
    var avgZ: Float = 0f
}

// Game Mode Enums
enum class GameMode {
    MENU,
    GARAGE,
    PLAYING,
    GAME_OVER,
    MISSION_SUCCESS
}

enum class DrivingMode {
    RACE,        // Career racing mode
    FREE_ROAM    // Free world wandering
}

// Car Specs data structure
data class CarModel(
    val id: String,
    val name: String,
    val price: Int,
    val baseColor: Color,
    val maxSpeed: Float,         // Speed multiplier
    val acceleration: Float,    // Acceleration rate
    val handling: Float,        // Steering speed
    val fuelCapacity: Float,    // Total fuel volume
    val description: String,
    var unlocked: Boolean = false
)

// In-game Vehicle instance (Player or Traffic)
data class GameVehicle(
    var id: String,
    var x: Float,
    var y: Float,
    var z: Float,
    var yaw: Float,               // Steering angle
    var speed: Float,             // Current velocity
    var maxSpeed: Float,
    var color: Color,
    var width: Float = 2.2f,
    var length: Float = 4.5f,
    var height: Float = 1.4f,
    var isTraffic: Boolean = false,
    var targetLane: Int = 0,      // For traffic lanes
    var brakeLight: Boolean = false,
    var targetRoadZ: Boolean = true // True if on central N/S road, False if on E/W side cut
) {
    fun getBoundingBoxPair(): Pair<Vector3D, Vector3D> {
        val halfW = width / 2
        val halfL = length / 2
        // Simplistic axis-aligned bounding box around center for fast collision
        return Pair(
            Vector3D(x - halfW, y, z - halfL),
            Vector3D(x + halfW, y + height, z + halfL)
        )
    }
}

// Fuel Station data structure
data class GasStation(
    val id: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val name: String,
    val onRight: Boolean = true,
    var isRefueling: Boolean = false
)

// Traffic Light signal states
enum class LightState {
    RED,
    YELLOW,
    GREEN
}

// Traffic Intersection Control
data class TrafficLightIntersection(
    val id: Int,
    val z: Float,
    var state: LightState = LightState.GREEN,
    var timer: Int = 0,
    val redDuration: Int = 240,     // Frames
    val yellowDuration: Int = 60,
    val greenDuration: Int = 240
) {
    fun update() {
        timer++
        val cycle = redDuration + yellowDuration + greenDuration
        val currentInCycle = timer % cycle
        state = when {
            currentInCycle < greenDuration -> LightState.GREEN
            currentInCycle < greenDuration + yellowDuration -> LightState.YELLOW
            else -> LightState.RED
        }
    }
}

// Preset missions
data class Mission(
    val id: String,
    val title: String,
    val description: String,
    val reward: Int,
    val targetDistance: Float = 1000f,
    val maxCollisions: Int = 0,
    val initialFuel: Float = 1.0f,
    val timeLimitSec: Int = 60,
    val driveMode: DrivingMode = DrivingMode.RACE
)

// Holds 2D projected coordinates with average camera depth for layout sorting
data class ProjectedFace(
    val points: List<Offset>,
    val color: Color,
    val showOutline: Boolean,
    val outlineColor: Color,
    val avgZ: Float
)
