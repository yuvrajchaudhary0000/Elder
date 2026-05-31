package com.example

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Force Landscape mode for arcade view
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        enableEdgeToEdge()
        
        sharedPrefs = getSharedPreferences("YuvrajDrivePrefs", Context.MODE_PRIVATE)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0F172A) // Sleek slate base
                ) {
                    GameManager(sharedPrefs)
                }
            }
        }
    }
}

@Composable
fun GameManager(sharedPrefs: SharedPreferences) {
    val context = LocalContext.current

    // Persistent values
    var savedCoins by remember { mutableStateOf(sharedPrefs.getInt("COINS_COUNT", 200)) }
    var unlockedCarsString by remember { mutableStateOf(sharedPrefs.getString("UNLOCKED_CARS", "sedan") ?: "sedan") }

    // Save helpers
    fun saveCoins(newCoins: Int) {
        savedCoins = newCoins
        sharedPrefs.edit().putInt("COINS_COUNT", newCoins).apply()
    }

    fun unlockCar(carId: String, cost: Int) {
        if (savedCoins >= cost) {
            val updated = "$unlockedCarsString,$carId"
            unlockedCarsString = updated
            sharedPrefs.edit().putString("UNLOCKED_CARS", updated).apply()
            saveCoins(savedCoins - cost)
        }
    }

    // Car catalog definition (5 distinct vehicles)
    val carCatalog = remember(unlockedCarsString) {
        val unlockedList = unlockedCarsString.split(",")
        listOf(
            CarModel(
                id = "sedan",
                name = "Hustler Classic",
                price = 0,
                baseColor = Color(0xFF3B82F6), // Ocean Blue
                maxSpeed = 1.0f,
                acceleration = 0.8f,
                handling = 1.0f,
                fuelCapacity = 100f,
                description = "Standard touring sedan. Reliable and balanced. Given free!",
                unlocked = true
            ),
            CarModel(
                id = "taxi",
                name = "Metropolitan Yellow",
                price = 350,
                baseColor = Color(0xFFEAB308), // Yellow Taxi
                maxSpeed = 1.2f,
                acceleration = 0.9f,
                handling = 1.1f,
                fuelCapacity = 120f,
                description = "Custom taxi tuning. Highly reactive handling, ideal for alleys.",
                unlocked = unlockedList.contains("taxi")
            ),
            CarModel(
                id = "suv",
                name = "Mountain Cruiser 4x4",
                price = 800,
                baseColor = Color(0xFF10B981), // Emerald Green
                maxSpeed = 1.1f,
                acceleration = 0.7f,
                handling = 0.8f,
                fuelCapacity = 180f, // Massive tank!
                description = "Heavy duty structure. Massive fuel tank, takes fewer station stops.",
                unlocked = unlockedList.contains("suv")
            ),
            CarModel(
                id = "drift",
                name = "Tokyo Street Drift",
                price = 1600,
                baseColor = Color(0xFFEC4899), // Neon Pink
                maxSpeed = 1.4f,
                acceleration = 1.2f,
                handling = 1.5f, // Dynamic rotation!
                fuelCapacity = 110f,
                description = "Extreme drift layout. Slids through sharp cuts at 120km/h.",
                unlocked = unlockedList.contains("drift")
            ),
            CarModel(
                id = "golden",
                name = "Yuvraj Signature Pro",
                price = 3000,
                baseColor = Color(0xFFFBBF24), // Metallic Gold
                maxSpeed = 1.7f,
                acceleration = 1.5f,
                handling = 1.3f,
                fuelCapacity = 140f,
                description = "Golden Hypercar. Doubled speed and premium golden flat-shading! Developed by Yuvraj Chaudhary.",
                unlocked = unlockedList.contains("golden")
            )
        )
    }

    // Active Selection State
    var selectedCarIdx by remember { mutableStateOf(0) }
    var activeCar = carCatalog[selectedCarIdx]

    // Navigation Screens
    var gameModeState by remember { mutableStateOf(GameMode.MENU) }
    var activeDrivingMode by remember { mutableStateOf(DrivingMode.FREE_ROAM) }

    // Predefined Missions List
    val missionsList = remember {
        listOf(
            Mission(
                id = "fuel_rush",
                title = "1. Fuel Rush",
                description = "City pipeline is DRY! Starting on 25% fuel. Reach Gas Station 2 at z = 800m before running empty.",
                reward = 150,
                targetDistance = 800f,
                initialFuel = 0.25f,
                timeLimitSec = -1, // No time limit
                driveMode = DrivingMode.RACE
            ),
            Mission(
                id = "red_light_respect",
                title = "2. City Traffic Rules",
                description = "Obey municipal codes. Drive to boundary z = 1000m. You must stop nicely at 2 RED LIGHTS! Any signal infractions fail.",
                reward = 250,
                targetDistance = 1000f,
                maxCollisions = 2,
                driveMode = DrivingMode.RACE
            ),
            Mission(
                id = "speed_attack",
                title = "3. Yuvraj Speed Champion",
                description = "Time trial! Blast through the city highway. Reach the end line z = 1500m in under 40 seconds!",
                reward = 400,
                targetDistance = 1500f,
                timeLimitSec = 40,
                driveMode = DrivingMode.RACE
            ),
            Mission(
                id = "peaceful_weaver",
                title = "4. Perfect Escort",
                description = "Zero damage run. Escort through heavy traffic to z = 1300m. Absolutely 0 collisions allowed!",
                reward = 600,
                targetDistance = 1300f,
                maxCollisions = 0,
                driveMode = DrivingMode.RACE
            )
        )
    }

    var selectedMissionIdx by remember { mutableStateOf(0) }
    val activeMission = missionsList[selectedMissionIdx]

    // Active Play States for passing into the Game Controller
    var endCoinsEarned by remember { mutableStateOf(0) }
    var endFailureReason by remember { mutableStateOf("") }

    // Screen Switching Router
    when (gameModeState) {
        GameMode.MENU -> {
            MainMenuScreen(
                coins = savedCoins,
                selectedCar = activeCar,
                onOpenGarage = { gameModeState = GameMode.GARAGE },
                onSelectMode = { mode ->
                    activeDrivingMode = mode
                    if (mode == DrivingMode.RACE) {
                        // Keep selected mission
                    }
                    gameModeState = GameMode.PLAYING
                },
                missions = missionsList,
                selectedMissionIdx = selectedMissionIdx,
                onSelectMission = { selectedMissionIdx = it }
            )
        }
        GameMode.GARAGE -> {
            GarageScreen(
                coins = savedCoins,
                cars = carCatalog,
                selectedIdx = selectedCarIdx,
                onCarSelected = { selectedCarIdx = it },
                onBack = { gameModeState = GameMode.MENU },
                onBuyCar = { carId, price -> unlockCar(carId, price) }
            )
        }
        GameMode.PLAYING -> {
            GamePlayScreen(
                car = activeCar,
                mode = activeDrivingMode,
                mission = if (activeDrivingMode == DrivingMode.RACE) activeMission else null,
                onGameFinished = { coinsEarned, isSuccess, failMsg ->
                    endCoinsEarned = coinsEarned
                    saveCoins(savedCoins + coinsEarned)
                    if (isSuccess) {
                        gameModeState = GameMode.MISSION_SUCCESS
                    } else {
                        endFailureReason = failMsg
                        gameModeState = GameMode.GAME_OVER
                    }
                },
                onExit = { gameModeState = GameMode.MENU }
            )
        }
        GameMode.GAME_OVER -> {
            GameOverScreen(
                failedReason = endFailureReason,
                coinsEarned = endCoinsEarned,
                onRetry = { gameModeState = GameMode.PLAYING },
                onExit = { gameModeState = GameMode.MENU }
            )
        }
        GameMode.MISSION_SUCCESS -> {
            MissionSuccessScreen(
                coinsEarned = endCoinsEarned,
                missionTitle = if (activeDrivingMode == DrivingMode.RACE) activeMission.title else "Free World Exploration",
                onContinue = { gameModeState = GameMode.MENU }
            )
        }
    }
}

