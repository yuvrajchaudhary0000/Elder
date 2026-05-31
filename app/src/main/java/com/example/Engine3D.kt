package com.example

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

object Engine3D {
    // Global Directional Light Source (the sun overhead-front-right)
    private val lightDirection = Vector3D(0.4f, 0.8f, 0.4f).normalized()

    // Project world coordinates to 2D screen coordinate
    // Returns null if clipped behind the camera near plane
    fun projectPoint(
        point: Vector3D,
        camX: Float,
        camY: Float,
        camZ: Float,
        yaw: Float,
        pitch: Float,
        width: Float,
        height: Float,
        fovDeg: Float = 60f
    ): Vector3D? {
        // 1. Translate
        val dx = point.x - camX
        val dy = point.y - camY
        val dz = point.z - camZ

        // 2. Rotate Yaw (Around Y-axis)
        // Camera rotates, so we rotate vertices in the opposite direction
        val cosY = cos(-yaw)
        val sinY = sin(-yaw)
        val rx1 = dx * cosY - dz * sinY
        val rz1 = dx * sinY + dz * cosY

        // 3. Rotate Pitch (Around X-axis)
        val cosP = cos(-pitch)
        val sinP = sin(-pitch)
        val ry2 = dy * cosP - rz1 * sinP
        val rz2 = dy * sinP + rz1 * cosP

        // Near plane clipping
        if (rz2 < 0.1f) return null

        // 4. Perspective Projection
        val fovAngleRad = Math.toRadians(fovDeg.toDouble()).toFloat()
        val fov = 1.0f / tan(fovAngleRad / 2f)

        // Uniform aspect ratio scaling relative to height
        val scaleX = fov * (height / 2f)
        val scaleY = fov * (height / 2f)

        val screenX = width / 2f + (rx1 / rz2) * scaleX
        val screenY = height / 2f - (ry2 / rz2) * scaleY

        // We return screen coordinates in X & Y, and raw relative depth in Z
        return Vector3D(screenX, screenY, rz2)
    }

    // Rotates a vertex around an arbitrary Y axis pivot
    fun rotateVertexY(v: Vector3D, pivotX: Float, pivotZ: Float, angle: Float): Vector3D {
        val cosA = cos(angle)
        val sinA = sin(angle)
        val dx = v.x - pivotX
        val dz = v.z - pivotZ
        val rx = dx * cosA - dz * sinA
        val rz = dx * sinA + dz * cosA
        return Vector3D(pivotX + rx, v.y, pivotZ + rz)
    }

    // Flat Shading color helper
    fun calculateFlatShading(baseColor: Color, v1: Vector3D, v2: Vector3D, v3: Vector3D): Color {
        val edge1 = v2 - v1
        val edge2 = v3 - v1
        val normal = edge1.cross(edge2).normalized()

        // Dot product with sun direction vector
        val dot = normal.dot(lightDirection)
        // Convert -1..1 to a reasonable intensity range (0.35 to 1.0)
        val intensity = 0.35f + 0.65f * ((dot + 1f) / 2f)

        return Color(
            red = (baseColor.red * intensity).coerceIn(0f, 1f),
            green = (baseColor.green * intensity).coerceIn(0f, 1f),
            blue = (baseColor.blue * intensity).coerceIn(0f, 1f),
            alpha = baseColor.alpha
        )
    }

