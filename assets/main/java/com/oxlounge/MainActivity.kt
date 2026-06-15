package com.oxlounge

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

// Sleek Theme Colors
val DarkBg = Color(0xFF0F0F12) // Very sleek, dark luxury coal background
val DeepPurple = Color(0xFF1A1A24) // Deep dark slate background for cards
val GlowPurple = Color(0xFF8B5CF6) // Elegant purple accent (Tailwind's purple-500)
val AccentGold = Color(0xFFFBBF24) // Warm amber-400 gold/yellow glow
val GlowGold = Color(0xFFD97706) // Deep amber-600 gold accent
val SoftLilac = Color(0xFF94A3B8) // Muted slate-400 blue-gray for text
val NeonRed = Color(0xFFEF4444) // Cyber red for errors (red-500)

enum class AppScreen {
    SPLASH,
    PASSCODE,
    SPIN_WHEEL
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LoungeAppTheme {
                LoungeAppContent()
            }
        }
    }
}

@Composable
fun LoungeAppTheme(content: @Composable () -> Unit) {
    val darkColorScheme = darkColorScheme(
        primary = AccentGold,
        secondary = GlowPurple,
        background = DarkBg,
        surface = DeepPurple,
        onPrimary = Color.Black,
        onSecondary = Color.White,
        onBackground = SoftLilac,
        onSurface = Color.White
    )
    MaterialTheme(
        colorScheme = darkColorScheme,
        typography = Typography(),
        content = content
    )
}

data class WheelSegment(
    val shortTitle: String,
    val fullPrizeName: String,
    val description: String,
    var weight: Float, // Editable probability factor
    val color: Color,
    val textColor: Color,
    val iconEmoji: String
)

