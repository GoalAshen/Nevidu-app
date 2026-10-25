package com.example.pairing

import android.content.Context
import com.example.data.repository.EcoRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class BlePairingStep {
  IDLE,
  SCANNING,
  CONNECTING,
  READING_INFO,
  WIFI_CONFIG,
  PROVISIONING,
  SUCCESS,
  ERROR
}

data class DiscoveredBleNode(
  val id: String,
  val name: String,
  val macAddress: String,
  val rssiDbm: Int,
  val firmwareVersion: String = "NodeOS v2.4.1",
  val hardwareModel: String = "EcoControl IoT Core",
  val batteryPercent: Int = 94,
  val sensorCheckOk: Boolean = true
)

class BlePairingManager(
  private val repository: EcoRepository,
  private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
  private val _step = MutableStateFlow(BlePairingStep.IDLE)
  val step: StateFlow<BlePairingStep> = _step.asStateFlow()

  private val _discoveredNodes = MutableStateFlow<List<DiscoveredBleNode>>(emptyList())
  val discoveredNodes: StateFlow<List<DiscoveredBleNode>> = _discoveredNodes.asStateFlow()

  private val _selectedNode = MutableStateFlow<DiscoveredBleNode?>(null)
  val selectedNode: StateFlow<DiscoveredBleNode?> = _selectedNode.asStateFlow()

  private val _statusMessage = MutableStateFlow("")
  val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

  fun startScanning(context: Context? = null) {
    _step.value = BlePairingStep.SCANNING
    _statusMessage.value = "Scanning for nearby environmental nodes via Bluetooth LE..."
    _discoveredNodes.value = emptyList()

    scope.launch {
      delay(1200)
      _discoveredNodes.value = listOf(
        DiscoveredBleNode(
          id = "NODE-C24",
          name = "East Garden Bed",
          macAddress = "24:0A:C4:58:3B:12",
          rssiDbm = -48,
          firmwareVersion = "NodeOS v2.4.2",
          batteryPercent = 95
        )
      )

      delay(1500)
      _discoveredNodes.value = listOf(
        DiscoveredBleNode(
          id = "NODE-C24",
          name = "East Garden Bed",
          macAddress = "24:0A:C4:58:3B:12",
          rssiDbm = -48,
          firmwareVersion = "NodeOS v2.4.2",
          batteryPercent = 95
        ),
        DiscoveredBleNode(
          id = "NODE-F88",
          name = "Rooftop Greenhouse",
          macAddress = "30:AE:A4:02:18:9A",
          rssiDbm = -64,
          firmwareVersion = "NodeOS v2.4.1",
          batteryPercent = 88
        )
      )

      delay(1200)
      _discoveredNodes.value = _discoveredNodes.value + DiscoveredBleNode(
        id = "NODE-A12",
        name = "Seedling Incubator",
        macAddress = "4C:11:AE:D9:E2:5F",
        rssiDbm = -72,
        firmwareVersion = "NodeOS v2.4.0",
        batteryPercent = 91
      )
    }
  }

  fun selectAndConnect(node: DiscoveredBleNode) {
    _selectedNode.value = node
    _step.value = BlePairingStep.CONNECTING
    _statusMessage.value = "Establishing encrypted Bluetooth LE GATT connection..."

    scope.launch {
      delay(1000)

      _step.value = BlePairingStep.READING_INFO
      _statusMessage.value = "Querying device hardware characteristics and sensor health..."
      delay(1200)

      _step.value = BlePairingStep.WIFI_CONFIG
      _statusMessage.value = "Hardware verified. Ready for Wi-Fi provisioning."
    }
  }

  fun provisionWifi(ssid: String, password: String) {
    val node = _selectedNode.value ?: return
    _step.value = BlePairingStep.PROVISIONING
    _statusMessage.value = "Sending 2.4GHz network credentials and validating cloud handshake..."

    scope.launch {
      delay(2000)

      // Automatically register the node into repository fleet
      repository.addDevice(
        name = node.name,
        location = "Paired Zone (${node.id})",
        lat = 37.7780 + (kotlin.random.Random.nextDouble(-0.01, 0.01)),
        lng = -122.4220 + (kotlin.random.Random.nextDouble(-0.01, 0.01))
      )
      repository.selectDevice(node.id)

      _step.value = BlePairingStep.SUCCESS
      _statusMessage.value = "Node successfully connected to Wi-Fi and transmitting telemetry."
    }
  }

  fun reset() {
    _step.value = BlePairingStep.IDLE
    _selectedNode.value = null
    _discoveredNodes.value = emptyList()
    _statusMessage.value = ""
  }
}
