package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ScreenAwakeManager
import com.example.ui.MainViewModel
import com.example.ui.ScreenOffActivity
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.AmberSecondary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.LuceSchermoTheme
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.TextMuted

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ScreenAwakeManager.init(this)
        if (!ScreenAwakeManager.isAwake(this)) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            ScreenAwakeManager.releaseWakeLock(this)
            com.example.service.KeepAwakeService.stop(this)
        }
        com.example.widget.BulbWidgetProvider.updateAllWidgets(this)
        com.example.widget.PowerWidgetProvider.updateAllWidgets(this)

        setContent {
            LuceSchermoTheme {
                val isActive by viewModel.isActive.collectAsStateWithLifecycle()

                // Keep screen on when app is in foreground and awake is active
                LaunchedEffect(isActive) {
                    if (isActive) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }

                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissions(this)
        if (!ScreenAwakeManager.isAwake(this)) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        com.example.widget.BulbWidgetProvider.updateAllWidgets(this)
        com.example.widget.PowerWidgetProvider.updateAllWidgets(this)
    }

    override fun onPause() {
        super.onPause()
        if (!ScreenAwakeManager.isAwake(this)) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val isActive by viewModel.isActive.collectAsStateWithLifecycle()
    val durationMinutes by viewModel.durationMinutes.collectAsStateWithLifecycle()
    val remainingSeconds by viewModel.remainingSeconds.collectAsStateWithLifecycle()
    val batteryProtection by viewModel.batteryProtection.collectAsStateWithLifecycle()
    val hapticEnabled by viewModel.hapticEnabled.collectAsStateWithLifecycle()
    val isScreenOffGranted by viewModel.isScreenOffGranted.collectAsStateWithLifecycle()
    val isLampMode by viewModel.isLampMode.collectAsStateWithLifecycle()
    val lampBrightness by viewModel.lampBrightness.collectAsStateWithLifecycle()
    val showPinSuccessDialog by viewModel.showPinSuccessDialog.collectAsStateWithLifecycle()

    var showGuideDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.checkPermissions(context)
    }

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Handled */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    if (isLampMode) {
        NightLampScreen(
            brightness = lampBrightness,
            onBrightnessChange = { viewModel.setLampBrightness(it) },
            onClose = { viewModel.setLampMode(false) }
        )
        return
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground,
                    titleContentColor = Color.White
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isActive) AmberPrimary else DarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LightMode,
                                contentDescription = null,
                                tint = if (isActive) Color.Black else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.setLampMode(true) },
                        modifier = Modifier.testTag("lamp_mode_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NightlightRound,
                            contentDescription = "Modalità Lampada",
                            tint = AmberSecondary
                        )
                    }
                    IconButton(
                        onClick = { showGuideDialog = true },
                        modifier = Modifier.testTag("info_guide_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Guida Widget",
                            tint = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hero Interactive Lightbulb Section
            item {
                BulbHeroCard(
                    isActive = isActive,
                    remainingSeconds = remainingSeconds,
                    onToggle = { viewModel.toggle(context) }
                )
            }

            // Duration Presets Section
            item {
                DurationPresetSection(
                    selectedDuration = durationMinutes,
                    onSelect = { minutes -> viewModel.setDuration(context, minutes) }
                )
            }

            // Home Screen Widget Highlight Card (with new Screen Off button)
            item {
                WidgetPromotionCard(
                    isPinSupported = viewModel.isPinWidgetSupported(context),
                    onPinWidget = { viewModel.requestPinWidget(context) },
                    onPinPowerWidget = { viewModel.requestPinPowerWidget(context) },
                    onOpenGuide = { showGuideDialog = true }
                )
            }

            // Immediate Screen Off Configuration & Test Card
            item {
                ScreenOffActionCard(
                    isGranted = isScreenOffGranted,
                    onTestScreenOff = { viewModel.testScreenOff(context) },
                    onConfigure = {
                        val intent = Intent(context, ScreenOffActivity::class.java)
                        context.startActivity(intent)
                    }
                )
            }

            // Quick Settings & Safeties Section
            item {
                SettingsSection(
                    batteryProtection = batteryProtection,
                    onBatteryProtectionChange = { viewModel.setBatteryProtection(context, it) },
                    hapticEnabled = hapticEnabled,
                    onHapticChange = { viewModel.setHapticEnabled(context, it) }
                )
            }

            // Quick Tile Guide
            item {
                QuickTileGuideCard()
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showGuideDialog) {
        WidgetGuideDialog(onDismiss = { showGuideDialog = false })
    }

    if (showPinSuccessDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissPinDialog() },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = AmberPrimary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Widget inviato alla Home!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Troverai il nuovo widget con 2 pulsanti: tocca la lampadina per mantenere lo schermo sempre attivo, o il tasto rosso per spegnere subito il display senza aprire l'app!",
                    color = Color(0xFFC9D1D9)
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissPinDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                ) {
                    Text("Perfetto", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun BulbHeroCard(
    isActive: Boolean,
    remainingSeconds: Long,
    onToggle: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "halo_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )

    val animatedScale by animateFloatAsState(
        targetValue = if (isActive) 1.02f else 0.98f,
        animationSpec = tween(300),
        label = "bulb_scale"
    )

    val cardBorderColor by animateColorAsState(
        targetValue = if (isActive) AmberPrimary.copy(alpha = 0.7f) else Color(0xFF30363D),
        animationSpec = tween(300),
        label = "border_color"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_bulb_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(
            colors = listOf(cardBorderColor, cardBorderColor.copy(alpha = 0.2f))
        ))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggle
                    )
                    .testTag("toggle_bulb_button"),
                contentAlignment = Alignment.Center
            ) {
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        AmberGlow.copy(alpha = haloAlpha),
                                        AmberPrimary.copy(alpha = haloAlpha * 0.5f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .scale(animatedScale)
                        .clip(CircleShape)
                        .background(
                            if (isActive) Color(0xFF281E00) else Color(0xFF1B2028)
                        )
                        .border(
                            width = if (isActive) 2.5.dp else 1.5.dp,
                            color = if (isActive) AmberPrimary else Color(0xFF384355),
                            shape = CircleShape
                        )
                        .shadow(
                            elevation = if (isActive) 24.dp else 4.dp,
                            shape = CircleShape,
                            spotColor = if (isActive) AmberPrimary else Color.Transparent
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(
                            if (isActive) R.drawable.img_bulb_on else R.drawable.img_bulb_off
                        ),
                        contentDescription = "Lampadina schermo sempre attivo",
                        modifier = Modifier.size(130.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(100.dp),
                color = if (isActive) AmberPrimary.copy(alpha = 0.15f) else Color(0xFF21262D),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isActive) AmberPrimary else Color(0xFF30363D)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isActive) StatusGreen else Color(0xFF8B949E))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isActive) "SCHERMO SEMPRE ATTIVO" else "TIMEOUT DI SISTEMA NORMALE",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) AmberPrimary else Color(0xFFC9D1D9),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isActive) "Tocca la lampadina per spegnere" else "Tocca la lampadina per accendere",
                fontSize = 14.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )

            AnimatedVisibility(visible = isActive && remainingSeconds > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(12.dp))
                    val mins = remainingSeconds / 60
                    val secs = remainingSeconds % 60
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF332500))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassBottom,
                            contentDescription = null,
                            tint = AmberPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = String.format("Spegnimento automatico tra %02d:%02d", mins, secs),
                            color = AmberSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DurationPresetSection(
    selectedDuration: Int,
    onSelect: (Int) -> Unit
) {
    val presets = listOf(
        0 to "Infinito",
        5 to "5 min",
        15 to "15 min",
        30 to "30 min",
        60 to "1 ora"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "DURATA DELLO SCHERMO ACCESO",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 0.8.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(presets) { (minutes, label) ->
                val isSelected = selectedDuration == minutes
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelect(minutes) },
                    label = {
                        Text(
                            text = label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AmberPrimary,
                        selectedLabelColor = Color.Black,
                        containerColor = DarkSurface,
                        labelColor = Color(0xFFC9D1D9)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) AmberPrimary else Color(0xFF30363D),
                        selectedBorderColor = AmberPrimary
                    ),
                    modifier = Modifier.testTag("duration_chip_$minutes")
                )
            }
        }
    }
}