@Composable
fun LoungeAppContent(startScreen: AppScreen = AppScreen.SPLASH) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
    val safeVibrate = remember(vibrator) {
        { millis: Long ->
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator?.vibrate(android.os.VibrationEffect.createOneShot(millis, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(millis)
                }
            } catch (e: Exception) {
                // Silently ignore vibration warnings or security exceptions
            }
        }
    }

    // Navigation and Passcode state
    var currentScreen by remember { mutableStateOf(startScreen) }
    var enteredPasscode by remember { mutableStateOf("") }
    var isPasscodeError by remember { mutableStateOf(false) }

    // Spin animation and Wheel Segment configuration states derived from high-fidelity CSS conic gradient segments
    val segmentsList = remember {
        mutableStateListOf(
            WheelSegment(
                shortTitle = "Free Coke",
                fullPrizeName = "Win Free Coke",
                description = "Redeemable immediately. Pop open a premium cold Coca-Cola on the house!",
                weight = 15f,
                color = Color(0xFF3B0764),
                textColor = AccentGold,
                iconEmoji = "🥤"
            ),
            WheelSegment(
                shortTitle = "Free Water",
                fullPrizeName = "Win Free Water",
                description = "Stay refreshed with a premium bottle of pure mineral water.",
                weight = 20f,
                color = Color(0xFF1C1B22),
                textColor = Color.White,
                iconEmoji = "💧"
            ),
            WheelSegment(
                shortTitle = "Buy 2 Win Promo",
                fullPrizeName = "Buy 2 More Bottles (Win Free Drink/Small Towel)",
                description = "Purchase two extra bottles to unlock a luxury refreshment or a branded workout towel!",
                weight = 25f,
                color = Color(0xFF4C1D95),
                textColor = Color.White,
                iconEmoji = "🏷️"
            ),
            WheelSegment(
                shortTitle = "Buy 5 Win Towel",
                fullPrizeName = "Buy 5 More Bottles (Win Big Towel)",
                description = "Unlock the premier tier! Grab five bottles to take home a premium designer lounge beach towel.",
                weight = 10f,
                color = Color(0xFF1C1B22),
                textColor = AccentGold,
                iconEmoji = "🎁"
            ),
            WheelSegment(
                shortTitle = "Try Again",
                fullPrizeName = "Try Again",
                description = "No luck this turn. Recharge your energy and challenge the Oxlounge wheel again!",
                weight = 30f,
                color = Color(0xFF581C87),
                textColor = SoftLilac,
                iconEmoji = "🔄"
            )
        )
    }

    // Interactive Spinning animation
    val rotationAnimatable = remember { Animatable(0f) }
    var isSpinning by remember { mutableStateOf(false) }
    var showPrizeDialog by remember { mutableStateOf(false) }
    var winningSegment by remember { mutableStateOf<WheelSegment?>(null) }
    var showWeightsTuner by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                // High fidelity dark lounge background drawing with soft radial purple glow
                drawRect(color = DarkBg)
                val radialBrush = Brush.radialGradient(
                    colors = listOf(Color(0xFF220A45), Color(0xFF0F0F12)),
                    center = Offset(size.width / 2f, size.height / 3f),
                    radius = size.width * 1.2f
                )
                drawRect(brush = radialBrush)
            },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = currentScreen,
                animationSpec = tween(durationMillis = 500),
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    AppScreen.SPLASH -> {
                        SplashScreen(
                            onSplashFinished = {
                                currentScreen = AppScreen.PASSCODE
                            }
                        )
                    }

                    AppScreen.PASSCODE -> {
                        PasscodeScreen(
                            enteredPasscode = enteredPasscode,
                            isError = isPasscodeError,
                            onKeyClick = { digit ->
                                if (enteredPasscode.length < 4 && !isPasscodeError) {
                                    safeVibrate(30)
                                    enteredPasscode += digit
                                    if (enteredPasscode.length == 4) {
                                        // Verify passcode
                                        if (enteredPasscode == "1234") {
                                            coroutineScope.launch {
                                                delay(250)
                                                currentScreen = AppScreen.SPIN_WHEEL
                                                enteredPasscode = ""
                                            }
                                        } else {
                                            isPasscodeError = true
                                            safeVibrate(200)
                                            coroutineScope.launch {
                                                delay(1200)
                                                isPasscodeError = false
                                                enteredPasscode = ""
                                            }
                                        }
                                    }
                                }
                            },
                            onBackspaceClick = {
                                if (enteredPasscode.isNotEmpty() && !isPasscodeError) {
                                    safeVibrate(20)
                                    enteredPasscode = enteredPasscode.dropLast(1)
                                }
                            },
                            onClearClick = {
                                if (!isPasscodeError) {
                                    safeVibrate(20)
                                    enteredPasscode = ""
                                }
                            }
                        )
                    }

                    AppScreen.SPIN_WHEEL -> {
                        SpinWheelScreen(
                            rotationAngle = rotationAnimatable.value,
                            segments = segmentsList,
                            isSpinning = isSpinning,
                            showTuner = showWeightsTuner,
                            onToggleTuner = { showWeightsTuner = !showWeightsTuner },
                            onSpinTrigger = {
                                if (!isSpinning) {
                                    isSpinning = true
                                    coroutineScope.launch {
                                        // 1. Selector logic using dynamic weights
                                        val selectedIndex = selectWeightedIndex(segmentsList)
                                        val prize = segmentsList[selectedIndex]

                                        // 2. Physics calculation for rotation
                                        val sectorSweep = 360f / segmentsList.size
                                        val targetCenterAngle = (selectedIndex * sectorSweep) + (sectorSweep / 2f)
                                        
                                        // Reset animatable cleanly within bounds before launch to prevent rotation overflow
                                        val currentAngleBounded = rotationAnimatable.value % 360f
                                        rotationAnimatable.snapTo(currentAngleBounded)

                                        val offsetToTarget = ((270f - targetCenterAngle) - currentAngleBounded + 360f) % 360f
                                        val destinationAngle = currentAngleBounded + (8 * 360f) + offsetToTarget

                                        // 3. Smooth custom decelerating float animation
                                        rotationAnimatable.animateTo(
                                            targetValue = destinationAngle,
                                            animationSpec = tween(
                                                durationMillis = 5000,
                                                easing = CubicBezierEasing(0.12f, 0.8f, 0.15f, 1.0f)
                                            )
                                        )

                                        // Trigger haptics, set outcome, and open dialog
                                        safeVibrate(150)
                                        winningSegment = prize
                                        showPrizeDialog = true
                                        isSpinning = false
                                    }
                                }
                            },
                            onLockClick = {
                                safeVibrate(50)
                                currentScreen = AppScreen.PASSCODE
                                enteredPasscode = ""
                            },
                            onWeightChange = { index, newWeight ->
                                segmentsList[index] = segmentsList[index].copy(weight = newWeight)
                            }
                        )
                    }
                }
            }

            // High Fidelity Neon Victory Overlay Modal (State 3)
            if (showPrizeDialog && winningSegment != null) {
                PrizeWinningDialog(
                    prize = winningSegment!!,
                    onDismiss = {
                        showPrizeDialog = false
                    },
                    onLockDismiss = {
                        showPrizeDialog = false
                        currentScreen = AppScreen.PASSCODE
                        enteredPasscode = ""
                    }
                )
            }
        }
    }
}

