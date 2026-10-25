package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.localization.EcoLanguage
import com.example.localization.TranslationStrings
import com.example.pairing.BlePairingStep
import com.example.pairing.DiscoveredBleNode
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.EcoViewModel
import com.example.util.NotificationHelper

enum class EcoTab(val icon: androidx.compose.ui.graphics.vector.ImageVector) {
  DASHBOARD(Icons.Default.Dashboard),
  CONTROLS(Icons.Default.Tune),
  CHARTS(Icons.Default.ShowChart),
  PAIR_NODE(Icons.Default.BluetoothSearching),
  SETTINGS(Icons.Default.Settings);

  fun getLabel(strings: TranslationStrings): String = when (this) {
    DASHBOARD -> strings.navDashboard
    CONTROLS -> strings.navControls
    CHARTS -> strings.navCharts
    PAIR_NODE -> strings.navPair
    SETTINGS -> strings.navSettings
  }
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    NotificationHelper.createNotificationChannel(this)

    setContent {
      val systemInDark = isSystemInDarkTheme()
      var isDarkTheme by remember { mutableStateOf(systemInDark) }

      EcoControlTheme(darkTheme = isDarkTheme) {
        EcoControlApp(
          isDarkTheme = isDarkTheme,
          onToggleDarkTheme = { isDarkTheme = !isDarkTheme }
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcoControlApp(
  isDarkTheme: Boolean,
  onToggleDarkTheme: () -> Unit,
  viewModel: EcoViewModel = viewModel()
) {
  val context = LocalContext.current
  var currentTab by remember { mutableStateOf(EcoTab.DASHBOARD) }
  var showDeviceSwitcherModal by remember { mutableStateOf(false) }

  // Localization States
  val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
  val strings by viewModel.translationStrings.collectAsStateWithLifecycle()
  val showWelcomeModal by viewModel.showWelcomeLanguageModal.collectAsStateWithLifecycle()

  // BLE Pairing States
  val pairingStep by viewModel.pairingStep.collectAsStateWithLifecycle()
  val discoveredBleNodes by viewModel.discoveredBleNodes.collectAsStateWithLifecycle()
  val selectedDiscoveredNode by viewModel.selectedDiscoveredNode.collectAsStateWithLifecycle()
  val pairingStatusMessage by viewModel.pairingStatusMessage.collectAsStateWithLifecycle()

  // Telemetry & Control States
  val selectedDevice by viewModel.selectedDevice.collectAsStateWithLifecycle()
  val allDevices by viewModel.allDevices.collectAsStateWithLifecycle()
  val sensorData by viewModel.currentSensorData.collectAsStateWithLifecycle()
  val plantHealth by viewModel.currentPlantHealth.collectAsStateWithLifecycle()
  val relayState by viewModel.currentRelayState.collectAsStateWithLifecycle()
  val thresholds by viewModel.currentThresholds.collectAsStateWithLifecycle()
  val hardwareControls by viewModel.currentHardwareControls.collectAsStateWithLifecycle()
  val schedule by viewModel.currentSchedule.collectAsStateWithLifecycle()
  val history by viewModel.currentHistory.collectAsStateWithLifecycle()
  val alerts by viewModel.alerts.collectAsStateWithLifecycle()
  val eventLogs by viewModel.eventLogs.collectAsStateWithLifecycle()
  val firebaseConfig by viewModel.firebaseConfig.collectAsStateWithLifecycle()
  val connectionError by viewModel.connectionError.collectAsStateWithLifecycle()
  val isOnline by viewModel.isCurrentDeviceOnline.collectAsStateWithLifecycle()
  val notificationsEnabled by viewModel.systemNotificationsEnabled.collectAsStateWithLifecycle()

  // Notification runtime permission launcher (Android 13+)
  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      NotificationHelper.sendTestNotification(context)
    }
  }

  // Bluetooth runtime permissions launcher (Android 12+ / Location on legacy)
  val blePermissionsLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions()
  ) {
    viewModel.startBleScan(context)
  }

  fun requestScan() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      blePermissionsLauncher.launch(
        arrayOf(
          Manifest.permission.BLUETOOTH_SCAN,
          Manifest.permission.BLUETOOTH_CONNECT
        )
      )
    } else {
      blePermissionsLauncher.launch(
        arrayOf(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION
        )
      )
    }
  }

  // Initialize context for repository and notifications
  LaunchedEffect(Unit) {
    viewModel.setAppContext(context.applicationContext)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  val listState = rememberLazyListState()

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Surface(
              shape = CircleShape,
              color = EcoEmerald.copy(alpha = 0.2f),
              modifier = Modifier.size(34.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Eco,
                  contentDescription = "Logo",
                  tint = EcoEmerald,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
            Column {
              Text(
                text = "EcoControl",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp
              )
              Text(
                text = strings.appSubtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        },
        actions = {
          // Language Switcher Pill
          Surface(
            onClick = { viewModel.showLanguagePicker() },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(end = 4.dp).testTag("top_bar_language_pill")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Text(
                text = "${currentLanguage.flagEmoji} ${currentLanguage.nativeName.take(6)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
              Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Language",
                modifier = Modifier.size(16.dp)
              )
            }
          }

          // Device Quick Select Pill
          Surface(
            onClick = { showDeviceSwitcherModal = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(end = 4.dp).testTag("top_bar_device_pill")
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .background(if (isOnline) EcoEmerald else EcoRed, CircleShape)
              )
              Text(
                text = selectedDevice?.name?.take(10) ?: strings.navPair,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
              Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
            }
          }

          // Alerts badge button (takes to Settings tab)
          IconButton(
            onClick = { currentTab = EcoTab.SETTINGS },
            modifier = Modifier.testTag("top_bar_alerts_button")
          ) {
            BadgedBox(
              badge = {
                if (alerts.isNotEmpty()) {
                  Badge(containerColor = EcoRed) {
                    Text("${alerts.size}", fontSize = 10.sp)
                  }
                }
              }
            ) {
              Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Alerts",
                tint = if (alerts.isNotEmpty()) EcoRed else MaterialTheme.colorScheme.onSurface
              )
            }
          }

          // Dark/Light Theme Switch
          IconButton(
            onClick = onToggleDarkTheme,
            modifier = Modifier.testTag("top_bar_theme_toggle")
          ) {
            Icon(
              imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
              contentDescription = "Toggle Theme",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surface
        )
      )
    },
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
      ) {
        EcoTab.entries.forEach { tab ->
          val selected = currentTab == tab
          val tabLabel = tab.getLabel(strings)
          NavigationBarItem(
            selected = selected,
            onClick = { currentTab = tab },
            icon = {
              if (tab == EcoTab.SETTINGS && alerts.isNotEmpty()) {
                BadgedBox(badge = { Badge(containerColor = EcoRed) { Text("${alerts.size}") } }) {
                  Icon(imageVector = tab.icon, contentDescription = tabLabel)
                }
              } else {
                Icon(imageVector = tab.icon, contentDescription = tabLabel)
              }
            },
            label = {
              Text(
                text = tabLabel,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1
              )
            },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = Color.Black,
              indicatorColor = EcoEmerald,
              selectedTextColor = EcoEmerald,
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.testTag("nav_item_${tab.name}")
          )
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      LazyColumn(
        state = listState,
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Clear Error Banner for Firebase Auth / Connection Failures
        connectionError?.let { errorMsg ->
          item {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.errorContainer,
              border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error)
              ),
              modifier = Modifier.fillMaxWidth().testTag("firebase_connection_error_banner")
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Connection Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp)
                  )
                  Text(
                    text = "Firebase Connection or Auth Alert",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                  )
                }
                Text(
                  text = errorMsg,
                  fontSize = 12.sp,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  lineHeight = 16.sp
                )
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.End,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  TextButton(
                    onClick = { viewModel.dismissConnectionError() }
                  ) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
                  }
                  Spacer(modifier = Modifier.width(8.dp))
                  Button(
                    onClick = { currentTab = EcoTab.SETTINGS },
                    colors = ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.error,
                      contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                  ) {
                    Text("Fix Credentials", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }

        // Heartbeat Status Header (Shown on all main tabs except pair)
        if (currentTab != EcoTab.PAIR_NODE) {
          item {
            HeartbeatStatusHeader(
              device = selectedDevice,
              sensorData = sensorData,
              isOnline = isOnline,
              isDemoMode = firebaseConfig.isDemoMode,
              onDeviceSwitchClick = { showDeviceSwitcherModal = true }
            )
          }
        }

        // Active Alert Snippet on Dashboard
        if (currentTab == EcoTab.DASHBOARD && alerts.isNotEmpty()) {
          item {
            Surface(
              onClick = { currentTab = EcoTab.SETTINGS },
              shape = RoundedCornerShape(14.dp),
              color = EcoRed.copy(alpha = 0.12f),
              border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(EcoRed.copy(alpha = 0.5f))
              ),
              modifier = Modifier.fillMaxWidth().testTag("dashboard_alerts_alert_banner")
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = EcoRed)
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${alerts.size} Active System Alerts",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = EcoRed
                  )
                  Text(
                    text = alerts.first().title,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
                Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = EcoRed)
              }
            }
          }
        }

        when (currentTab) {
          EcoTab.DASHBOARD -> {
            // Plant Health Vitality Card (Evaluated from Firebase Sensor Telemetry)
            item {
              PlantHealthCard(
                plantHealth = plantHealth,
                isRelayOn = relayState?.isPumpOn == true,
                onToggleRelay = { viewModel.togglePump(it, context) }
              )
            }

            // SENSORS TO DISPLAY (Read from Firebase):
            // - Temperature and humidity
            item {
              TempHumidityCard(
                tempC = sensorData?.temperatureC ?: 24.5,
                humidity = sensorData?.humidityPercent ?: 60.0
              )
            }

            // - 3 soil moisture zones as gauges with %
            item {
              SoilMoistureGaugesCard(
                zone1 = sensorData?.soilZone1 ?: 45,
                zone2 = sensorData?.soilZone2 ?: 30,
                zone3 = sensorData?.soilZone3 ?: 75,
                dryThreshold = thresholds?.soilDryPercent ?: 35
              )
            }

            // - Light level & Rain: Dry / Raining
            item {
              LightAndRainCard(
                lightLux = sensorData?.lightLux ?: 45000,
                lightPercent = sensorData?.lightPercent ?: 45,
                isRaining = sensorData?.isRaining ?: false,
                onToggleRain = { viewModel.toggleRainSensor(context) }
              )
            }

            // - Battery % and solar charging status
            item {
              PowerSystemCard(
                batteryPercent = sensorData?.batteryPercent ?: 88,
                solarCharging = sensorData?.solarCharging ?: true
              )
            }

            // Quick Relay Switch on Dashboard
            item {
              RelayControlCard(
                relayState = relayState,
                thresholds = thresholds,
                currentSoilAvg = sensorData?.soilAverage ?: 50,
                isRaining = sensorData?.isRaining ?: false,
                onTogglePump = { viewModel.togglePump(it, context) },
                onSetMode = { viewModel.setRelayMode(it, context) }
              )
            }
          }

          EcoTab.CONTROLS -> {
            // 1. Device Node Power Control (Turn OFF / ON device node)
            item {
              NodePowerCard(
                device = selectedDevice,
                isPoweredOn = selectedDevice?.isPoweredOn ?: true,
                onTogglePower = { viewModel.toggleDevicePower(it, context) }
              )
            }

            // 2. Relay Switch (Turn ON / OFF Relay)
            item {
              RelayControlCard(
                relayState = relayState,
                thresholds = thresholds,
                currentSoilAvg = sensorData?.soilAverage ?: 50,
                isRaining = sensorData?.isRaining ?: false,
                onTogglePump = { viewModel.togglePump(it, context) },
                onSetMode = { viewModel.setRelayMode(it, context) }
              )
            }

            // 3. Plant Health & Irrigation Status
            item {
              PlantHealthCard(
                plantHealth = plantHealth,
                isRelayOn = relayState?.isPumpOn == true,
                onToggleRelay = { viewModel.togglePump(it, context) }
              )
            }

            // 4. On-Device Autonomous Firmware Limits & Triggers
            item {
              HardwareFirmwareLimitsCard(
                sensorData = sensorData,
                thresholds = thresholds
              )
            }

            // 5. Daily Schedule for relay (time + duration)
            item {
              DailyScheduleCard(
                schedule = schedule ?: com.example.data.model.DailySchedule(),
                onUpdateSchedule = { viewModel.updateSchedule(it) }
              )
            }
          }

          EcoTab.CHARTS -> {
            // Dedicated Sensor History Graph Card
            item {
              SensorHistoryGraphCard(historyPoints = history)
            }

            // Telemetry multi-stream chart
            item {
              TelemetryChartCard(historyPoints = history)
            }

            // Soil Moisture Gauge Overview
            item {
              SoilMoistureGaugesCard(
                zone1 = sensorData?.soilZone1 ?: 45,
                zone2 = sensorData?.soilZone2 ?: 30,
                zone3 = sensorData?.soilZone3 ?: 75,
                dryThreshold = thresholds?.soilDryPercent ?: 35
              )
            }
          }

          EcoTab.PAIR_NODE -> {
            // BLE Scanning, Device Info, Diagnostics & Wi-Fi Provisioning
            item {
              NodePairingScreen(
                pairingStep = pairingStep,
                discoveredNodes = discoveredBleNodes,
                selectedNode = selectedDiscoveredNode,
                statusMessage = pairingStatusMessage,
                strings = strings,
                onStartScan = { requestScan() },
                onSelectNode = { viewModel.selectBleNode(it) },
                onProvisionWifi = { ssid, pass -> viewModel.provisionNodeWifi(ssid, pass) },
                onFinish = {
                  selectedDiscoveredNode?.let { viewModel.selectDevice(it.id) }
                  currentTab = EcoTab.DASHBOARD
                },
                onReset = { viewModel.resetPairing() }
              )
            }
          }

          EcoTab.SETTINGS -> {
            // Settings & System Management
            // 1. Alerts & Logs
            item {
              AlertsAndLogsView(
                alerts = alerts,
                eventLogs = eventLogs,
                notificationsEnabled = notificationsEnabled,
                onToggleNotifications = { viewModel.toggleSystemNotifications(it, context) },
                onSendTestNotification = {
                  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                  }
                  viewModel.triggerTestNotification(context)
                },
                onDismissAlert = { viewModel.dismissAlert(it) },
                onExportCsv = { viewModel.exportCsv(context) },
                onClearLogs = { viewModel.clearEventLogs(context) }
              )
            }

            // 2. Device Manager & Map
            item {
              DeviceManagerAndMap(
                devices = allDevices,
                selectedDeviceId = selectedDevice?.id ?: "",
                onSelectDevice = { viewModel.selectDevice(it) },
                onAddDevice = { name, loc, lat, lng -> viewModel.addDevice(name, loc, lat, lng) },
                onUpdateLocation = { id, lat, lng, loc -> viewModel.updateDeviceLocation(id, lat, lng, loc) }
              )
            }

            // 3. Backend & Cloud Firebase Configuration
            item {
              FirebaseConfigCard(
                config = firebaseConfig,
                isHeartbeatPaused = firebaseConfig.simulateHeartbeatTimeout,
                onSaveConfig = { viewModel.saveFirebaseSettings(it, context) },
                onToggleDemoMode = { viewModel.toggleDemoMode(it) },
                onToggleSimulateHeartbeatTimeout = { viewModel.toggleSimulateHeartbeatTimeout(context) }
              )
            }
          }
        }
      }
    }
  }

  // Welcome / Language Selection Dialog
  if (showWelcomeModal) {
    WelcomeLanguageModal(
      currentLanguage = currentLanguage,
      onSelectLanguage = { viewModel.setLanguage(it) },
      onDismiss = { viewModel.dismissWelcomeLanguageModal() }
    )
  }

  // Device Quick Switch Modal Sheet
  if (showDeviceSwitcherModal) {
    AlertDialog(
      onDismissRequest = { showDeviceSwitcherModal = false },
      title = {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(imageVector = Icons.Default.Sensors, contentDescription = null, tint = EcoEmerald)
          Text(text = "Select EcoControl Node", fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          allDevices.forEach { dev ->
            val isSelected = dev.id == selectedDevice?.id
            Surface(
              onClick = {
                viewModel.selectDevice(dev.id)
                showDeviceSwitcherModal = false
              },
              shape = RoundedCornerShape(10.dp),
              color = if (isSelected) EcoEmerald.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.fillMaxWidth().testTag("modal_select_device_${dev.id}")
            ) {
              Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column {
                  Text(text = dev.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                  Text(text = "${dev.id} • ${dev.locationName}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (isSelected) {
                  Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = EcoEmerald)
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showDeviceSwitcherModal = false }) {
          Text("Close")
        }
      }
    )
  }
}