@Composable
fun WidgetPromotionCard(
    isPinSupported: Boolean,
    onPinWidget: () -> Unit,
    onPinPowerWidget: () -> Unit,
    onOpenGuide: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("widget_promo_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131A26)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF2C394F), Color(0xFF1B2433))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1F2B3E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        tint = AmberPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Due Pulsanti nel Widget",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Lampadina (Sempre Attivo) + Tasto Spegni Schermo",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Widget visual demo preview card showing BOTH the bulb and the power off button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0C1017))
                    .border(1.dp, Color(0xFF212936), RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Left button: Lightbulb
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF281E00))
                            .border(1.5.dp, AmberPrimary, RoundedCornerShape(14.dp))
                            .padding(vertical = 10.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.img_bulb_on),
                            contentDescription = null,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "SEMPRE ATTIVO",
                            color = AmberPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "Tocca per timeout",
                            color = Color(0xFFFFD54F),
                            fontSize = 8.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Right button: Power Off
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFD32F2F))
                            .border(1.5.dp, Color(0xFFFF8A80), RoundedCornerShape(14.dp))
                            .padding(vertical = 10.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.img_power_off_btn),
                            contentDescription = null,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "SPEGNI SCHERMO",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "Spegne subito",
                            color = Color(0xFFFFCDD2),
                            fontSize = 8.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isPinSupported) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onPinWidget,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pin_widget_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Aggiungi Widget 2-in-1 (Lampadina + Spegni)",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onPinPowerWidget,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tasto Spegni (1x1)",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenGuide,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.testTag("open_guide_button")
                        ) {
                            Text(text = "Guida", fontSize = 12.sp)
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onOpenGuide,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_guide_button")
                ) {
                    Text(text = "Come Aggiungere i Widget alla Home", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun ScreenOffActionCard(
    isGranted: Boolean,
    onTestScreenOff: () -> Unit,
    onConfigure: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF3B2424), Color(0xFF221717))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF331515)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Spegnimento Immediato Schermo",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    Text(
                        text = if (isGranted) "Autorizzazione attiva" else "Richiede autorizzazione",
                        fontSize = 12.sp,
                        color = if (isGranted) StatusGreen else Color(0xFFFFB300)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Il pulsante di spegnimento presente sul widget spegne e blocca subito il display con 1 tocco, senza premere il pulsante fisico.",
                fontSize = 12.sp,
                color = Color(0xFFC9D1D9),
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onTestScreenOff,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Testa Spegnimento",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                if (!isGranted) {
                    OutlinedButton(
                        onClick = onConfigure,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text(text = "Autorizza", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSection(
    batteryProtection: Boolean,
    onBatteryProtectionChange: (Boolean) -> Unit,
    hapticEnabled: Boolean,
    onHapticChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF30363D), Color(0xFF21262D))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = "PREFERENZE E SICUREZZA",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Battery Saver Safeguard
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A1F1D)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryAlert,
                        contentDescription = null,
                        tint = Color(0xFFFF7B72),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Protezione Batteria (< 15%)",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Disattiva automaticamente a batteria scarica",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
                Switch(
                    checked = batteryProtection,
                    onCheckedChange = onBatteryProtectionChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AmberPrimary,
                        checkedTrackColor = Color(0xFF453000)
                    ),
                    modifier = Modifier.testTag("battery_switch")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Haptic Feedback
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E2838)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = null,
                        tint = AmberSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Feedback Aptico (Vibrazione)",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Vibra al tocco del widget o della lampadina",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
                Switch(
                    checked = hapticEnabled,
                    onCheckedChange = onHapticChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AmberPrimary,
                        checkedTrackColor = Color(0xFF453000)
                    ),
                    modifier = Modifier.testTag("haptic_switch")
                )
            }
        }
    }
}