// Keypad / Passcode Entering Portal
@Composable
fun PasscodeScreen(
    enteredPasscode: String,
    isError: Boolean,
    onKeyClick: (String) -> Unit,
    onBackspaceClick: () -> Unit,
    onClearClick: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val availableHeight = maxHeight
        val isCompactHeight = availableHeight < 680.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 24.dp,
                    vertical = if (isCompactHeight) 12.dp else 24.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 16.dp else 28.dp)
        ) {
            if (!isCompactHeight) {
                Spacer(modifier = Modifier.height(28.dp))
            }

            // Branding and secure lock visual header matching Sleek style
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(if (isCompactHeight) 80.dp else 100.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    // Circular main logo branding
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(16.dp, CircleShape, spotColor = GlowPurple)
                            .clip(CircleShape)
                            .border(2.dp, if (isError) NeonRed else AccentGold, CircleShape)
                            .background(DeepPurple)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.oxlounge_logo_1781498129964),
                            contentDescription = "Oxlounge Logo",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    // Small secure floating lock seal badge
                    Box(
                        modifier = Modifier
                            .size(if (isCompactHeight) 24.dp else 30.dp)
                            .offset(x = 2.dp, y = 2.dp)
                            .clip(CircleShape)
                            .background(DarkBg)
                            .border(1.5.dp, if (isError) NeonRed else AccentGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Badge",
                            tint = if (isError) NeonRed else AccentGold,
                            modifier = Modifier.size(if (isCompactHeight) 11.dp else 14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (isCompactHeight) 10.dp else 18.dp))

                Text(
                    text = "STAFF PORTAL",
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFC084FC), Color(0xFFFCD34D))
                        ),
                        fontSize = if (isCompactHeight) 20.sp else 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 4.sp
                    ),
                    modifier = Modifier.testTag("portal_header")
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isError) "ACCESS DENIED — RETRYING" else "Authorized Staff Code Required",
                    color = if (isError) NeonRed else SoftLilac,
                    fontSize = if (isCompactHeight) 12.sp else 13.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }

            // Passcode entry dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = if (isCompactHeight) 4.dp else 12.dp)
            ) {
                for (i in 0 until 4) {
                    val filled = i < enteredPasscode.length
                    val dotColor = if (isError) NeonRed else if (filled) AccentGold else GlowPurple.copy(alpha = 0.35f)
                    val dotRadius = if (filled) (if (isCompactHeight) 10.dp else 11.dp) else (if (isCompactHeight) 7.dp else 8.dp)
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.CenterVertically),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(dotRadius)
                                .clip(CircleShape)
                                .background(dotColor)
                                .border(1.dp, if (filled) Color.White else Color.Transparent, CircleShape)
                        )
                    }
                }
            }

            // Numeric Keypad Grid
            Column(
                modifier = Modifier.padding(bottom = if (isCompactHeight) 8.dp else 24.dp),
                verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 12.dp else 16.dp)
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "⌫")
                )

                for (row in keys) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(22.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (key in row) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                if (key.isEmpty()) {
                                    Spacer(modifier = Modifier.size(if (isCompactHeight) 58.dp else 70.dp))
                                } else {
                                    KeypadButton(
                                        label = key,
                                        isSpecial = key == "C" || key == "⌫",
                                        disabled = isError,
                                        isCompact = isCompactHeight,
                                        onClick = {
                                            when (key) {
                                                "C" -> onClearClick()
                                                "⌫" -> onBackspaceClick()
                                                else -> onKeyClick(key)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadButton(
    label: String,
    isSpecial: Boolean,
    disabled: Boolean,
    isCompact: Boolean = false,
    onClick: () -> Unit
) {
    val size = if (isCompact) 58.dp else 70.dp
    val fontSize = if (isSpecial) (if (isCompact) 16.sp else 18.sp) else (if (isCompact) 20.sp else 24.sp)

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (isSpecial) Color.Transparent else DeepPurple.copy(alpha = 0.45f)
            )
            .border(
                width = 1.5.dp,
                color = if (isSpecial) GlowPurple.copy(alpha = 0.3f) else GlowPurple.copy(alpha = 0.7f),
                shape = CircleShape
            )
            .clickable(
                enabled = !disabled,
                onClick = onClick
            )
            .testTag("key_$label"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSpecial) SoftLilac else AccentGold,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp
        )
    }
}

// Spinning Portal Screen
@Composable
fun SpinWheelScreen(
    rotationAngle: Float,
    segments: List<WheelSegment>,
    isSpinning: Boolean,
    showTuner: Boolean,
    onToggleTuner: () -> Unit,
    onSpinTrigger: () -> Unit,
    onLockClick: () -> Unit,
    onWeightChange: (Int, Float) -> Unit
) {
    Scaffold(
        topBar = {
            // High fidelity upper VIP navigation and branding top row matching "Sleek Interface" HTML header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onLockClick() }
                        .border(1.5.dp, AccentGold, CircleShape)
                        .shadow(4.dp, CircleShape)
                        .testTag("lock_app_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.oxlounge_logo_1781498129964),
                        contentDescription = "Oxlounge Logo Button",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "OXLOUNGE",
                        style = TextStyle(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFC084FC), Color(0xFFFCD34D))
                            ),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        modifier = Modifier.testTag("portal_lobby_header")
                    )
                    Text(
                        text = "STAFF EXCLUSIVE",
                        color = SoftLilac,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp
                    )
                }

                IconButton(
                    onClick = onToggleTuner,
                    modifier = Modifier
                        .background(if (showTuner) GlowPurple else DeepPurple, RoundedCornerShape(12.dp))
                        .border(1.dp, GlowPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .testTag("toggle_tuner_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Odds Tweaking Sheet",
                        tint = if (showTuner) Color.White else AccentGold
                    )
                }
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            val wheelOuterSize = if (showTuner) 180.dp else 310.dp
            val wheelInnerSize = wheelOuterSize - 20.dp
            val wheelHaloSize = wheelOuterSize - 4.dp

            Spacer(modifier = Modifier.weight(0.02f))

            // Large physical Canvas wheel container
            Box(
                modifier = Modifier
                    .size(wheelOuterSize)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center
            ) {
                // Shadow behind the wheel for organic floating depth (shadow-[0_0_60px_rgba(168,85,247,0.2)])
                Box(
                    modifier = Modifier
                        .size(wheelInnerSize)
                        .shadow(
                            elevation = if (showTuner) 16.dp else 30.dp,
                            shape = CircleShape,
                            ambientColor = GlowPurple.copy(alpha = 0.5f),
                            spotColor = GlowPurple
                        )
                )

                // Neon halo ring outer border (border-8 border-purple-900/50 relative overflow-hidden)
                Canvas(modifier = Modifier.size(wheelHaloSize)) {
                    drawCircle(
                        color = Color(0xFF581C87).copy(alpha = 0.5f),
                        style = Stroke(width = if (showTuner) 5.dp.toPx() else 8.dp.toPx())
                    )
                    // Outer glowing detail
                    drawCircle(
                        color = GlowPurple.copy(alpha = 0.2f),
                        style = Stroke(width = 1.dp.toPx()),
                        radius = (size.width / 2f) + (if (showTuner) 2.5.dp.toPx() else 4.dp.toPx())
                    )
                }

                // Rotated digital canvas wheel with custom wedge structure
                Canvas(
                    modifier = Modifier
                        .size(wheelInnerSize)
                        .rotate(rotationAngle)
                        .testTag("spin_canvas")
                ) {
                    val wheelSize = size
                    val centerPt = Offset(wheelSize.width / 2f, wheelSize.height / 2f)
                    val radius = wheelSize.width / 2f
                    val sweepAngle = 360f / segments.size

                    for (i in segments.indices) {
                        val segment = segments[i]
                        val startAngle = i * sweepAngle

                        // 1. Arc segment background
                        drawArc(
                            color = segment.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = true,
                            size = wheelSize
                        )

                        // 2. Stylish division lines (bg-purple-500/20 in HTML)
                        val borderRad = Math.toRadians(startAngle.toDouble())
                        val borderX = centerPt.x + radius * cos(borderRad).toFloat()
                        val borderY = centerPt.y + radius * sin(borderRad).toFloat()
                        drawLine(
                            color = GlowPurple.copy(alpha = 0.25f),
                            start = centerPt,
                            end = Offset(borderX, borderY),
                            strokeWidth = 1.dp.toPx()
                        )

                        // 3. Segment Titles drawn rotated to sector centerlines
                        val centerAngle = startAngle + sweepAngle / 2f
                        rotate(degrees = centerAngle, pivot = centerPt) {
                            drawIntoCanvas { canvas ->
                                val nativeC = canvas.nativeCanvas
                                val baseTextSize = if (segment.shortTitle.length > 13) 11.dp.toPx() else 13.dp.toPx()
                                val paint = Paint().apply {
                                    color = segment.textColor.toArgb()
                                    textSize = if (showTuner) baseTextSize * 0.62f else baseTextSize
                                    isAntiAlias = true
                                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                    textAlign = Paint.Align.RIGHT
                                }
                                // Situate title text beautifully outward towards rim
                                val placementX = wheelSize.width - (if (showTuner) 12.dp.toPx() else 24.dp.toPx())
                                val placementY = centerPt.y + (paint.textSize / 3.2f)
                                nativeC.drawText(
                                    "${segment.iconEmoji} ${segment.shortTitle}",
                                    placementX,
                                    placementY,
                                    paint
                                )
                            }
                        }
                    }
                }

                // High aesthetic center metallic crown/hub with pulsing glow dot (animate-pulse)
                val infiniteTransition = rememberInfiniteTransition(label = "HubPulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.85f,
                    targetValue = 1.3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "PulseDotScale"
                )

                Box(
                    modifier = Modifier
                        .size(if (showTuner) 28.dp else 48.dp)
                        .clip(CircleShape)
                        .background(DarkBg)
                        .border(if (showTuner) 2.5.dp else 4.dp, AccentGold, CircleShape)
                        .shadow(elevation = if (showTuner) 6.dp else 12.dp, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (showTuner) 7.dp else 12.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(AccentGold)
                    )
                }

                // Stationary Glowing Downward Pointer Arrow matching border-t-amber-400 drop-shadow
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 1.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Canvas(
                        modifier = Modifier
                            .offset(y = if (showTuner) (-10).dp else (-14).dp)
                            .size(if (showTuner) 14.dp else 24.dp, if (showTuner) 18.dp else 32.dp)
                    ) {
                        val path = Path().apply {
                            moveTo(size.width / 2f, size.height)
                            lineTo(0f, 0f)
                            lineTo(size.width, 0f)
                            close()
                        }
                        drawPath(path = path, color = AccentGold)
                        drawPath(
                            path = path,
                            color = Color.White.copy(alpha = 0.5f),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.02f))

            // Tuner & Sliders Area vs Main Action Controller
            if (showTuner) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.88f)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(DeepPurple)
                        .border(1.5.dp, GlowPurple.copy(alpha = 0.3f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "OFFLINE WEIGHT TWEENER",
                                color = AccentGold,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Standard Preset Default",
                                    tint = SoftLilac,
                                    modifier = Modifier
                                        .clickable {
                                            // Reset default equalized weights
                                            onWeightChange(0, 15f)
                                            onWeightChange(1, 20f)
                                            onWeightChange(2, 25f)
                                            onWeightChange(3, 10f)
                                            onWeightChange(4, 30f)
                                        }
                                        .size(20.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close settings and go back to game",
                                    tint = AccentGold,
                                    modifier = Modifier
                                        .clickable { onToggleTuner() }
                                        .size(22.dp)
                                        .testTag("close_tuner_button")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val totalWeight = segments.sumOf { it.weight.toDouble() }.toFloat()

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(segments) { index, segment ->
                                val currentPercent = if (totalWeight > 0) (segment.weight / totalWeight) * 100f else 0f
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DarkBg.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        .border(1.dp, GlowPurple.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${segment.iconEmoji} ${segment.shortTitle}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "Weight: ${segment.weight.toInt()} (${String.format("%.1f", currentPercent)}%)",
                                            color = AccentGold,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Slider(
                                        value = segment.weight,
                                        onValueChange = { newValue ->
                                            onWeightChange(index, newValue.coerceIn(1f, 100f))
                                        },
                                        valueRange = 1f..100f,
                                        steps = 98,
                                        colors = SliderDefaults.colors(
                                            thumbColor = AccentGold,
                                            activeTrackColor = GlowPurple,
                                            inactiveTrackColor = GlowPurple.copy(alpha = 0.24f)
                                        ),
                                        modifier = Modifier
                                            .height(28.dp)
                                            .testTag("slider_$index")
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Main Action Center / Static guide messages
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                ) {
                    Text(
                        text = "Oxlounge VIP sweepstakes rewards are 100% locally calculated.",
                        color = SoftLilac,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // High fidelity fully customized SPIN button matching border/dimensions of "Sleek Interface" active element
                    Button(
                        onClick = onSpinTrigger,
                        enabled = !isSpinning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSpinning) DeepPurple else AccentGold,
                            contentColor = if (isSpinning) AccentGold else Color.Black
                        ),
                        shape = RoundedCornerShape(16.dp), // rounded-2xl in HTML (16.dp)
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(56.dp)
                            .shadow(
                                elevation = if (isSpinning) 0.dp else 12.dp,
                                shape = RoundedCornerShape(16.dp),
                                clip = false,
                                ambientColor = AccentGold.copy(alpha = 0.3f),
                                spotColor = AccentGold
                            )
                            .testTag("spin_button"),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Spin Dynamic Loop",
                                modifier = Modifier.size(20.dp),
                                tint = if (isSpinning) AccentGold else Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSpinning) "SPINNING..." else "SPIN",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// Dialog/Pop-up Victory overlay display matching Sleek Interface design
@Composable
fun PrizeWinningDialog(
    prize: WheelSegment,
    onDismiss: () -> Unit,
    onLockDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(24.dp)) // rounded-3xl (24.dp)
                    .background(DeepPurple)
                    .border(2.dp, GlowPurple.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Oxlounge Luxury Logo Stamp in celebration dialog
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .shadow(16.dp, CircleShape, spotColor = AccentGold)
                        .clip(CircleShape)
                        .border(2.5.dp, AccentGold, CircleShape)
                        .background(DeepPurple),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.oxlounge_logo_1781498129964),
                        contentDescription = "Oxlounge Logo Stamp",
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Winner!",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "You've unlocked the",
                    color = SoftLilac,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = prize.fullPrizeName,
                    color = AccentGold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .testTag("prize_title")
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = prize.description,
                    color = SoftLilac.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Core control actions matching Sleek HTML
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Safe reset action returning directly to lock state (Reset Device bg-purple-600)
                    Button(
                        onClick = onLockDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlowPurple,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("claim_and_lock_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Redemption Lock Icon",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Reset Device",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Keep spinning options (secondary control for staff flexibility)
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(14.dp),
                        border = borderStroke(1.dp, GlowPurple.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AccentGold
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("dismiss_dialog_button")
                    ) {
                        Text(
                            text = "Spin Again (Keep Unlocked)",
                            fontSize = 12.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

// Internal reusable helper for border styling compatibility
fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) = androidx.compose.foundation.BorderStroke(width, color)

// Weighted random selector implementation
fun selectWeightedIndex(segments: List<WheelSegment>): Int {
    val totalWeight = segments.sumOf { it.weight.toDouble() }.toFloat()
    if (totalWeight <= 0f) return 0

    // Offline secure mathematical random
    val value = Random.nextFloat() * totalWeight
    var accumulated = 0f
    for (i in segments.indices) {
        accumulated += segments[i].weight
        if (value <= accumulated) {
            return i
        }
    }
    return 0
}

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Parallel animations: scale up and fade in
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(1200, easing = LinearOutSlowInEasing)
            )
        }
        delay(2500) // Keep splash screen for 2.5 seconds
        // Fade out
        launch {
            alpha.animateTo(0f, animationSpec = tween(500))
        }
        delay(500)
        onSplashFinished()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            // Elegant glowing logo container
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .shadow(32.dp, CircleShape, spotColor = GlowPurple, ambientColor = GlowPurple.copy(alpha = 0.5f))
                    .clip(CircleShape)
                    .border(3.dp, AccentGold, CircleShape)
                    .background(DeepPurple),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.oxlounge_logo_1781498129964),
                    contentDescription = "Oxlounge Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "OXLOUNGE",
                style = TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFC084FC), Color(0xFFFCD34D))
                    ),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 6.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "STAFF EXCLUSIVE",
                color = SoftLilac,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Premium elegant horizontal loading indicator
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                val progressAnim = rememberInfiniteTransition(label = "SplashProgress")
                val widthShrink by progressAnim.animateFloat(
                    initialValue = 0f,
                    targetValue = 120f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "width"
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(widthShrink.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(GlowPurple, AccentGold)
                            )
                        )
                )
            }
        }
    }
}