// ==========================================
// SCREEN 1: MAIN MENU (LANDSCAPE GRAPHICS)
// ==========================================
@Composable
fun MainMenuScreen(
    coins: Int,
    selectedCar: CarModel,
    onOpenGarage: () -> Unit,
    onSelectMode: (DrivingMode) -> Unit,
    missions: List<Mission>,
    selectedMissionIdx: Int,
    onSelectMission: (Int) -> Unit
) {
    // Rotation animation for stylish decorative background element
    val infiniteTransition = rememberInfiniteTransition(label = "menu_spin")
    val ambientAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin"
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
                    center = Offset(700f, 300f),
                    radius = 1200f
                )
            )
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Branding and Dev details
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1.1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "DR. YUVRAJ DRIVE 3D",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    letterSpacing = 1.sp,
                    color = Color(0xFFFBBF24) // Gold
                )
            )
            Text(
                text = "DEVELOPED BY YUVRAJ CHAUDHARY",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 3.sp,
                    color = Color(0xFF94A3B8)
                ),
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Coins display
            Box(
                modifier = Modifier
                    .background(Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .width(180.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Gold Coin Indicator
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(Color(0xFFFFB703), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "$coins COINS",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action: Car Garage & Start
            Row {
                Button(
                    onClick = onOpenGarage,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x22FFFFFF),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(24.dp))
                ) {
                    Text("CAR GARAGE / PURCHASE", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { onSelectMode(DrivingMode.FREE_ROAM) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x2210B981),
                        contentColor = Color(0xFF34D399)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .border(1.dp, Color(0x6610B981), RoundedCornerShape(24.dp))
                ) {
                    Text("FREE CITY", fontWeight = FontWeight.Black)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Equipped Car: ",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )
                Text(
                    text = selectedCar.name,
                    color = selectedCar.baseColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Right Column: Gorgeous Mission Selection Layout
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.9f)
                .background(Color(0x1BFFFFFF), RoundedCornerShape(24.dp))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "CAREER MISSIONS",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            )

            // Mission tabs selector
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                missions.forEachIndexed { index, mission ->
                    val isSelected = selectedMissionIdx == index
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) Color(0x3DFFFFFF) else Color(0x11FFFFFF),
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) Color(0x66FFFFFF) else Color(0x1AFFFFFF),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelectMission(index) }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = mission.title,
                                color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Reward: +${mission.reward} coins",
                                color = Color(0xFFFFB703),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color(0xFFFFB703), CircleShape)
                            )
                        }
                    }
                }
            }

            // Mission detail description & Play button
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = missions[selectedMissionIdx].description,
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.height(48.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onSelectMode(DrivingMode.RACE) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x3DFE3C3C),
                        contentColor = Color(0xFFFCA5A5)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0x73FE3C3C), RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("LAUNCH MISSION ARCADES", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 2: GARAGE & VEHICLE UNLOCKS
// ==========================================
@Composable
fun GarageScreen(
    coins: Int,
    cars: List<CarModel>,
    selectedIdx: Int,
    onCarSelected: (Int) -> Unit,
    onBack: () -> Unit,
    onBuyCar: (String, Int) -> Unit
) {
    val selectedCar = cars[selectedIdx]
    
    // Rotating preview model angle
    var rotationAngle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            rotationAngle += 0.5f
            if (rotationAngle >= 360f) rotationAngle = 0f
            delay(16)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        // Left Column: Shop/Buy info and Rotating 3D Canvas
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1.2f)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x22FFFFFF),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(20.dp))
                ) {
                    Text("BACK", color = Color.White)
                }

                // Balance (Frosted Badge)
                Box(
                    modifier = Modifier
                        .background(Color(0x1BFFFFFF), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).background(Color(0xFFFFB703), CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$coins COINS",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Beautiful 3D Rotating Canvas representation inside the garage! (Frosted glass pedestal viewport)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp)
                    .background(Color(0x11FFFFFF), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                // We render a standard compound 3D mesh inside this viewport using the selected car's details rotated!
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Draw grid/podium floor lines
                    drawCircle(Color(0x22FFFFFF), radius = h * 0.45f, center = Offset(w/2f, h/2f + 20f), style = Stroke(width = 3f))
                    drawCircle(Color(0x11FFFFFF), radius = h * 0.25f, center = Offset(w/2f, h/2f + 20f), style = Stroke(width = 1.5f))

                    // 3D Camera at isometric angle looking down at origin (0f, 0.4f, 0f)
                    val camY = 1.3f
                    val camZ = -4.5f
                    val yaw = Math.toRadians((rotationAngle).toDouble()).toFloat()
                    val pitch = -0.15f
                    
                    // Generate local vehicle model centered at 000
                    val vehicleFaces = Engine3D.createVehicleMesh(0f, 0f, 0f, yaw, selectedCar.baseColor, selectedCar.id)
                    
                    // Project faces
                    val projected = vehicleFaces.mapNotNull { face ->
                        val projPoints = face.vertices.map { v ->
                            Engine3D.projectPoint(v, 0f, camY, camZ, 0f, pitch, w, h, fovDeg = 45f)
                        }
                        if (projPoints.any { it == null }) return@mapNotNull null
                        
                        ProjectedFace(
                            points = projPoints.map { Offset(it!!.x, it.y) },
                            color = face.color,
                            showOutline = face.showOutline,
                            outlineColor = face.outlineColor,
                            avgZ = projPoints.map { it!!.z }.average().toFloat()
                        )
                    }.sortedByDescending { it.avgZ }

                    // Draw
                    for (pf in projected) {
                        val path = Path().apply {
                            moveTo(pf.points[0].x, pf.points[0].y)
                            for (i in 1 until pf.points.size) {
                                lineTo(pf.points[i].x, pf.points[i].y)
                            }
                            close()
                        }
                        drawPath(path, pf.color)
                        if (pf.showOutline) {
                            drawPath(path, pf.outlineColor, style = Stroke(width = 1.5f))
                        }
                    }
                }
                
                // Rotation helper hint
                Text(
                    text = "DRAG TO ROTATE",
                    color = Color(0x66FFFFFF),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
                )
            }

            // Specs Title card
            Column {
                Text(text = selectedCar.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = selectedCar.description,
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Right Column: Shop list item cells and specifications (Frosted Glass Container)
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
                .background(Color(0x1BFFFFFF), RoundedCornerShape(24.dp))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Stats Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("VEHICLE SPECS", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(8.dp))

                // Stat Row: Max Speed
                SpecStatRow("Max Speed", "${(selectedCar.maxSpeed * 100).toInt()} km/h", selectedCar.maxSpeed / 1.7f)
                // Stat Row: Acceleration
                SpecStatRow("Acceleration", "${(selectedCar.acceleration * 10).toInt()}/10", selectedCar.acceleration / 1.5f)
                // Stat Row: Handling
                SpecStatRow("Steer Grip", "${(selectedCar.handling * 10).toInt()}/10", selectedCar.handling / 1.5f)
                // Stat Row: Fuel Tank
                SpecStatRow("Fuel Tank", "${selectedCar.fuelCapacity.toInt()}L", selectedCar.fuelCapacity / 180f)
            }

            // Catalog Cells
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                cars.forEachIndexed { idx, car ->
                    val isSelectedCell = idx == selectedIdx
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelectedCell) Color(0x3DFFFFFF) else Color(0x11FFFFFF),
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelectedCell) car.baseColor else Color(0x1AFFFFFF),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onCarSelected(idx) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(car.baseColor, CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = car.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        
                        if (car.unlocked) {
                            Text("OWNED", color = Color(0xFF34D399), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Text("${car.price} $", color = Color(0xFFFFB703), fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }

            // BUY BUTTON / EQUIP ACTION (Frosted glass primary triggers)
            val canEquip = selectedCar.unlocked
            val affordable = coins >= selectedCar.price

            Button(
                onClick = {
                    if (canEquip) {
                        onBack()
                    } else if (affordable) {
                        onBuyCar(selectedCar.id, selectedCar.price)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (canEquip) Color(0x3310B981) else if (affordable) Color(0x33DC2626) else Color(0x11FFFFFF),
                        RoundedCornerShape(24.dp)
                    )
                    .border(
                        1.dp,
                        if (canEquip) Color(0x6610B981) else if (affordable) Color(0x66DC2626) else Color(0x22FFFFFF),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                if (canEquip) {
                    Text("EQUIP VEHICLE", fontWeight = FontWeight.Black, color = Color.White)
                } else if (affordable) {
                    Text("BUY CAR FOR ${selectedCar.price} COINS", fontWeight = FontWeight.Black, color = Color.White)
                } else {
                    Text("INSUFFICIENT COINS", fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

@Composable
fun SpecStatRow(label: String, valStr: String, progress: Float) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color(0xFF94A3B8), fontSize = 11.sp)
            Text(valStr, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(4.dp),
            color = Color(0xFFFBBF24),
            trackColor = Color(0x33FFFFFF),
            strokeCap = StrokeCap.Round
        )
    }
}

// ==========================================
// SCREEN 3: GAMEPLAY ENGINE & RENDERING CANVA
// ==========================================
@Composable
fun GamePlayScreen(
    car: CarModel,
    mode: DrivingMode,
    mission: Mission?,
    onGameFinished: (coinsEarned: Int, isSuccess: Boolean, failMsg: String) -> Unit,
    onExit: () -> Unit
) {
    // 3D Camera coordinates
    var camX by remember { mutableFloatStateOf(0f) }
    var camY by remember { mutableFloatStateOf(3.2f) }
    var camZ by remember { mutableFloatStateOf(-12f) }
    var camYaw by remember { mutableFloatStateOf(0f) }

    // Player instance
    var playerX by remember { mutableFloatStateOf(2f) } // Spawn in right lane
    var playerY by remember { mutableFloatStateOf(0f) }
    var playerZ by remember { mutableFloatStateOf(0f) }
    var playerYaw by remember { mutableFloatStateOf(0f) }
    var playerSpeed by remember { mutableFloatStateOf(0f) }

    // Fuel and health tracking
    var fuel by remember { mutableFloatStateOf(100f) }
    var maxFuel = car.fuelCapacity
    var carHealth by remember { mutableFloatStateOf(100f) }

    // Scoring & Stats
    var currentCoinsCollected by remember { mutableIntStateOf(0) }
    var coinsByDrivingDistance by remember { mutableIntStateOf(0) }
    val maxZReached = remember { mutableIntStateOf(0) }
    var lightsStoppedCount by remember { mutableIntStateOf(0) }
    var redLightViolations by remember { mutableIntStateOf(0) }
    var raceTimerRemaining by remember { mutableIntStateOf(mission?.timeLimitSec ?: -1) }
    var showInfractionAlert by remember { mutableStateOf("") }
    var alertTimer by remember { mutableIntStateOf(0) }
    
    // UI control states
    var isAccelerating by remember { mutableStateOf(false) }
    var isBraking by remember { mutableStateOf(false) }
    var steeringSliderVal by remember { mutableFloatStateOf(0f) } // -1f to 1f
    var isDrifting by remember { mutableStateOf(false) }
    var isRefuelingInProgress by remember { mutableStateOf(false) }

    // Init mission values
    LaunchedEffect(mission) {
        if (mission != null) {
            fuel = maxFuel * mission.initialFuel
        } else {
            fuel = maxFuel
        }
    }

    // Traffic light controllers
    val trafficLights = remember {
        listOf(
            TrafficLightIntersection(id = 1, z = 220f),
            TrafficLightIntersection(id = 2, z = 580f),
            TrafficLightIntersection(id = 3, z = 1000f),
            TrafficLightIntersection(id = 4, z = 1420f),
            TrafficLightIntersection(id = 5, z = 1850f)
        )
    }

    // Gas Station static objects
    val gasStations = remember {
        listOf(
            GasStation("station1", x = 11.5f, y = 0f, z = 60f, "Gas Station 1", onRight = true),
            GasStation("station2", x = -11.5f, y = 0f, z = 800f, "Gas Station 2", onRight = false),
            GasStation("station3", x = 11.5f, y = 0f, z = 1550f, "Highway Refuel Core", onRight = true)
        )
    }

    // Ground scenery generators
    val blocksList = remember {
        val list = mutableListOf<Triple<Float, Float, Float>>() // x, z, height
        for (z in -400..2500 step 150) {
            // Make sure buildings don't block intersections
            var blockIntersection = false
            for (iz in listOf(220, 580, 1000, 1420, 1850)) {
                if (abs(z - iz) < 50) {
                    blockIntersection = true
                    break
                }
            }
            if (!blockIntersection) {
                list.add(Triple(-22f, z.toFloat(), 15f + (z % 35).toFloat()))
                list.add(Triple(-38f, z.toFloat() + 40f, 25f + (z % 55).toFloat()))
                list.add(Triple(22f, z.toFloat() + 20f, 12f + (z % 45).toFloat()))
                list.add(Triple(38f, z.toFloat() - 25f, 32f + (z % 65).toFloat()))
            }
        }
        list
    }

    val treeCoords = remember {
        val list = mutableListOf<Pair<Float, Float>>() // x, z
        for (z in -400..2500 step 45) {
            var blockIntersection = false
            for (iz in listOf(220, 580, 1000, 1420, 1850)) {
                if (abs(z - iz) < 35) blockIntersection = true
            }
            if (!blockIntersection) {
                list.add(Pair(-10.5f, z.toFloat()))
                list.add(Pair(10.5f, z.toFloat() + 15f))
            }
        }
        list
    }

    // Spinning coin packs
    val coinInstances = remember {
        val list = mutableListOf<Vector3D>()
        for (z in 120..2400 step 180) {
            list.add(Vector3D(-4f, 0.4f, z.toFloat()))
            list.add(Vector3D(4f, 0.4f, z.toFloat() + 40f))
        }
        // Spawn inside intersections
        for (iz in listOf(220, 580, 1000, 1420, 1850)) {
            list.add(Vector3D(-34f, 0.4f, iz.toFloat()))
            list.add(Vector3D(34f, 0.4f, iz.toFloat()))
        }
        list.toMutableStateList()
    }

    // Active traffic vehicles list
    val trafficCars = remember {
        val list = mutableListOf<GameVehicle>()
        // Spawn 8 initial traffic vehicles
        for (i in 0 until 8) {
            val lane = Random.nextInt(4) // 4 lanes
            val laneX = when (lane) {
                0 -> -5.5f // Southbound outer
                1 -> -1.8f // Southbound inner
                2 -> 1.8f  // Northbound inner
                else -> 5.5f // Northbound outer
            }
            list.add(
                GameVehicle(
                    id = if (i % 3 == 0) "taxi" else if (i % 4 == 0) "police" else "sedan",
                    x = laneX,
                    y = 0f,
                    z = 80f + i * 160f,
                    yaw = if (lane < 2) Math.PI.toFloat() else 0f, // heading
                    speed = 0.35f + Random.nextFloat() * 0.15f,
                    maxSpeed = 0.5f,
                    color = when (i % 3) {
                        0 -> Color(0xFFDC2626) // crimson
                        1 -> Color(0xFFFBBF24) // gold
                        else -> Color(0xFFFFFFFF)
                    },
                    isTraffic = true,
                    targetLane = lane
                )
            )
        }
        list.toMutableStateList()
    }

    // Timer and Physics Game Loop ticks
    var tickCount by remember { mutableIntStateOf(0) }
    var gameCompletedTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        var lastTime = System.currentTimeMillis()
        var secTimerAccumulator = 0f

        while (!gameCompletedTriggered) {
            tickCount++
            val now = System.currentTimeMillis()
            val dt = ((now - lastTime) / 1000f).coerceIn(0.005f, 0.08f)
            lastTime = now

            // Update countdown for missions
            if (mission != null) {
                secTimerAccumulator += dt
                if (secTimerAccumulator >= 1.0f) {
                    secTimerAccumulator = 0f
                    if (raceTimerRemaining > 0) {
                        raceTimerRemaining--
                    } else if (mission.timeLimitSec > 0) {
                        // Ran out of time
                        gameCompletedTriggered = true
                        onGameFinished(currentCoinsCollected, false, "Time limit exceeded! You failed to finish within 40s.")
                        break
                    }
                }
            }

            // Update traffic lights
            for (light in trafficLights) {
                light.update()
            }

            // ==========================================
            // PLAYER CAR PHYSICS
            // ==========================================
            val maxSpeedMult = car.maxSpeed * 0.9f
            val accelRate = car.acceleration * 1.5f * dt
            val dragCoefficient = 0.08f * dt

            // Accelerate / Brake input response
            if (isAccelerating) {
                if (fuel > 0f) {
                    playerSpeed = (playerSpeed + accelRate).coerceAtMost(maxSpeedMult)
                    fuel = (fuel - dt * 1.5f).coerceAtLeast(0f)
                } else {
                    playerSpeed = (playerSpeed - dragCoefficient).coerceAtLeast(0f)
                }
            } else if (isBraking) {
                playerSpeed = (playerSpeed - accelRate * 2f).coerceAtLeast(-maxSpeedMult * 0.3f) // reverse
            } else {
                // Free coasting deceleration
                if (playerSpeed > 0f) {
                    playerSpeed = (playerSpeed - dragCoefficient).coerceAtLeast(0f)
                } else if (playerSpeed < 0f) {
                    playerSpeed = (playerSpeed + dragCoefficient).coerceAtMost(0f)
                }
            }

            // Steering calculation
            val steeringResponseMult = car.handling * if (isDrifting) 1.8f else 1.0f
            playerYaw = (steeringSliderVal * 0.35f * steeringResponseMult).coerceIn(-0.6f, 0.6f)

            // Update coordinates based on Heading:
            // Forward is along look-vector:
            val speedDelta = playerSpeed * dt * 110f
            playerX += speedDelta * sin(playerYaw)
            playerZ += speedDelta * cos(playerYaw)

            // Auto center wheel if player not touching steer slider
            // Done programmatically in the steer handle block

            // Bounding collision checks with barriers
            if (abs(playerX) >= 11.2f) {
                // Off track bounding block
                playerX = playerX.sign * 11.2f
                playerSpeed = -playerSpeed * 0.3f // bounce back
                carHealth = (carHealth - dt * 25f).coerceAtLeast(0f)
                showInfractionAlert = "SIDE BARRIER CRASH!"
                alertTimer = 60
            }

            // ==========================================
            // COIN AND REFUEL TRIGGERS
            // ==========================================
            // Spinning coin collision
            val iterator = coinInstances.iterator()
            while (iterator.hasNext()) {
                val c = iterator.next()
                val dist = sqrt((playerX - c.x).pow(2) + (playerZ - c.z).pow(2))
                if (dist < 3.2f) {
                    currentCoinsCollected += 10
                    iterator.remove()
                    showInfractionAlert = "+10 COINS: BONUS DRIFT"
                    alertTimer = 60
                }
            }

            // Gas Station replenishment zone checks
            var refuelingNow = false
            for (st in gasStations) {
                val dx = abs(playerX - st.x)
                val dz = abs(playerZ - st.z)
                if (dx < 5.5f && dz < 5.5f && playerSpeed < 0.15f) {
                    refuelingNow = true
                    if (fuel < maxFuel) {
                        fuel = (fuel + maxFuel * 0.15f * dt).coerceAtMost(maxFuel)
                        if (currentCoinsCollected > 0 && tickCount % 50 == 0) {
                            currentCoinsCollected = (currentCoinsCollected - 1).coerceAtLeast(0)
                        }
                    }
                }
            }
            isRefuelingInProgress = refuelingNow

            // Distancial distance coin updates
            val currentDistanceInInt = playerZ.toInt()
            if (currentDistanceInInt > maxZReached.value) {
                val extraMeters = currentDistanceInInt - maxZReached.value
                maxZReached.value = currentDistanceInInt
                // 1 coin per 20 meters driven free roam
                if (tickCount % 20 == 0) {
                    coinsByDrivingDistance += 1
                }
            }

            // ==========================================
            // RED LIGHT & INTERSECTION RULE COMPLIANCE
            // ==========================================
            for (il in trafficLights) {
                val relZ = playerZ - il.z
                // Stopping line is located at intersectionZ - 12f to intersectionZ - 5f
                val inStopZone = playerZ > il.z - 15f && playerZ < il.z - 5f
                
                // If light is red, and player complies by stopping
                if (il.state == LightState.RED && inStopZone && playerSpeed < 0.02f) {
                    // Reward for stopping nicely
                    if (tickCount % 80 == 0) {
                        currentCoinsCollected += 5
                        lightsStoppedCount++
                        showInfractionAlert = "+5 BONUS: LAW ABIDING!"
                        alertTimer = 80
                    }
                }

                // If player flies past intersection center coordinate when Signal is RED
                val pastStopLineAndCrossingCenter = playerZ >= il.z - 5f && playerZ <= il.z + 5f
                if (il.state == LightState.RED && pastStopLineAndCrossingCenter) {
                    // Violation! Ensure penalty registers once
                    if (tickCount % 50 == 0) {
                        redLightViolations++
                        currentCoinsCollected = (currentCoinsCollected - 30).coerceAtLeast(0)
                        showInfractionAlert = "-30 COINS: RED LIGHT CITATION!"
                        alertTimer = 100
                        if (mission?.id == "red_light_respect") {
                            // Instant fail
                            gameCompletedTriggered = true
                            onGameFinished(currentCoinsCollected, false, "Committed municipal red-light violation. Mission aborted.")
                            break
                        }
                    }
                }
            }

            // ==========================================
            // TRAFFIC CYCLE & MOVEMENT PHYSICS
            // ==========================================
            for (tc in trafficCars) {
                // Lane index speed vectors
                val isSouthbound = tc.targetLane < 2
                val currentMoveDelta = tc.speed * dt * 90f

                // Intersection stop logic for robots
                var nextIntState = LightState.GREEN
                var activeStopZ = -1f
                for (light in trafficLights) {
                    if (isSouthbound && tc.z > light.z + 10f) {
                        nextIntState = light.state
                        activeStopZ = light.z + 14f
                        break
                    } else if (!isSouthbound && tc.z < light.z - 10f) {
                        nextIntState = light.state
                        activeStopZ = light.z - 14f
                        break
                    }
                }

                val robotDetectsRed = nextIntState == LightState.RED && activeStopZ != -1f
                val distToStop = abs(tc.z - activeStopZ)

                val shouldStopAtSignal = robotDetectsRed && distToStop < 15f

                // Avoid piling into other robot ahead
                var robotTooCloseAhead = false
                for (tcAhead in trafficCars) {
                    if (tcAhead === tc) continue
                    if (tcAhead.targetLane == tc.targetLane) {
                        val aheadDistance = if (!isSouthbound) tcAhead.z - tc.z else tc.z - tcAhead.z
                        if (aheadDistance in 0.1f..15.0f) {
                            robotTooCloseAhead = true
                        }
                    }
                }

                if (shouldStopAtSignal || robotTooCloseAhead) {
                    // Friction slow down
                    tc.speed = (tc.speed - dt * 3.5f).coerceAtLeast(0f)
                    tc.brakeLight = true
                } else {
                    // Normal accelerate to average limit
                    tc.speed = (tc.speed + dt * 0.9f).coerceAtMost(tc.maxSpeed)
                    tc.brakeLight = false
                }

                // Update axis position
                if (isSouthbound) {
                    tc.z -= currentMoveDelta
                } else {
                    tc.z += currentMoveDelta
                }

                // Push traffic cycle ahead if player passed them
                val distanceBehindPlayer = playerZ - tc.z
                if (distanceBehindPlayer > 120f) {
                    tc.z = playerZ + 140f + Random.nextFloat() * 60f
                    val newLane = Random.nextInt(4)
                    tc.targetLane = newLane
                    tc.x = when (newLane) {
                        0 -> -5.5f
                        1 -> -1.8f
                        2 -> 1.8f
                        else -> 5.5f
                    }
                    tc.yaw = if (newLane < 2) Math.PI.toFloat() else 0f
                }
            }

            // ==========================================
            // PLAYER-TO-TRAFFIC COLLISION DISPLACEMENT
            // ==========================================
            for (tc in trafficCars) {
                val dx = abs(playerX - tc.x)
                val dz = abs(playerZ - tc.z)
                if (dx < 2.0f && dz < 4.0f) {
                    // Extreme crash!
                    playerSpeed = -playerSpeed * 0.4f // bounce player
                    tc.speed = playerSpeed + 0.3f    // push robot
                    carHealth = (carHealth - 25f).coerceAtLeast(0f)
                    currentCoinsCollected = (currentCoinsCollected - 10).coerceAtLeast(0)
                    showInfractionAlert = "COLLISION: DRIVING INFRACTION!"
                    alertTimer = 60

                    if (mission?.id == "peaceful_weaver") {
                        // Instant Fail
                        gameCompletedTriggered = true
                        onGameFinished(currentCoinsCollected, false, "Scraped a civilian traffic vehicle. Clean escort failed.")
                        break
                    }
                }
            }

            // Camera chasing lerp update
            val desireCamX = playerX - 11f * sin(playerYaw)
            val desireCamZ = playerZ - 11f * cos(playerYaw)
            camX += (desireCamX - camX) * 0.18f
            camY = playerY + 3.4f
            camZ += (desireCamZ - camZ) * 0.18f
            camYaw = playerYaw

            // Alert banner timer cycle
            if (alertTimer > 0) {
                alertTimer--
            } else {
                showInfractionAlert = ""
            }

            // ==========================================
            // WIN CONDITIONS CHECKS
            // ==========================================
            if (carHealth <= 0f) {
                gameCompletedTriggered = true
                onGameFinished(currentCoinsCollected, false, "Your vehicle sustained critical structural damage and broke down.")
                break
            }

            // Low Fuel Check
            if (fuel <= 0f && playerSpeed < 0.01f) {
                gameCompletedTriggered = true
                onGameFinished(currentCoinsCollected, false, "Your engine sputtered and died. You ran completely out of fuel!")
                break
            }

            // Target Milestone Reach Success
            if (mission != null && playerZ >= mission.targetDistance) {
                gameCompletedTriggered = true
                val finalEarnings = currentCoinsCollected + mission.reward
                onGameFinished(finalEarnings, true, "")
                break
            }

            delay(16) // tick lock
        }
    }

    // Centering steer slider value automatically on frame changes if cursor left
    LaunchedEffect(steeringSliderVal) {
        if (steeringSliderVal != 0f) {
            // Drag decays slowly to make arcade self-centering steering
            delay(10)
            steeringSliderVal = (steeringSliderVal * 0.82f)
            if (abs(steeringSliderVal) < 0.05f) {
                steeringSliderVal = 00f
            }
        }
    }

    // MAIN COCKPIT VIEWPORT ROW
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // BACKGROUND 3D RENDERING LAYER
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            // Touch trigger
                        }
                    )
                }
        ) {
            val width = size.width
            val height = size.height

            // 1. Sky Rendering (High-fidelity beautiful dusk sky gradient)
            val skyGradient = Brush.verticalGradient(
                colors = listOf(Color(0xFF0369A1), Color(0xFF0F172A)),
                startY = 0f,
                endY = height * 0.55f
            )
            drawRect(skyGradient, size = Size(width, height * 0.55f))

            // Horizon accent divider
            drawRect(Color(0xFF1E1B4B), topLeft = Offset(0f, height * 0.53f), size = Size(width, height * 0.02f))

            // 2. Off-track Ground Terrain (Dark forest emerald)
            drawRect(Color(0xFF064E3B), topLeft = Offset(0f, height * 0.55f), size = Size(width, height * 0.45f))

            // 3. Collect ALL 3D meshes to draw
            val active3DFaces = mutableListOf<Face3D>()

            // A. Roads geometry: Central Boulevard (z range from -400 to 2400)
            // Left boundary shoulder
            active3DFaces.addAll(Engine3D.createCubeMesh(0f, -0.05f, 1000f, 22.4f, 0.01f, 3000f, Color(0xFF334155), forceDrawFirst = true))
            // Central main road
            active3DFaces.addAll(Engine3D.createCubeMesh(0f, -0.01f, 1000f, 14.0f, 0.01f, 3000f, Color(0xFF1E293B), forceDrawFirst = true))

            // Intersecting road cuts
            for (iz in listOf(220f, 580f, 1000f, 1420f, 1850f)) {
                // Crossing lanes asphalt pad
                active3DFaces.addAll(Engine3D.createCubeMesh(0f, 0.01f, iz, 100f, 0.02f, 16f, Color(0xFF1E293B)))
                // Crosswalk white lines overlay
                active3DFaces.addAll(Engine3D.createCubeMesh(0f, 0.02f, iz - 10f, 14f, 0.01f, 0.8f, Color.White))
                active3DFaces.addAll(Engine3D.createCubeMesh(0f, 0.02f, iz + 10f, 14f, 0.01f, 0.8f, Color.White))
            }

            // B. Road central dividers (Dashed white lines for 4 lanes)
            for (z in -200..2300 step 45) {
                active3DFaces.addAll(Engine3D.createCubeMesh(-3.7f, 0.02f, z.toFloat(), 0.12f, 0.01f, 6f, Color.White))
                active3DFaces.addAll(Engine3D.createCubeMesh(3.7f, 0.02f, z.toFloat() + 15f, 0.12f, 0.01f, 6f, Color.White))
                // Central yellow double line
                active3DFaces.addAll(Engine3D.createCubeMesh(-0.1f, 0.02f, z.toFloat() + 25f, 0.08f, 0.01f, 10f, Color(0xFFEAB308)))
                active3DFaces.addAll(Engine3D.createCubeMesh(0.1f, 0.02f, z.toFloat() + 25f, 0.08f, 0.01f, 10f, Color(0xFFEAB308)))
            }

            // C. Populating scenery (Skyscrapers)
            for (bk in blocksList) {
                // Filter out buildings that are way behind player to make it performant
                if (abs(bk.second - playerZ) > 350f) continue
                active3DFaces.addAll(
                    Engine3D.createSkyscraperMesh(
                        bk.first, 0f, bk.second,
                        width = 12f, height = bk.third, length = 12f,
                        baseColor = when ((bk.second.toInt() / 150) % 4) {
                            0 -> Color(0xFF334155) // Slate
                            1 -> Color(0xFF1E3A8A) // deep blue
                            2 -> Color(0xFF581C87) // purple
                            else -> Color(0xFF1F2937)
                        }
                    )
                )
            }

            // D. Populating Trees
            for (tr in treeCoords) {
                if (abs(tr.second - playerZ) > 240f) continue
                active3DFaces.addAll(Engine3D.createTreeMesh(tr.first, 0f, tr.second, scale = 1.0f + (tr.second % 0.4f)))
            }

            // E. Solid coins spinning
            val coinRotation = tickCount * 0.12f
            for (c in coinInstances) {
                if (abs(c.z - playerZ) > 180f) continue
                active3DFaces.addAll(
                    Engine3D.createCubeMesh(
                        c.x, c.y, c.z,
                        0.7f, 0.7f, 0.15f,
                        Color(0xFFEAB308),
                        showOutline = true,
                        outlineColor = Color.White,
                        isLightSensitive = false,
                        yaw = coinRotation
                    )
                )
            }

            // F. Rendering Traffic signal poles
            for (light in trafficLights) {
                if (abs(light.z - playerZ) > 300f) continue
                // Left shoulder pole
                active3DFaces.addAll(Engine3D.createTrafficLightMesh(-8.8f, 0f, light.z, light.state))
                // Right shoulder pole
                active3DFaces.addAll(Engine3D.createTrafficLightMesh(8.8f, 0f, light.z, light.state))
            }

            // G. Rendering Gas Stations
            for (st in gasStations) {
                if (abs(st.z - playerZ) > 280f) continue
                active3DFaces.addAll(Engine3D.createGasStationMesh(st.x, st.y, st.z, timeScale = tickCount.toFloat()))
            }

            // H. Civilian Traffic Vehicles
            for (tc in trafficCars) {
                if (abs(tc.z - playerZ) > 280f) continue
                active3DFaces.addAll(Engine3D.createVehicleMesh(tc.x, tc.y, tc.z, tc.yaw, tc.color, tc.id))
            }

            // I. Player Car (Chased by camera)
            // Draw player car slightly ahead relative to cam coordinate (visible on reverse or drifts)
            active3DFaces.addAll(Engine3D.createVehicleMesh(playerX, playerY, playerZ, playerYaw, car.baseColor, car.id))

            // ==========================================
            // PREFORM CAMERA PROJECTIONS & PAINTERS SORT
            // ==========================================
            val finalProjectedFaces = active3DFaces.mapNotNull { face ->
                // Project all points of the face
                val projPoints = face.vertices.map { v ->
                    Engine3D.projectPoint(v, camX, camY, camZ, camYaw, -0.15f, width, height, fovDeg = 65f)
                }

                // Discard faces if entirely clipped behind the camera near plane
                if (projPoints.all { it == null } || projPoints.any { it == null }) return@mapNotNull null

                // Compute depth weight
                val zAvg = projPoints.map { it!!.z }.average().toFloat()

                ProjectedFace(
                    points = projPoints.map { Offset(it!!.x, it.y) },
                    color = face.color,
                    showOutline = face.showOutline,
                    outlineColor = face.outlineColor,
                    avgZ = zAvg
                )
            }.sortedByDescending { it.avgZ } // Painter's sorting back-to-front

            // Draw Paths on canvas
            for (pf in finalProjectedFaces) {
                val path = Path().apply {
                    moveTo(pf.points[0].x, pf.points[0].y)
                    for (i in 1 until pf.points.size) {
                        lineTo(pf.points[i].x, pf.points[i].y)
                    }
                    close()
                }

                // Render shaded face solid
                drawPath(path, pf.color, style = Fill)
                
                // Extra outline for high contrast low-poly edge tracing
                if (pf.showOutline) {
                    drawPath(path, pf.outlineColor, style = Stroke(width = 1.0f))
                }
            }
        }

        // ==========================================
        // FLOATING WARNING / CITATION INFRACTION ALERTS
        // ==========================================
        if (showInfractionAlert.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 45.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (showInfractionAlert.startsWith("+")) Color(0xDD047857) else Color(0xDDEC1D24),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = showInfractionAlert,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // ==========================================
        // TOP DECK HUD: CONTEXT OVERLAYS
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // L1: Top-Left Card - Mission detail progress or mode indicator (Frosted Glass)
            Column(
                modifier = Modifier
                    .background(Color(0x1BFFFFFF), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                    .padding(12.dp)
                    .width(220.dp)
            ) {
                if (mission != null) {
                    Text(text = "MISSION: " + mission.title.uppercase(), color = Color(0xFFFFB703), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Goal: Reach ${mission.targetDistance.toInt()}m",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    val distancePct = (playerZ / mission.targetDistance).coerceIn(0f, 1f)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Text("${playerZ.toInt()}m / ${mission.targetDistance.toInt()}m", color = Color(0xFFCBD5E1), fontSize = 9.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        LinearProgressIndicator(
                            progress = { distancePct },
                            color = Color(0xFF34D399),
                            trackColor = Color(0x33FFFFFF),
                            modifier = Modifier.weight(1f).height(4.dp)
                        )
                    }
                    
                    // Extra metrics: remaining timer, or rules infractions count
                    if (mission.timeLimitSec > 0) {
                        Text(
                            text = "TIMER REMAINS: $raceTimerRemaining s",
                            color = if (raceTimerRemaining < 10) Color(0xFFEF4444) else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                } else {
                    Text("DRIVING MODE: FREE WORLD", color = Color(0xFF34D399), fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text("Meters Driven: ${playerZ.toInt()}m", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Red Signals Met: $lightsStoppedCount stopped", color = Color(0xFF94A3B8), fontSize = 10.sp)
                }

                // Health Bar
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text("CAR DAMAGE: ", color = Color(0xFFCBD5E1), fontSize = 9.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    LinearProgressIndicator(
                        progress = { carHealth / 100f },
                        color = if (carHealth < 35f) Color(0xFFEF4444) else Color(0xFFFCA5A5),
                        trackColor = Color(0x33FFFFFF),
                        modifier = Modifier.weight(1f).height(4.dp)
                    )
                }
            }

            // L2: Top-Center: Speed and fuel dial (Frosted Glass)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(Color(0x1BFFFFFF), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Show digital display KM/H
                val speedKmh = (abs(playerSpeed) * 115).toInt()
                Text(
                    text = "$speedKmh",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "KM/H",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCBD5E1)
                )

                // FUEL BAR
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                    Text("FUEL: ", color = Color(0xFFFFB703), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(6.dp)
                            .background(Color(0x33FFFFFF), RoundedCornerShape(2.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width((80 * (fuel / maxFuel)).dp)
                                .background(
                                    if (fuel < maxFuel * 0.25f) Color(0xFFFE3C3C) else Color(0xFFFFB703),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                    if (isRefuelingInProgress) {
                        Text(
                            " [RECOVERING+]",
                            fontSize = 8.sp,
                            color = Color(0xFF34D399),
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }

            // L3: Top-Right Card - Coin indicators & Abort (Frosted Glass)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .background(Color(0x1BFFFFFF), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    val earnings = currentCoinsCollected + coinsByDrivingDistance
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFFFFB703), CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "$earnings COINS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Red Infractions: $redLightViolations", color = Color(0xFFF87171), fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onExit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x22DC2626),
                        contentColor = Color(0xFFFCA5A5)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .border(1.dp, Color(0x66DC2626), RoundedCornerShape(16.dp))
                ) {
                    Text("ABORT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ==========================================
        // MAIN COCKPIT INTERACTION FOOTER ROW
        // ==========================================
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            // CONTROL-LEFT: VIRTUAL WHEEL SEGMENT
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(Color(0x330F172A), CircleShape)
                    .border(1.5.dp, Color(0x33FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Interactive wheel
                Canvas(
                    modifier = Modifier
                        .size(126.dp)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    // Change slide steering position
                                    steeringSliderVal = (steeringSliderVal + dragAmount.x * 0.025f).coerceIn(-1.0f, 1.0f)
                                    change.consume()
                                },
                                onDragEnd = {
                                    // auto centering does it in tick update
                                }
                            )
                        }
                ) {
                    val w = size.width
                    val h = size.height

                    // Dynamic wheel rotation offset
                    val drawAngle = steeringSliderVal * 90f

                    drawCircle(Color(0xBD18181B), radius = w / 2f)
                    
                    // Styled circular frame of physical look
                    drawCircle(Color(0xFF475569), radius = w / 2f, style = Stroke(width = 8f))
                    drawCircle(Color(0xFF1E293B), radius = w / 2f - 4f, style = Stroke(width = 2f))

                    // Draw center console cap
                    drawCircle(Color(0xFF1E293B), radius = w * 0.22f)
                    drawCircle(Color(0xFFFBBF24), radius = w * 0.20f, style = Stroke(width = 1.5f))

                    // Dynamic rotated wheel spokes
                    val spokeAngleRad1 = Math.toRadians((0f + drawAngle).toDouble()).toFloat()
                    val spokeAngleRad2 = Math.toRadians((120f + drawAngle).toDouble()).toFloat()
                    val spokeAngleRad3 = Math.toRadians((240f + drawAngle).toDouble()).toFloat()

                    drawLine(
                        Color(0xFF94A3B8),
                        start = Offset(w/2f, h/2f),
                        end = Offset(w/2f + (w/2f - 6f) * cos(spokeAngleRad1), h/2f + (h/2f - 6f) * sin(spokeAngleRad1)),
                        strokeWidth = 6f
                    )
                    drawLine(
                        Color(0xFF94A3B8),
                        start = Offset(w/2f, h/2f),
                        end = Offset(w/2f + (w/2f - 6f) * cos(spokeAngleRad2), h/2f + (h/2f - 6f) * sin(spokeAngleRad2)),
                        strokeWidth = 6f
                    )
                    drawLine(
                        Color(0xFF94A3B8),
                        start = Offset(w/2f, h/2f),
                        end = Offset(w/2f + (w/2f - 6f) * cos(spokeAngleRad3), h/2f + (h/2f - 6f) * sin(spokeAngleRad3)),
                        strokeWidth = 6f
                    )
                }

                // Visual arrows
                Text("< STEER >", color = Color(0x55FFFFFF), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp))
            }

            // MIDDLE CREDITS / DETAILED CORNER MINIMAP (Frosted Glass Badge)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .background(Color(0x1BFFFFFF), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("DEVELOPER CREDIT", color = Color(0x88FFFFFF), fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("Yuvraj Chaudhary", color = Color(0xFFFFB703), fontSize = 11.sp, fontWeight = FontWeight.Black)
            }

            // CONTROL-RIGHT: INDIVIDUAL TOUCH-ACTIVE PEDALS
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // BRAKE / REVERSE PEDAL (Red Frosted glowing theme)
                Box(
                    modifier = Modifier
                        .size(width = 65.dp, height = 95.dp)
                        .background(
                            if (isBraking) Color(0x66EF4444) else Color(0x1AEF4444),
                            RoundedCornerShape(12.dp)
                        )
                        .border(1.5.dp, Color(0xAAEF4444), RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isBraking = true
                                    tryAwaitRelease()
                                    isBraking = false
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("BRAKE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        Text("REVERSE", color = Color(0xFFFCA5A5), fontSize = 8.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // GAS / ACCELERATE PEDAL (Green Frosted glowing theme)
                Box(
                    modifier = Modifier
                        .size(width = 72.dp, height = 110.dp)
                        .background(
                            if (isAccelerating) Color(0x6610B981) else Color(0x1A10B981),
                            RoundedCornerShape(12.dp)
                        )
                        .border(2.dp, Color(0xAA10B981), RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isAccelerating = true
                                    tryAwaitRelease()
                                    isAccelerating = false
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "GAS",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// SCREEN 4: GAME OVER DIALOG VIEW
// ==========================================
@Composable
fun GameOverScreen(
    failedReason: String,
    coinsEarned: Int,
    onRetry: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC0B1120)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .background(Color(0x1BFFFFFF), RoundedCornerShape(24.dp))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                .padding(28.dp)
                .width(420.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CRASH / GAME OVER",
                color = Color(0xFFFCA5A5),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = "DEVELOPED BY YUVRAJ CHAUDHARY",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            Text(
                text = failedReason,
                color = Color.White,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(Color(0xFFFFB703), CircleShape))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Salary Earned: +$coinsEarned Coins",
                    color = Color(0xFFFFB703),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = onExit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x22FFFFFF),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
                ) {
                    Text("BACK TO MENU", color = Color.White)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x33EF4444),
                        contentColor = Color(0xFFFCA5A5)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, Color(0x66EF4444), RoundedCornerShape(20.dp))
                ) {
                    Text("RETRY LEVEL", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// SCREEN 5: MISSION SUCCESS DIALOG VIEW
// ==========================================
@Composable
fun MissionSuccessScreen(
    coinsEarned: Int,
    missionTitle: String,
    onContinue: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC0F172A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .background(Color(0x1BFFFFFF), RoundedCornerShape(24.dp))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                .padding(28.dp)
                .width(420.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "MISSION ACCOMPLISHED!",
                color = Color(0xFF34D399),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )

            Text(
                text = "DEVELOPED BY YUVRAJ CHAUDHARY",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            Text(
                text = "You cleared the requirements for \"$missionTitle\" successfully!",
                color = Color(0xFFCBD5E1),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Prize earnings card (Frosted glass mini-cell)
            Box(
                modifier = Modifier
                    .background(Color(0x22FFFFFF), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(14.dp).background(Color(0xFFFFB703), CircleShape))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "+$coinsEarned COINS REWARD",
                        color = Color(0xFFFFB703),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x3310B981),
                    contentColor = Color(0xFF34D399)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0x6610B981), RoundedCornerShape(24.dp))
            ) {
                Text("COLLECT EARNINGS & BACK TO MENU", color = Color.White, fontWeight = FontWeight.Black)
            }
        }
    }
}