    // ==========================================
    // 3D BOX (CUBE) MESH GENERATION
    // ==========================================
    fun createCubeMesh(
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        color: Color,
        showOutline: Boolean = false,
        outlineColor: Color = Color.Black,
        isLightSensitive: Boolean = true,
        forceDrawFirst: Boolean = false,
        yaw: Float = 0f
    ): List<Face3D> {
        val hx = sx / 2f
        val hz = sz / 2f

        // Initial vertices before yaw rotation
        val localVerts = listOf(
            Vector3D(cx - hx, cy, cz - hz),       // 0: Bottom-Left-Back
            Vector3D(cx + hx, cy, cz - hz),       // 1: Bottom-Right-Back
            Vector3D(cx + hx, cy + sy, cz - hz),  // 2: Top-Right-Back
            Vector3D(cx - hx, cy + sy, cz - hz),  // 3: Top-Left-Back
            Vector3D(cx - hx, cy, cz + hz),       // 4: Bottom-Left-Front
            Vector3D(cx + hx, cy, cz + hz),       // 5: Bottom-Right-Front
            Vector3D(cx + hx, cy + sy, cz + hz),  // 6: Top-Right-Front
            Vector3D(cx - hx, cy + sy, cz + hz)   // 7: Top-Left-Front
        )

        // Rotate vertices around cube center (cx, cz) if yaw is not zero
        val verts = if (yaw != 0f) {
            localVerts.map { rotateVertexY(it, cx, cz, yaw) }
        } else {
            localVerts
        }

        // Define faces (Counter-clockwise winding order for outward facing normals)
        val faces = mutableListOf<Face3D>()

        // 1. Front face [4, 5, 6, 7]
        var col = if (isLightSensitive) calculateFlatShading(color, verts[4], verts[5], verts[6]) else color
        faces.add(Face3D(listOf(verts[4], verts[5], verts[6], verts[7]), col, showOutline, outlineColor, isLightSensitive, forceDrawFirst))

        // 2. Back face [1, 0, 3, 2]
        col = if (isLightSensitive) calculateFlatShading(color, verts[1], verts[0], verts[3]) else color
        faces.add(Face3D(listOf(verts[1], verts[0], verts[3], verts[2]), col, showOutline, outlineColor, isLightSensitive, forceDrawFirst))

        // 3. Left face [0, 4, 7, 3]
        col = if (isLightSensitive) calculateFlatShading(color, verts[0], verts[4], verts[7]) else color
        faces.add(Face3D(listOf(verts[0], verts[4], verts[7], verts[3]), col, showOutline, outlineColor, isLightSensitive, forceDrawFirst))

        // 4. Right face [5, 1, 2, 6]
        col = if (isLightSensitive) calculateFlatShading(color, verts[5], verts[1], verts[2]) else color
        faces.add(Face3D(listOf(verts[5], verts[1], verts[2], verts[6]), col, showOutline, outlineColor, isLightSensitive, forceDrawFirst))

        // 5. Top face [3, 7, 6, 2]
        col = if (isLightSensitive) calculateFlatShading(color, verts[3], verts[7], verts[6]) else color
        faces.add(Face3D(listOf(verts[3], verts[7], verts[6], verts[2]), col, showOutline, outlineColor, isLightSensitive, forceDrawFirst))

        // 6. Bottom face [0, 1, 5, 4]
        col = if (isLightSensitive) calculateFlatShading(color, verts[0], verts[1], verts[5]) else color
        faces.add(Face3D(listOf(verts[0], verts[1], verts[5], verts[4]), col, showOutline, outlineColor, isLightSensitive, forceDrawFirst))

        return faces
    }

