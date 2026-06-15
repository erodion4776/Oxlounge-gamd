package com.oxlounge

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LoungeAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoungeAppContent(startScreen = AppScreen.SPIN_WHEEL)
                }
            }
        }
    }
}

// Config file name for offline persistence
private const val PREFS_NAME = "OxloungePrefs"
private const val KEY_SEGMENTS = "segments_json"
private const val KEY_HISTORY = "history_json"

@Composable
fun LoungeAppContent(startScreen: AppScreen = AppScreen.SPIN_WHEEL) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // Default prize segmentation configuration
    val defaultSegments = listOf(
        PrizeSegment("1", "Vegas VIP Pass", "#F59E0B", 2, true),
        PrizeSegment("2", "Neon Cocktail", "#A855F7", 4, true),
        PrizeSegment("3", "Better Luck", "#3B82F6", 6, false),
        PrizeSegment("4", "Premium Shisha", "#EC4899", 3, true),
        PrizeSegment("5", "Lounge Couch Access", "#10B981", 2, true),
        PrizeSegment("6", "House Shot", "#8B5CF6", 5, true),
        PrizeSegment("7", "Classic Hookah", "#F43F5E", 3, true),
        PrizeSegment("8", "No Win - Spin Again", "#6B7280", 5, false)
    )

    // State definitions
    var currentScreen by remember { mutableStateOf(startScreen) }
    var segmentsList by remember { mutableStateOf<List<PrizeSegment>>(emptyList()) }
    var winHistory by remember { mutableStateOf<List<WinHistoryItem>>(emptyList()) }
    
    // Loaded status
    var isInitialized by remember { mutableStateOf(false) }

    // Load persistent data
    LaunchedEffect(Unit) {
        if (!isInitialized) {
            val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val segmentsJson = sharedPreferences.getString(KEY_SEGMENTS, null)
            val historyJson = sharedPreferences.getString(KEY_HISTORY, null)

            // Parse list
            segmentsList = if (segmentsJson != null) {
                try {
                    val arr = JSONArray(segmentsJson)
                    val result = mutableListOf<PrizeSegment>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        result.add(
                            PrizeSegment(
                                id = obj.getString("id"),
                                name = obj.getString("name"),
                                hexColor = obj.getString("hexColor"),
                                probabilityWeight = obj.getInt("probabilityWeight"),
                                isWin = obj.getBoolean("isWin"),
                                winCount = obj.optInt("winCount", 0)
                            )
                        )
                    }
                    result
                } catch (e: Exception) {
                    defaultSegments
                }
            } else {
                defaultSegments
            }

            // Parse history list
            winHistory = if (historyJson != null) {
                try {
                    val arr = JSONArray(historyJson)
                    val result = mutableListOf<WinHistoryItem>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        result.add(
                            WinHistoryItem(
                                prizeName = obj.getString("prizeName"),
                                timestamp = obj.getLong("timestamp"),
                                customerCode = obj.getString("customerCode")
                            )
                        )
                    }
                    result
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
            isInitialized = true
        }
    }

    // Save helper functions
    fun saveSegments(updatedList: List<PrizeSegment>) {
        segmentsList = updatedList
        scope.launch {
            val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val arr = JSONArray()
            updatedList.forEach { item ->
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("name", item.name)
                obj.put("hexColor", item.hexColor)
                obj.put("probabilityWeight", item.probabilityWeight)
                obj.put("isWin", item.isWin)
                obj.put("winCount", item.winCount)
                arr.put(obj)
            }
            sharedPreferences.edit().putString(KEY_SEGMENTS, arr.toString()).apply()
        }
    }

    fun saveHistory(updatedHistory: List<WinHistoryItem>) {
        winHistory = updatedHistory
        scope.launch {
            val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val arr = JSONArray()
            updatedHistory.forEach { item ->
                val obj = JSONObject()
                obj.put("prizeName", item.prizeName)
                obj.put("timestamp", item.timestamp)
                obj.put("customerCode", item.customerCode)
                arr.put(obj)
            }
            sharedPreferences.edit().putString(KEY_HISTORY, arr.toString()).apply()
        }
    }

    // Increments offline win tracker on hit
    fun registerWin(wonPrize: PrizeSegment, code: String) {
        val updatedList = segmentsList.map {
            if (it.id == wonPrize.id) it.copy(winCount = it.winCount + 1) else it
        }
        saveSegments(updatedList)

        val updatedHistory = listOf(
            WinHistoryItem(prizeName = wonPrize.name, timestamp = System.currentTimeMillis(), customerCode = code)
        ) + winHistory
        saveHistory(updatedHistory)
    }

    if (!isInitialized) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF131024),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = (currentScreen == AppScreen.SPIN_WHEEL),
                    onClick = { currentScreen = AppScreen.SPIN_WHEEL },
                    icon = { Icon(Icons.Filled.PlayArrow, contentDescription = "Spin Wheel") },
                    label = { Text("Lounge Spin", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFF59E0B),
                        selectedTextColor = Color(0xFFF59E0B),
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray,
                        indicatorColor = Color(0xFF2E2A4E)
                    )
                )

                NavigationBarItem(
                    selected = (currentScreen == AppScreen.SEGMENT_EDIT),
                    onClick = { currentScreen = AppScreen.SEGMENT_EDIT },
                    icon = { Icon(Icons.Filled.Edit, contentDescription = "Prizes") },
                    label = { Text("Rewards", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFF59E0B),
                        selectedTextColor = Color(0xFFF59E0B),
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray,
                        indicatorColor = Color(0xFF2E2A4E)
                    )
                )

                NavigationBarItem(
                    selected = (currentScreen == AppScreen.PROMO_STATS),
                    onClick = { currentScreen = AppScreen.PROMO_STATS },
                    icon = { Icon(Icons.Filled.Info, contentDescription = "Statistics") },
                    label = { Text("Analytics", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFF59E0B),
                        selectedTextColor = Color(0xFFF59E0B),
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray,
                        indicatorColor = Color(0xFF2E2A4E)
                    )
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF1F123C), Color(0xFF0C091A)),
                        radius = 2000f
                    )
                )
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    AppScreen.SPIN_WHEEL -> {
                        SpinWheelScreen(
                            segmentsList = segmentsList,
                            onWinRegistered = { prize, code -> registerWin(prize, code) }
                        )
                    }
                    AppScreen.SEGMENT_EDIT -> {
                        SegmentManagerScreen(
                            segmentsList = segmentsList,
                            onSegmentsUpdated = { updated -> saveSegments(updated) }
                        )
                    }
                    AppScreen.PROMO_STATS -> {
                        OfflineStatsScreen(
                            segmentsList = segmentsList,
                            winHistory = winHistory,
                            onClearHistory = {
                                saveHistory(emptyList())
                                saveSegments(segmentsList.map { it.copy(winCount = 0) })
                            }
                        )
                    }
                }
            }
        }
    }
}