@Composable
fun QuickTileGuideCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141923)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                listOf(Color(0xFF253042), Color(0xFF18202C))
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1C2433)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = AmberSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Disponibile anche nei Quick Settings",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = 13.sp
                )
                Text(
                    text = "Abbassa la tenda notifiche di Android > Modifica riquadri > Trascina il riquadro 'Schermo Attivo' per accendere ovunque.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
fun WidgetGuideDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Guida al Widget",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Chiudi",
                            tint = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                GuideStepItem(
                    step = "1",
                    title = "Tasto Lampadina (Schermo Attivo)",
                    desc = "Toccando la lampadina sul widget, attivi o disattivi lo schermo sempre acceso senza timeout."
                )

                Spacer(modifier = Modifier.height(12.dp))

                GuideStepItem(
                    step = "2",
                    title = "Tasto Spegni Schermo (Power Off)",
                    desc = "Toccando il pulsante rosso di spegnimento sul widget, lo schermo del telefono si spegne e si blocca all'istante."
                )

                Spacer(modifier = Modifier.height(12.dp))

                GuideStepItem(
                    step = "3",
                    title = "Accesso Rapido e Diretto",
                    desc = "Non serve aprire l'app né le impostazioni di sistema: entrambe le funzioni sono utilizzabili direttamente dalla schermata Home."
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                ) {
                    Text("Perfetto", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GuideStepItem(step: String, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(AmberPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                fontSize = 14.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                text = desc,
                fontSize = 12.sp,
                color = Color(0xFFC9D1D9),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun NightLampScreen(
    brightness: Float,
    onBrightnessChange: (Float) -> Unit,
    onClose: () -> Unit
) {
    val warmColor = Color(0xFFFFB300).copy(alpha = brightness.coerceIn(0.1f, 1.0f))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0A00))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* Keep active */ }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(warmColor, Color(0xFF1F1400), Color.Black)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.img_bulb_on),
                    contentDescription = null,
                    modifier = Modifier.size(180.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Modalità Lampada Ambientale",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = "Lo schermo rimane acceso con luce calda soffusa",
                    color = Color(0xFFFFE082),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x66000000))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NightlightRound,
                        contentDescription = null,
                        tint = AmberSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Slider(
                        value = brightness,
                        onValueChange = onBrightnessChange,
                        valueRange = 0.1f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = AmberPrimary,
                            activeTrackColor = AmberPrimary,
                            inactiveTrackColor = Color(0xFF554422)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Default.LightMode,
                        contentDescription = null,
                        tint = AmberPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF332500)),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text("Chiudi Lampada", color = AmberSecondary)
                }
            }
        }
    }
}