    // ==========================================
    // 3D PYRAMID MESH GENERATION (Trees, roofs)
    // ==========================================
    fun createPyramidMesh(
        cx: Float, cy: Float, cz: Float,
        sx: Float, sy: Float, sz: Float,
        color: Color
    ): List<Face3D> {
        val hx = sx / 2f
        val hz = sz / 2f

        val vBase0 = Vector3D(cx - hx, cy, cz - hz)
        val vBase1 = Vector3D(cx + hx, cy, cz - hz)
        val vBase2 = Vector3D(cx + hx, cy, cz + hz)
        val vBase3 = Vector3D(cx - hx, cy, cz + hz)
        val tip = Vector3D(cx, cy + sy, cz)

        val faces = mutableListOf<Face3D>()

        // Base [0, 1, 2, 3]
        var col = calculateFlatShading(color, vBase0, vBase1, vBase2)
        faces.add(Face3D(listOf(vBase0, vBase1, vBase2, vBase3), col, false))

        // Front [0, tip, 1]
        col = calculateFlatShading(color, vBase0, tip, vBase1)
        faces.add(Face3D(listOf(vBase0, tip, vBase1), col, false))

        // Right [1, tip, 2]
        col = calculateFlatShading(color, vBase1, tip, vBase2)
        faces.add(Face3D(listOf(vBase1, tip, vBase2), col, false))

        // Back [2, tip, 3]
        col = calculateFlatShading(color, vBase2, tip, vBase3)
        faces.add(Face3D(listOf(vBase2, tip, vBase3), col, false))

        // Left [3, tip, 0]
        col = calculateFlatShading(color, vBase3, tip, vBase0)
        faces.add(Face3D(listOf(vBase3, tip, vBase0), col, false))

        return faces
    }