// ---------------------- MAIN SPIN WHEEL SCREEN ----------------------
@Composable
fun SpinWheelScreen(
    segmentsList: List<PrizeSegment>,
    onWinRegistered: (PrizeSegment, String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    var degrees by remember { mutableStateOf(0f) }
    var isSpinning by remember { mutableStateOf(false) }
    var resultText by remember { mutableStateOf("") }
    var resultPrize by remember { mutableStateOf<PrizeSegment?>(null) }
    var enterPromoCode by remember { mutableStateOf("") }
    
    // Shows input / claim overlays
    var showWinnerDialog by remember { mutableStateOf(false) }
    
    // Physics / Sound simulation states
    var lastTickAngle by remember { mutableStateOf(0f) }

    // Confetti particles representation
    var isWinConfettiVisible by remember { mutableStateOf(false) }

    val wheelWeightSum = segmentsList.sumOf { it.probabilityWeight }.toFloat()

    // Determine segments starting / ending angle spans
    val segmentAngles = remember(segmentsList) {
        var currentAngle = 0f
        segmentsList.map { seg ->
            val angleSpan = (seg.probabilityWeight.toFloat() / wheelWeightSum) * 360f
            val start = currentAngle
            currentAngle += angleSpan
            Pair(start, currentAngle)
        }
    }

    // Dynamic rotation animator
    val rotationAnim = remember { Animatable(0f) }

    fun spinTheWheel() {
        if (isSpinning || segmentsList.isEmpty()) return
        
        isSpinning = true
        resultText = ""
        resultPrize = null
        isWinConfettiVisible = false

        // Random deceleration targets
        val totalRotations = 6 + Random.nextInt(4)
        val finalAngleOffset = Random.nextFloat() * 360f
        val targetRotation = (totalRotations * 360f) + finalAngleOffset

        scope.launch {
            lastTickAngle = 0f
            // Run a decaying velocity model or animate with exponential decay custom curve
            rotationAnim.snapTo(0f)
            
            // Decaying spring/cubic-bezier curve for high-velocity starting momentum to fine Lounge drift selection
            rotationAnim.animateTo(
                targetValue = targetRotation,
                animationSpec = tween(
                    durationMillis = 4800,
                    easing = CubicBezierEasing(0.12f, 0.8f, 0.15f, 1.0f)
                )
            ) {
                // Approximate realistic clicking tactile ticks
                val relativeRotation = value % 360f
                val delta = (value - lastTickAngle)
                if (delta >= (360f / segmentsList.size)) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    lastTickAngle = value
                }
                degrees = value
            }

            // Finished Rotation - Determine the true winner
            val finalAngleRelative = ((270f - (targetRotation % 360f)) + 360f) % 360f
            
            // Search inside ranges
            var winnerIdx = 0
            for (i in segmentAngles.indices) {
                val span = segmentAngles[i]
                if (finalAngleRelative >= span.first && finalAngleRelative < span.second) {
                    winnerIdx = i
                    break
                }
            }

            val winnerPrize = segmentsList[winnerIdx]
            resultPrize = winnerPrize
            resultText = winnerPrize.name

            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            
            if (winnerPrize.isWin) {
                isWinConfettiVisible = true
            }
            
            // Prompt custom dialog
            showWinnerDialog = true
            isSpinning = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Overlay particles
        if (isWinConfettiVisible) {
            ConfettiRain()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Logo Branding / Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "O X L O U N G E",
                        color = LightGold,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        letterSpacing = 4.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        text = "EXCLUSIVE SPIN VIP PROMOTIONS",
                        color = NeonPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            // Beautiful physical Lounge style wheel canvas assembly
            Box(
                modifier = Modifier
                    .size(310.dp)
                    .drawBehind {
                        // Drawing an outer purple/violet ambient radial neon glow
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(NeonPurple.copy(alpha = 0.35f), Color.Transparent),
                                center = center,
                                radius = size.minDimension * 0.72f
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Interactive spin wheel component
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .rotate(degrees)
                ) {
                    val diameter = size.minDimension - 20.dp.toPx()
                    val wheelRect = Size(diameter, diameter)
                    val offset = (size.minDimension - diameter) / 2f
                    val boundingBox = Offset(offset, offset)

                    if (segmentsList.isNotEmpty()) {
                        var accumulatedAngle = 0f
                        segmentsList.forEach { segment ->
                            val sweep = (segment.probabilityWeight.toFloat() / wheelWeightSum) * 360f
                            
                            // Segment Fill Arc
                            drawArc(
                                color = segment.getColor(),
                                startAngle = accumulatedAngle,
                                sweepAngle = sweep,
                                useCenter = true,
                                topLeft = boundingBox,
                                size = wheelRect
                            )

                            // Inner dividing lines
                            val dividerR = diameter / 2f
                            val rad = Math.toRadians(accumulatedAngle.toDouble())
                            val endX = center.x + dividerR * cos(rad).toFloat()
                            val endY = center.y + dividerR * sin(rad).toFloat()
                            drawLine(
                                color = Color(0xFF131024).copy(alpha = 0.75f),
                                start = center,
                                end = Offset(endX, endY),
                                strokeWidth = 3f * density
                            )

                            // Sector label drawing
                            val midRad = Math.toRadians((accumulatedAngle + sweep / 2f).toDouble())
                            val textDistance = diameter * 0.34f
                            val textX = center.x + textDistance * cos(midRad).toFloat()
                            val textY = center.y + textDistance * sin(midRad).toFloat()

                            // Custom sector labels painted on Canvas
                            rotate(
                                degrees = accumulatedAngle + (sweep / 2f) + 180f,
                                pivot = Offset(textX, textY)
                            ) {
                                // Draw truncated aesthetic text labels
                                val textPaint = android.graphics.Paint().apply {
                                    color = android.graphics.Color.WHITE
                                    textSize = 10.dp.toPx()
                                    isFakeBoldText = true
                                    textAlign = android.graphics.Paint.Align.CENTER
                                    typeface = android.graphics.Typeface.SANS_SERIF
                                }
                                val maxChar = 12
                                val labelTxt = if (segment.name.length > maxChar) {
                                    segment.name.substring(0, maxChar - 1) + ".."
                                } else {
                                    segment.name
                                }
                                drawContext.canvas.nativeCanvas.drawText(
                                    labelTxt,
                                    textX,
                                    textY + 3.dp.toPx(),
                                    textPaint
                                )
                            }

                            accumulatedAngle += sweep
                        }
                    }

                    // Outer golden neon metallic boundary Ring
                    drawCircle(
                        color = LightGold,
                        center = center,
                        radius = (diameter / 2f) + 1.dp.toPx(),
                        style = Stroke(width = 4.dp.toPx())
                    )

                    // Secondary neon purple internal ring
                    drawCircle(
                        color = Color(0xFF1F123C),
                        center = center,
                        radius = (diameter / 2f) - 6.dp.toPx(),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Central high-contrast neon command SPIN button (does not rotate)
                Box(
                    modifier = Modifier
                        .size(86.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF231A3F), Color(0xFF120B27))
                            )
                        )
                        .border(3.dp, GoldenGold, CircleShape)
                        .clickable(enabled = !isSpinning) { spinTheWheel() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SPIN",
                            color = LightGold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            imageIcons = Icons.Filled.Star,
                            color = LightGold,
                            size = 14.dp
                        )
                    }
                }

                // Glowing Neon golden top pin indicator marker pointing straight down
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 12.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Canvas(modifier = Modifier.size(28.dp, 36.dp)) {
                        val path = Path().apply {
                            moveTo(size.width / 2f, size.height)
                            lineTo(size.width * 0.15f, 0f)
                            lineTo(size.width * 0.85f, 0f)
                            close()
                        }
                        
                        // Drop shadow glow
                        drawPath(
                            path = path,
                            color = Color(0xFFEC4899),
                            style = Stroke(width = 5.dp.toPx(), join = StrokeJoin.Round)
                        )

                        // Central Gold Core shape indicator
                        drawPath(
                            path = path,
                            color = LightGold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.12f))

            // Bottom CTA section helper info
            if (!isSpinning && resultText.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1535).copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, NeonPurple.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Tips",
                            tint = LightGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Tap the Gold SPIN knob to award the guest. Runs offline!",
                            color = TextGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Beautiful result confirmation alert
        if (showWinnerDialog && resultPrize != null) {
            AlertDialog(
                onDismissRequest = { showWinnerDialog = false },
                containerColor = Color(0xFF1E1638),
                textContentColor = TextLight,
                titleContentColor = LightGold,
                shape = RoundedCornerShape(24.dp),
                icon = {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(if (resultPrize!!.isWin) NeonPurple.copy(alpha = 0.25f) else Color(0x23EF4444)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (resultPrize!!.isWin) Icons.Filled.CheckCircle else Icons.Filled.Close,
                            contentDescription = "Outcome Icon",
                            tint = if (resultPrize!!.isWin) LightGold else Color(0xFFF87171),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = if (resultPrize!!.isWin) "COUPON UNLOCKED!" else "AESTHETIC RETRY",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Serif,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val resultNameStyled = resultPrize!!.name.uppercase()
                        Text(
                            text = resultNameStyled,
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        )

                        Text(
                            text = if (resultPrize!!.isWin) {
                                "Verify this with the counter host. You can enter an optional custom Customer Code or Promo Tag below to save locally:"
                            } else {
                                "The wheel did not resolve in a reward target. Tap close to retry your spin!"
                            },
                            color = TextGray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        if (resultPrize!!.isWin) {
                            OutlinedTextField(
                                value = enterPromoCode,
                                onValueChange = { enterPromoCode = it },
                                label = { Text("Customer ID / Lead Code") },
                                textStyle = LocalTextStyle.current.copy(color = Color.White),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonPurple,
                                    unfocusedBorderColor = Color(0xFF3C3364),
                                    focusedLabelColor = LightGold,
                                    unfocusedLabelColor = TextGray
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val code = enterPromoCode.trim().ifEmpty { "GUEST-${Random.nextInt(1000, 9999)}" }
                            onWinRegistered(resultPrize!!, code)
                            showWinnerDialog = false
                            enterPromoCode = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LightGold),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("CONFIRM WIN", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWinnerDialog = false }) {
                        Text("CLOSE", color = TextGray)
                    }
                }
            )
        }
    }
}

// Simple Icon Size component helper
@Composable
fun Icon(imageIcons: ImageVector, color: Color, size: Dp) {
    Icon(
        imageVector = imageIcons,
        contentDescription = null,
        tint = color,
        modifier = Modifier.size(size)
    )
}

// ---------------------- REWARDS / SEGMENT MANAGER SCREEN ----------------------
@Composable
fun SegmentManagerScreen(
    segmentsList: List<PrizeSegment>,
    onSegmentsUpdated: (List<PrizeSegment>) -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }

    // Forms fields
    var newName by remember { mutableStateOf("") }
    var newWeight by remember { mutableStateOf("3") }
    var newIsWin by remember { mutableStateOf(true) }
    var newHexColor by remember { mutableStateOf("#A855F7") }

    // Preset nice colors to assign
    val loungeColors = listOf(
        "#A855F7", // Neon Purple
        "#F59E0B", // Golden Yellow
        "#3B82F6", // Electric Blue
        "#EC4899", // Neon Hot Pink
        "#10B981", // Emerald Green
        "#EF4444", // Coral Red
        "#8B5CF6", // Dark Lavender
        "#1F2937"  // Lounge Slate
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Lounge Prizes",
                    color = LightGold,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "${segmentsList.size} segments configured",
                    color = TextGray,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Prize", tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("ADD SEGMENT", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (segmentsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Empty",
                        tint = TextGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No prize segments configured.", color = TextGray, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(segmentsList) { segment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1832)),
                        border = BorderStroke(1.dp, Color(0xFF2E2A4E)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Sector color indicator
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(segment.getColor())
                                    .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = segment.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    SuggestionChip(
                                        onClick = { },
                                        label = { Text("Weight: ${segment.probabilityWeight}", fontSize = 10.sp) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            labelColor = LightGold,
                                            containerColor = Color.Transparent
                                        ),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = LightGold.copy(alpha = 0.3f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (segment.isWin) "Redeemable Win" else "No Reward",
                                        color = if (segment.isWin) Color(0xFF4ADE80) else TextGray,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Delete button (maintain minimum 3 segments for visual rendering)
                            IconButton(
                                onClick = {
                                    if (segmentsList.size <= 3) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Keep at least 3 segments for the Wheel!",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        val updated = segmentsList.filter { it.id != segment.id }
                                        onSegmentsUpdated(updated)
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color(0xFFF87171))
                            }
                        }
                    }
                }
            }
        }

        // Add Segment Form Dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                containerColor = Color(0xFF1E1638),
                textContentColor = TextLight,
                titleContentColor = LightGold,
                shape = RoundedCornerShape(24.dp),
                title = {
                    Text(
                        text = "NEW WHEEL SECTOR",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        fontSize = 17.sp
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Prize Segment Label") },
                            textStyle = LocalTextStyle.current.copy(color = Color.White),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPurple,
                                unfocusedBorderColor = Color(0xFF3C3364),
                                focusedLabelColor = LightGold
                            )
                        )

                        OutlinedTextField(
                            value = newWeight,
                            onValueChange = { newWeight = it },
                            label = { Text("Probability Weight (1-10)") },
                            textStyle = LocalTextStyle.current.copy(color = Color.White),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPurple,
                                unfocusedBorderColor = Color(0xFF3C3364),
                                focusedLabelColor = LightGold
                            )
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Is Redeemable Reward?", color = TextLight, modifier = Modifier.weight(1f))
                            Switch(
                                checked = newIsWin,
                                onCheckedChange = { newIsWin = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = LightGold,
                                    checkedTrackColor = NeonPurple
                                )
                            )
                        }

                        Text("Select Sector Theme Color:", color = TextGray, fontSize = 12.sp)

                        // Nice Color Preset Swatches
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            loungeColors.forEach { colorStr ->
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(colorStr)))
                                        .border(
                                            width = if (newHexColor == colorStr) 2.dp else 0.dp,
                                            color = Color.White,
                                            shape = CircleShape
                                        )
                                        .clickable { newHexColor = colorStr }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newName.isBlank()) return@Button
                            val weight = newWeight.toIntOrNull() ?: 1
                            val newSeg = PrizeSegment(
                                id = UUID.randomUUID().toString(),
                                name = newName.trim(),
                                hexColor = newHexColor,
                                probabilityWeight = weight.coerceIn(1, 10),
                                isWin = newIsWin
                            )
                            onSegmentsUpdated(segmentsList + newSeg)
                            showAddDialog = false
                            newName = ""
                            newWeight = "3"
                            newIsWin = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LightGold)
                    ) {
                        Text("ADD SECTOR", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("CANCEL", color = TextGray)
                    }
                }
            )
        }
    }
}

