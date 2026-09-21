package com.amol.amr.communication

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import com.amol.amr.navigation.NavigationCommand
import com.amol.amr.util.Logger
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException

enum class UsbConnectionState {
    DISCONNECTED,
    CONNECTING,
    PERMISSION_REQUIRED,
    CONNECTED,
    ERROR
}

class UsbSerialManager(
    private val context: Context,
    private val baudRate: Int = 115200
) {
    companion object {
        private const val ACTION_USB_PERMISSION = "com.amol.amr.USB_PERMISSION"
    }

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var serialPort: UsbSerialPort? = null
    private val writeMutex = Mutex()
    private var sequenceNumber: Long = 0L

    private val _connectionState = MutableStateFlow(UsbConnectionState.DISCONNECTED)
    val connectionState: StateFlow<UsbConnectionState> = _connectionState.asStateFlow()

    private val _lastSentCommand = MutableStateFlow<String>("")
    val lastSentCommand: StateFlow<String> = _lastSentCommand.asStateFlow()

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (ACTION_USB_PERMISSION == intent?.action) {
                synchronized(this) {
                    val device: UsbDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.let { openDevice(it) }
                    } else {
                        Logger.w("UsbSerialManager", "USB Permission denied for device $device")
                        _connectionState.value = UsbConnectionState.PERMISSION_REQUIRED
                    }
                }
            } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == intent?.action) {
                disconnect()
            }
        }
    }

    init {
        val filter = IntentFilter(ACTION_USB_PERMISSION).apply {
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(usbReceiver, filter)
        }
    }

    fun connect(): Boolean {
        _connectionState.value = UsbConnectionState.CONNECTING
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty()) {
            Logger.w("UsbSerialManager", "No USB Serial devices found")
            _connectionState.value = UsbConnectionState.DISCONNECTED
            return false
        }

        val driver = availableDrivers[0]
        val device = driver.device

        if (!usbManager.hasPermission(device)) {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val permissionIntent = PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), flags)
            _connectionState.value = UsbConnectionState.PERMISSION_REQUIRED
            usbManager.requestPermission(device, permissionIntent)
            return false
        }

        return openDevice(device)
    }

    private fun openDevice(device: UsbDevice): Boolean {
        try {
            val driver = UsbSerialProber.getDefaultProber().probeDevice(device) ?: return false
            val connection = usbManager.openDevice(driver.device) ?: return false

            val port = driver.ports[0]
            port.open(connection)
            port.setParameters(baudRate, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
            port.dtr = true
            port.rts = true

            serialPort = port
            _connectionState.value = UsbConnectionState.CONNECTED
            Logger.i("UsbSerialManager", "Connected to ESP32-S3 on port 0 at $baudRate baud")
            return true
        } catch (e: Exception) {
            Logger.e("UsbSerialManager", "Failed to open USB device", e)
            _connectionState.value = UsbConnectionState.ERROR
            return false
        }
    }

    suspend fun sendCommand(command: NavigationCommand): Result<Unit> = withContext(Dispatchers.IO) {
        val port = serialPort
        if (port == null || _connectionState.value != UsbConnectionState.CONNECTED) {
            return@withContext Result.failure(IOException("USB Serial Port is not connected"))
        }

        writeMutex.withLock {
            try {
                sequenceNumber++
                val packetString = command.toProtocolString(sequenceNumber)
                val bytes = packetString.toByteArray(Charsets.US_ASCII)
                port.write(bytes, 200) // 200ms write timeout
                _lastSentCommand.value = packetString.trim()
                Result.success(Unit)
            } catch (e: Exception) {
                Logger.e("UsbSerialManager", "USB write failed", e)
                _connectionState.value = UsbConnectionState.ERROR
                Result.failure(e)
            }
        }
    }

    fun disconnect() {
        try {
            serialPort?.close()
        } catch (e: Exception) {
            Logger.e("UsbSerialManager", "Error closing USB port", e)
        } finally {
            serialPort = null
            _connectionState.value = UsbConnectionState.DISCONNECTED
        }
    }

    fun release() {
        disconnect()
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (_: Exception) {}
    }
}