    // ==========================================
    // 3D COMPOUND VEHICLE MESH GENERATION
    // ==========================================
    fun createVehicleMesh(
        x: Float, y: Float, z: Float,
        yaw: Float,
        bodyColor: Color,
        carId: String
    ): List<Face3D> {
        val faces = mutableListOf<Face3D>()

        // Visual variables depending on model type
        val isPolice = carId == "taxi" || carId == "police" // Adds emergency sirens
        val isSports = carId == "sports" || carId == "drift" || carId == "golden"
        val isSuv = carId == "suv"

        val chassisW = 2.0f
        val chassisH = 0.5f
        val chassisL = 4.2f

        val cabinW = 1.7f
        val cabinH = 0.6f
        val cabinL = 2.2f
        val cabinZOffset = -0.3f // Shift slightly back

        // 1. Lower Chassis (Main body)
        faces.addAll(
            createCubeMesh(
                x, y + 0.3f, z,
                chassisW, chassisH, chassisL,
                bodyColor,
                showOutline = true,
                outlineColor = Color.DarkGray,
                yaw = yaw
            )
        )

        // 2. Cabin Roof
        val cabY = y + 0.3f + chassisH
        faces.addAll(
            createCubeMesh(
                x, cabY, z + cabinZOffset,
                cabinW, cabinH, cabinL,
                bodyColor,
                showOutline = true,
                outlineColor = Color.DarkGray,
                yaw = yaw
            )
        )

        // 3. Cabin Windows & Windshield - Overlay meshes
        // To be simpler and resilient in depth sorting, we can generate custom stylized details
        val glassColor = Color(0xFF1E293B) // Dark slate
        val headlightColor = Color(0xFFFDE047) // Glowing Yellow
        val brakeColor = Color(0xFFEF4444) // Glowing Red

        // Front windshield face overlay (At cabin front)
        // Offset slightly frontwards on rotated z
        val windshieldL = 0.1f
        val windshieldW = 1.5f
        val windshieldHeight = 0.5f
        val windshieldZ = z + cabinZOffset + (cabinL / 2f) + 0.05f
        faces.addAll(
            createCubeMesh(
                x, cabY + 0.05f, windshieldZ,
                windshieldW, windshieldHeight, windshieldL,
                glassColor,
                yaw = yaw
            )
        )

        // Rear window (Cabin back)
        val rearWindowZ = z + cabinZOffset - (cabinL / 2f) - 0.05f
        faces.addAll(
            createCubeMesh(
                x, cabY + 0.05f, rearWindowZ,
                windshieldW, windshieldHeight, windshieldL,
                glassColor,
                yaw = yaw
            )
        )

        // Front Headlights
        val headlightW = 0.3f
        val headlightH = 0.2f
        val headlightL = 0.1f
        val headlightZ = z + (chassisL / 2f) + 0.05f
        val headlightXOffset = 0.7f

        // Left Headlight
        faces.addAll(
            createCubeMesh(
                x - headlightXOffset, y + 0.45f, headlightZ,
                headlightW, headlightH, headlightL,
                headlightColor,
                isLightSensitive = false,
                yaw = yaw
            )
        )
        // Right Headlight
        faces.addAll(
            createCubeMesh(
                x + headlightXOffset, y + 0.45f, headlightZ,
                headlightW, headlightH, headlightL,
                headlightColor,
                isLightSensitive = false,
                yaw = yaw
            )
        )

        // Reverse Brake Lights
        val brakeW = 0.3f
        val brakeH = 0.15f
        val brakeL = 0.1f
        val brakeZ = z - (chassisL / 2f) - 0.05f
        // Left Brake Light
        faces.addAll(
            createCubeMesh(
                x - headlightXOffset, y + 0.45f, brakeZ,
                brakeW, brakeH, brakeL,
                brakeColor,
                isLightSensitive = false,
                yaw = yaw
            )
        )
        // Right Brake Light
        faces.addAll(
            createCubeMesh(
                x + headlightXOffset, y + 0.45f, brakeZ,
                brakeW, brakeH, brakeL,
                brakeColor,
                isLightSensitive = false,
                yaw = yaw
            )
        )

        // 4. Wheels (represented as four dark grey cubes under the chassis edge)
        val wheelW = 0.35f
        val wheelH = 0.5f
        val wheelL = 0.65f
        val wheelY = y + 0.05f
        
        val wheelXOffset = 0.95f
        val wheelZOffset = 1.3f

        // Front-Left Wheel
        faces.addAll(createCubeMesh(x - wheelXOffset, wheelY, z + wheelZOffset, wheelW, wheelH, wheelL, Color.DarkGray, yaw = yaw))
        // Front-Right Wheel
        faces.addAll(createCubeMesh(x + wheelXOffset, wheelY, z + wheelZOffset, wheelW, wheelH, wheelL, Color.DarkGray, yaw = yaw))
        // Rear-Left Wheel
        faces.addAll(createCubeMesh(x - wheelXOffset, wheelY, z - wheelZOffset, wheelW, wheelH, wheelL, Color.DarkGray, yaw = yaw))
        // Rear-Right Wheel
        faces.addAll(createCubeMesh(x + wheelXOffset, wheelY, z - wheelZOffset, wheelW, wheelH, wheelL, Color.DarkGray, yaw = yaw))

        // 5. Special decorations (Police Siren, Taxi Sign, Sports Rear Wing)
        if (carId == "police") {
            // Blue/Red siren bar on roof
            faces.addAll(createCubeMesh(x - 0.3f, cabY + cabinH, z + cabinZOffset, 0.4f, 0.15f, 0.3f, Color.Blue, isLightSensitive = false, yaw = yaw))
            faces.addAll(createCubeMesh(x + 0.3f, cabY + cabinH, z + cabinZOffset, 0.4f, 0.15f, 0.3f, Color.Red, isLightSensitive = false, yaw = yaw))
        } else if (carId == "taxi") {
            // Yellow Taxi Sign
            faces.addAll(createCubeMesh(x, cabY + cabinH, z + cabinZOffset, 0.5f, 0.18f, 0.3f, Color(0xFFFBBF24), yaw = yaw))
        } else if (isSports) {
            // Rear Spoiler Wing
            val wingY = cabY + 0.1f
            val wingZ = z - (chassisL / 2f) + 0.2f
            faces.addAll(createCubeMesh(x, wingY, wingZ, 1.8f, 0.08f, 0.4f, bodyColor, yaw = yaw))
            // Spoiler supports
            faces.addAll(createCubeMesh(x - 0.7f, y + 0.5f, wingZ, 0.08f, 0.4f, 0.1f, Color.Black, yaw = yaw))
            faces.addAll(createCubeMesh(x + 0.7f, y + 0.5f, wingZ, 0.08f, 0.4f, 0.1f, Color.Black, yaw = yaw))
        }

        return faces
    }

