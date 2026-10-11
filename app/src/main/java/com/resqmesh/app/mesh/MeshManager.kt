package com.resqmesh.app.mesh

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections
import kotlin.random.Random

class MeshManager(private val context: Context) {
    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val soundManager = SoundManager(context)
    private val locationHelper = LocationHelper(context)

    private val connectedPeers: MutableMap<String, String> = Collections.synchronizedMap(linkedMapOf<String, String>())
    private val pendingConnections: MutableSet<String> = Collections.synchronizedSet(mutableSetOf<String>())
    private val pendingNames: MutableMap<String, String> = Collections.synchronizedMap(mutableMapOf<String, String>())
    private val delayedConnectionRunnables: MutableMap<String, Runnable> = Collections.synchronizedMap(mutableMapOf<String, Runnable>())

    private val rawSeenSet = object : LinkedHashSet<String>() {
        override fun add(element: String): Boolean {
            if (size >= 2000 && !contains(element)) {
                val iterator = iterator()
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                }
            }
            return super.add(element)
        }
    }
    private val seenMessages: MutableSet<String> = Collections.synchronizedSet(rawSeenSet)

    private val _peers = MutableStateFlow<Map<String, String>>(emptyMap())
    val peers: StateFlow<Map<String, String>> = _peers.asStateFlow()

    private val _messages = MutableStateFlow<List<MeshMessage>>(emptyList())
    val messages: StateFlow<List<MeshMessage>> = _messages.asStateFlow()

    private val _status = MutableStateFlow("Starting...")
    val status: StateFlow<String> = _status.asStateFlow()

    val displayName: String = "${Build.MODEL}-${Random.nextInt(100, 999)}"
    private var started = false

    @SuppressLint("MissingPermission")
    fun start() {
        if (started) return
        started = true
        _status.value = "Starting mesh..."

        startAdvertisingInternal()
        startDiscoveryInternal()
    }

    @SuppressLint("MissingPermission")
    private fun startAdvertisingInternal() {
        if (!started) return
        connectionsClient.startAdvertising(
            displayName,
            SERVICE_ID,
            connectionLifecycleCallback,
            AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        ).addOnSuccessListener {
            Log.d(TAG, "Advertising started successfully")
            if (connectedPeers.isEmpty()) {
                _status.value = "Advertising & Searching..."
            }
        }.addOnFailureListener { e ->
            if (e is ApiException && e.statusCode == ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING) {
                Log.d(TAG, "Already advertising (STATUS_ALREADY_ADVERTISING)")
                return@addOnFailureListener
            }
            Log.e(TAG, "Advertising failed to start", e)
            _status.value = "Error starting mesh: ${e.localizedMessage ?: "Check BT & Location"}"
        }
    }

    @SuppressLint("MissingPermission")
    private fun startDiscoveryInternal() {
        if (!started) return
        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        ).addOnSuccessListener {
            Log.d(TAG, "Discovery started successfully")
            if (connectedPeers.isEmpty()) {
                _status.value = "Advertising & Searching..."
            }
        }.addOnFailureListener { e ->
            if (e is ApiException && e.statusCode == ConnectionsStatusCodes.STATUS_ALREADY_DISCOVERING) {
                Log.d(TAG, "Already discovering (STATUS_ALREADY_DISCOVERING)")
                return@addOnFailureListener
            }
            Log.e(TAG, "Discovery failed to start", e)
            _status.value = "Error discovering: ${e.localizedMessage ?: "Check BT & Location"}"
        }
    }

    fun stop() {
        started = false
        delayedConnectionRunnables.values.forEach { mainHandler.removeCallbacks(it) }
        delayedConnectionRunnables.clear()
        soundManager.release()
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        pendingConnections.clear()
        pendingNames.clear()
        connectedPeers.clear()
        _peers.value = emptyMap()
        _status.value = "Stopped"
    }

    fun setStatus(value: String) {
        _status.value = value
    }

    fun sendMessage(text: String): Boolean {
        if (text.isBlank()) return false
        if (connectedPeers.isEmpty()) {
            _status.value = "Waiting for nearby phones..."
            return false
        }

        val message = MeshMessage(
            sender = displayName,
            text = text,
            ttl = 5,
            hops = 0,
            type = MeshMessage.TYPE_CHAT
        )

        seenMessages.add(message.id)
        _messages.value = _messages.value + message
        val payload = Payload.fromBytes(message.toJson().toByteArray(Charsets.UTF_8))

        synchronized(connectedPeers) {
            connectedPeers.keys.forEach { endpointId ->
                connectionsClient.sendPayload(endpointId, payload)
            }
        }

        _status.value = "Connected to ${connectedPeers.size}"
        return true
    }

    fun sendSosCall() {
        sendEmergencyCall(MeshMessage.TYPE_TRAPPED)
    }

    fun sendEmergencyCall(categoryType: String, customNote: String = "") {
        // 1. Play high-pitch emergency alarm sound & vibration locally
        soundManager.playEmergencyAlarm()

        // 2. Fetch offline GPS location and broadcast distress signal
        locationHelper.getCurrentLocation { lat, lng ->
            val defaultHeading = when (categoryType) {
                MeshMessage.TYPE_TRAPPED -> "🏗️ TRAPPED! Victim under collapse/debris! Urgent extraction needed!"
                MeshMessage.TYPE_STARVING -> "🍞 STARVING! Critical need for food and clean drinking water!"
                MeshMessage.TYPE_MEDICAL -> "🩺 URGENT MEDICAL CARE! Severe injury/medical emergency!"
                else -> "🚨 EMERGENCY DISTRESS CALL! Immediate Help Needed!"
            }

            val finalNote = if (customNote.isNotBlank()) "$defaultHeading ($customNote)" else defaultHeading
            val locSuffix = if (lat != null && lng != null) " [GPS: ${String.format("%.4f", lat)}, ${String.format("%.4f", lng)}]" else ""

            val sosMessage = MeshMessage(
                sender = displayName,
                text = finalNote + locSuffix,
                ttl = 5,
                hops = 0,
                type = categoryType,
                lat = lat,
                lng = lng
            )

            seenMessages.add(sosMessage.id)
            _messages.value = _messages.value + sosMessage

            if (connectedPeers.isNotEmpty()) {
                val payload = Payload.fromBytes(sosMessage.toJson().toByteArray(Charsets.UTF_8))
                synchronized(connectedPeers) {
                    connectedPeers.keys.forEach { endpointId ->
                        connectionsClient.sendPayload(endpointId, payload)
                    }
                }
            } else {
                _status.value = "SOS Broadcast queued! Searching for nearby phones..."
            }
        }
    }

    private val endpointDiscoveryCallback: EndpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Log.d(TAG, "Discovered endpoint: ${info.endpointName} ($endpointId)")
            if (connectedPeers.containsKey(endpointId) || pendingConnections.contains(endpointId)) {
                return
            }

            pendingConnections.add(endpointId)
            pendingNames[endpointId] = info.endpointName

            val requestRunnable = Runnable {
                delayedConnectionRunnables.remove(endpointId)
                if (connectedPeers.containsKey(endpointId)) return@Runnable

                Log.d(TAG, "Initiating connection request to ${info.endpointName} ($endpointId)")
                connectionsClient.requestConnection(displayName, endpointId, connectionLifecycleCallback)
                    .addOnSuccessListener {
                        Log.d(TAG, "Requested connection to ${info.endpointName}")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Request connection result for ${info.endpointName}: ${e.message}")
                        if (!connectedPeers.containsKey(endpointId)) {
                            pendingConnections.remove(endpointId)
                            pendingNames.remove(endpointId)
                            scheduleRetryDiscovery()
                        }
                    }
            }

            val delayMs = if (displayName < info.endpointName) 0L else 1500L
            if (delayMs == 0L) {
                requestRunnable.run()
            } else {
                Log.d(TAG, "Waiting 1.5s for ${info.endpointName} to initiate connection")
                delayedConnectionRunnables[endpointId] = requestRunnable
                mainHandler.postDelayed(requestRunnable, delayMs)
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Endpoint lost: $endpointId")
            val pendingRunnable = delayedConnectionRunnables.remove(endpointId)
            if (pendingRunnable != null) {
                mainHandler.removeCallbacks(pendingRunnable)
            }
            if (!connectedPeers.containsKey(endpointId)) {
                pendingConnections.remove(endpointId)
                pendingNames.remove(endpointId)
            }
        }
    }

    private val connectionLifecycleCallback: ConnectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            Log.d(TAG, "Connection initiated by ${info.endpointName} ($endpointId)")
            val pendingRunnable = delayedConnectionRunnables.remove(endpointId)
            if (pendingRunnable != null) {
                mainHandler.removeCallbacks(pendingRunnable)
            }

            pendingConnections.add(endpointId)
            pendingNames[endpointId] = info.endpointName

            connectionsClient.acceptConnection(endpointId, payloadCallback)
                .addOnFailureListener { e ->
                    Log.e(TAG, "Accept connection failed for $endpointId", e)
                    pendingConnections.remove(endpointId)
                    pendingNames.remove(endpointId)
                }
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            Log.d(TAG, "Connection result for $endpointId: ${result.status.statusCode}")
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    val peerName = pendingNames.remove(endpointId) ?: "Peer"
                    pendingConnections.remove(endpointId)
                    connectedPeers[endpointId] = peerName
                    _peers.value = HashMap(connectedPeers)
                    _status.value = "Connected to ${connectedPeers.size}"
                    scheduleRetryDiscovery()
                }
                else -> {
                    pendingNames.remove(endpointId)
                    pendingConnections.remove(endpointId)
                    connectedPeers.remove(endpointId)
                    _peers.value = HashMap(connectedPeers)
                    _status.value = if (connectedPeers.isEmpty()) {
                        "Searching for nearby phones..."
                    } else {
                        "Connected to ${connectedPeers.size}"
                    }
                    scheduleRetryDiscovery()
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.d(TAG, "Disconnected from $endpointId")
            connectedPeers.remove(endpointId)
            pendingConnections.remove(endpointId)
            pendingNames.remove(endpointId)
            _peers.value = HashMap(connectedPeers)
            _status.value = if (connectedPeers.isEmpty()) {
                "Waiting for nearby phones..."
            } else {
                "Connected to ${connectedPeers.size}"
            }

            scheduleRetryDiscovery()
        }
    }

    private fun scheduleRetryDiscovery() {
        if (!started) return
        mainHandler.postDelayed({
            if (!started) return@postDelayed
            connectionsClient.stopDiscovery()
            mainHandler.postDelayed({
                if (!started) return@postDelayed
                startDiscoveryInternal()
            }, 500L)
        }, 1500L)
    }

    private val payloadCallback: PayloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type != Payload.Type.BYTES) return
            val bytes = payload.asBytes() ?: return
            val message = MeshMessage.fromJson(String(bytes, Charsets.UTF_8)) ?: return
            handleIncomingMessage(message, endpointId)
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: com.google.android.gms.nearby.connection.PayloadTransferUpdate) = Unit
    }

    private fun handleIncomingMessage(message: MeshMessage, fromEndpointId: String) {
        if (!seenMessages.add(message.id)) {
            Log.d(TAG, "Duplicate message dropped: ${message.id}")
            return
        }

        // Trigger loud emergency alarm sound & vibration if message is an emergency type!
        if (message.type == MeshMessage.TYPE_TRAPPED || message.type == MeshMessage.TYPE_STARVING ||
            message.type == MeshMessage.TYPE_MEDICAL || message.type == MeshMessage.TYPE_SOS) {
            soundManager.playEmergencyAlarm()
            EmergencyNotificationHelper.showEmergencyNotification(context, message)
        }

        _messages.value = _messages.value + message

        if (message.ttl - 1 > 0) {
            val forwardedMessage = message.copy(
                ttl = message.ttl - 1,
                hops = message.hops + 1
            )
            val payload = Payload.fromBytes(forwardedMessage.toJson().toByteArray(Charsets.UTF_8))
            synchronized(connectedPeers) {
                connectedPeers.keys.filter { it != fromEndpointId }.forEach { endpointId ->
                    connectionsClient.sendPayload(endpointId, payload)
                }
            }
        }
    }

    companion object {
        @Volatile
        private var instance: MeshManager? = null

        fun getInstance(context: Context): MeshManager {
            return instance ?: synchronized(this) {
                instance ?: MeshManager(context.applicationContext).also { instance = it }
            }
        }

        private const val TAG = "ResQMesh"
        private const val SERVICE_ID = "com.resqmesh.app"
    }
}