// ---------------------- OFFLINE STATISTICS / ANALYTICS SCREEN ----------------------
@Composable
fun OfflineStatsScreen(
    segmentsList: List<PrizeSegment>,
    winHistory: List<WinHistoryItem>,
    onClearHistory: () -> Unit
) {
    val sdf = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Offline Stats",
                    color = LightGold,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "Real-time promotion counters",
                    color = TextGray,
                    fontSize = 12.sp
                )
            }

            if (winHistory.isNotEmpty()) {
                TextButton(
                    onClick = onClearHistory,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF87171))
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Clear", tint = Color(0xFFF87171))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("RESET ALL", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Basic KPI Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1832)),
                border = BorderStroke(1.dp, Color(0xFF2E2A4E))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Total Spins", color = TextGray, fontSize = 11.sp)
                    Text("${winHistory.size}", color = LightGold, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1832)),
                border = BorderStroke(1.dp, Color(0xFF2E2A4E))
            ) {
                val winnersCount = winHistory.count { it.prizeName.isNotBlank() }
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Rewards", color = TextGray, fontSize = 11.sp)
                    Text("$winnersCount", color = NeonPurple, fontSize = 24.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Visual Custom Analytics Chart
        Text(
            text = "REDEEMED PRIZES RATIO",
            color = LightGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF110C24)),
            border = BorderStroke(1.dp, Color(0xFF2E2A4E))
        ) {
            if (winHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tap standard spins first to view analytical bars.", color = TextGray, fontSize = 12.sp)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Maximum counter count
                    val maxWinCount = segmentsList.maxOf { it.winCount }.coerceAtLeast(1)
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(segmentsList.filter { it.isWin }) { prize ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = prize.name,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.width(90.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Render Horizontal Progress Bar
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF1F1A3A))
                                ) {
                                    val fraction = prize.winCount.toFloat() / maxWinCount.toFloat()
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction)
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(prize.getColor(), prize.getColor().copy(alpha = 0.6f))
                                                )
                                            )
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "${prize.winCount}",
                                    color = LightGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.width(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // History Log List
        Text(
            text = "RECENT COUPONS LOG (OFFLINE)",
            color = LightGold,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1832)),
            border = BorderStroke(1.dp, Color(0xFF2E2A4E)),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (winHistory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No spins recorded in session log yet.", color = TextGray, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(winHistory) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF231F3F), RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.prizeName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(
                                    text = "Code: ${item.customerCode}",
                                    color = LightGold,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = sdf.format(Date(item.timestamp)),
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- PRETTY FLOATING CONFETTI EMITTER ----------------------
@Composable
fun ConfettiRain() {
    val confettiColors = listOf(
        Color(0xFFA855F7), // violet
        Color(0xFFFBBF24), // gold/amber
        Color(0xFF3B82F6), // blue
        Color(0xFFF43F5E), // rose
        Color(0xFF10B981)  // emerald
    )

    val listCount = 45
    val infiniteTransition = rememberInfiniteTransition(label = "ConfettiTransition")

    // Setup animated factors for particle streams
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val seedRandom = Random(12345) // Fixed seed for stable visually aesthetic orbits

        for (i in 0 until listCount) {
            val color = confettiColors[seedRandom.nextInt(confettiColors.size)]
            
            // Layout offsets based on screen width/height
            val startX = seedRandom.nextFloat() * width
            val speedY = 400.dp.toPx() + seedRandom.nextFloat() * 600.dp.toPx()
            val driftAmplitude = 30.dp.toPx() + seedRandom.nextFloat() * 50.dp.toPx()
            
            // Calculated positions
            val currentY = (progress * speedY) % height
            val currentX = startX + sin(progress * 6f + i) * driftAmplitude
            
            val rotateAngle = progress * 360f * (1f + seedRandom.nextFloat())
            
            // Draw colorful visual squares / ribbons
            rotate(degrees = rotateAngle, pivot = Offset(currentX, currentY)) {
                drawRect(
                    color = color,
                    topLeft = Offset(currentX, currentY),
                    size = Size(8.dp.toPx(), 8.dp.toPx())
                )
            }
        }
    }
}