    // ==========================================
    // 3D GAS STATION MESH GENERATION
    // ==========================================
    fun createGasStationMesh(
        x: Float, y: Float, z: Float,
        timeScale: Float = 0f
    ): List<Face3D> {
        val faces = mutableListOf<Face3D>()

        // 1. Concrete Ground Pad
        faces.addAll(createCubeMesh(x, y, z, 7f, 0.1f, 10f, Color.LightGray, showOutline = true))

        // 2. Pillars (Left & Right support poles)
        faces.addAll(createCubeMesh(x - 2.8f, y + 0.1f, z - 4f, 0.3f, 4.0f, 0.3f, Color.Gray))
        faces.addAll(createCubeMesh(x + 2.8f, y + 0.1f, z - 4f, 0.3f, 4.0f, 0.3f, Color.Gray))
        faces.addAll(createCubeMesh(x - 2.8f, y + 0.1f, z + 4f, 0.3f, 4.0f, 0.3f, Color.Gray))
        faces.addAll(createCubeMesh(x + 2.8f, y + 0.1f, z + 4f, 0.3f, 4.0f, 0.3f, Color.Gray))

        // 3. Overhead Canopy (Bright Red)
        faces.addAll(createCubeMesh(x, y + 4.0f, z, 7.2f, 0.6f, 10.5f, Color(0xFFDC2626), showOutline = true, outlineColor = Color.Red))

        // 4. Gas Pump Station (Yellow / Central box)
        faces.addAll(createCubeMesh(x, y + 0.1f, z, 1.2f, 1.8f, 0.8f, Color(0xFFFBBF24), showOutline = true))
        // Screen decoration
        faces.addAll(createCubeMesh(x, y + 1.2f, z + 0.42f, 0.8f, 0.4f, 0.05f, Color.Black))

        // 5. Spinning canister icon (Floating on top)
        val spinAngle = timeScale * 0.05f
        faces.addAll(
            createCubeMesh(
                x, y + 5.5f, z,
                1.0f, 1.0f, 1.0f,
                Color(0xFF22C55E), // Vivid green
                showOutline = true,
                outlineColor = Color.White,
                isLightSensitive = false,
                yaw = spinAngle
            )
        )

        return faces
    }

    // ==========================================
    // 3D TRAFFIC LIGHT MESH GENERATION
    // ==========================================
    fun createTrafficLightMesh(
        x: Float, y: Float, z: Float,
        state: LightState
    ): List<Face3D> {
        val faces = mutableListOf<Face3D>()

        // 1. Heavy Black Base
        faces.addAll(createCubeMesh(x, y, z, 0.8f, 0.2f, 0.8f, Color.Black))

        // 2. Tall Metal Pole
        faces.addAll(createCubeMesh(x, y + 0.2f, z, 0.2f, 3.8f, 0.2f, Color(0xFF475569)))

        // 3. Signal Housing (Black container box at top)
        val houseY = y + 3.0f
        faces.addAll(createCubeMesh(x, houseY, z, 0.5f, 1.2f, 0.5f, Color(0xFF1E293B), showOutline = true, outlineColor = Color.Black))

        // 4. Glowing lights (represented as high contrast cubes on front and back faces)
        // Red, Yellow, Green bulbs
        val lightL = 0.05f
        val lightW = 0.3f
        val lightH = 0.25f

        val redOn = state == LightState.RED
        val yellowOn = state == LightState.YELLOW
        val greenOn = state == LightState.GREEN

        // Red bulb at top of housing (y = houseY + 0.35f)
        faces.addAll(
            createCubeMesh(
                x, houseY + 0.35f, z + 0.26f,
                lightW, lightH, lightL,
                if (redOn) Color(0xFFEF4444) else Color(0x33EF4444), // Glow vs dim
                isLightSensitive = false
            )
        )

        // Yellow bulb in middle of housing (y = houseY)
        faces.addAll(
            createCubeMesh(
                x, houseY, z + 0.26f,
                lightW, lightH, lightL,
                if (yellowOn) Color(0xFFFBBF24) else Color(0x33FBBF24),
                isLightSensitive = false
            )
        )

        // Green bulb at bottom of housing (y = houseY - 0.35f)
        faces.addAll(
            createCubeMesh(
                x, houseY - 0.35f, z + 0.26f,
                lightW, lightH, lightL,
                if (greenOn) Color(0xFF22C55E) else Color(0x3322C55E),
                isLightSensitive = false
            )
        )

        return faces
    }

    // ==========================================
    // 3D SCENERY ITEMS
    // ==========================================
    fun createTreeMesh(x: Float, y: Float, z: Float, scale: Float = 1.0f): List<Face3D> {
        val faces = mutableListOf<Face3D>()

        // Trunk (Brown box)
        faces.addAll(createCubeMesh(x, y, z, 0.4f * scale, 1.2f * scale, 0.4f * scale, Color(0xFF78350F)))

        // Foliage (Green Pyramid of various sizes stacked)
        val leafColor = Color(0xFF15803D)
        faces.addAll(createPyramidMesh(x, y + 1.2f * scale, z, 2.2f * scale, 1.8f * scale, 2.2f * scale, leafColor))
        faces.addAll(createPyramidMesh(x, y + 2.4f * scale, z, 1.6f * scale, 1.4f * scale, 1.6f * scale, Color(0xFF166534)))

        return faces
    }

    fun createSkyscraperMesh(
        x: Float, y: Float, z: Float,
        width: Float, height: Float, length: Float,
        baseColor: Color
    ): List<Face3D> {
        val faces = mutableListOf<Face3D>()
        // Main block
        faces.addAll(createCubeMesh(x, y, z, width, height, length, baseColor, showOutline = true, outlineColor = Color(0x22FFFFFF)))

        // Add 3D windows rows (represented as small bluish/yellow cells overlaid)
        // For performance and depth stability, we generate simple glowing glass panels
        val windowColor = Color(0xCC60A5FA) // Glow cyan
        val windowCols = 3
        val windowRows = 4
        
        val winW = width / (windowCols * 2f + 1f)
        val winH = height / (windowRows * 2f + 1f)

        // Only put windows on outer walls facing the road
        // Side wall overlay
        if (x > 0) { // On the right side, windows face left (toward the road)
            val winX = x - width / 2f - 0.02f
            for (r in 0 until windowRows) {
                val winY = y + winH + r * winH * 2.1f
                for (c in 0 until windowCols) {
                    val winZ = z - length / 2f + winW + c * winW * 2.1f
                    faces.add(
                        Face3D(
                            listOf(
                                Vector3D(winX, winY, winZ),
                                Vector3D(winX, winY + winH, winZ),
                                Vector3D(winX, winY + winH, winZ + winW),
                                Vector3D(winX, winY, winZ + winW)
                            ),
                            windowColor,
                            isLightSensitive = false
                        )
                    )
                }
            }
        } else { // On the left side, windows face right (toward the road)
            val winX = x + width / 2f + 0.02f
            for (r in 0 until windowRows) {
                val winY = y + winH + r * winH * 2.1f
                for (c in 0 until windowCols) {
                    val winZ = z - length / 2f + winW + c * winW * 2.1f
                    faces.add(
                        Face3D(
                            listOf(
                                Vector3D(winX, winY, winZ + winW),
                                Vector3D(winX, winY + winH, winZ + winW),
                                Vector3D(winX, winY + winH, winZ),
                                Vector3D(winX, winY, winZ)
                            ),
                            windowColor,
                            isLightSensitive = false
                        )
                    )
                }
            }
        }

        return faces
    }
}
