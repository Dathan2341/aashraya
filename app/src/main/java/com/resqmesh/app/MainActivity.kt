package com.resqmesh.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.resqmesh.app.mesh.MeshForegroundService
import com.resqmesh.app.ui.MeshScreen
import com.resqmesh.app.ui.ResQMeshTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { grantResults ->
            if (hasMeshPermissions()) {
                viewModel.startMesh()
                MeshForegroundService.start(this)
            } else {
                viewModel.setPermissionError(
                    "Location and Bluetooth permissions are required to discover nearby phones."
                )
            }
        }

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            ResQMeshTheme {
                MeshScreen(
                    uiState = uiState,
                    onSend = { message -> viewModel.sendMessage(message) },
                    onSendSos = { viewModel.sendSosCall() },
                    onSendEmergency = { category, note -> viewModel.sendEmergencyCall(category, note) }
                )
            }
        }

        val missingPermissions = getRequiredPermissions().filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isEmpty()) {
            viewModel.startMesh()
            MeshForegroundService.start(this)
        } else {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }

    private fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf<String>()

        permissions += Manifest.permission.ACCESS_FINE_LOCATION
        permissions += Manifest.permission.ACCESS_COARSE_LOCATION

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions += Manifest.permission.BLUETOOTH_SCAN
            permissions += Manifest.permission.BLUETOOTH_ADVERTISE
            permissions += Manifest.permission.BLUETOOTH_CONNECT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions += Manifest.permission.NEARBY_WIFI_DEVICES
                permissions += Manifest.permission.POST_NOTIFICATIONS
            }
        } else {
            permissions += Manifest.permission.BLUETOOTH
            permissions += Manifest.permission.BLUETOOTH_ADMIN
            permissions += Manifest.permission.ACCESS_WIFI_STATE
            permissions += Manifest.permission.CHANGE_WIFI_STATE
        }

        return permissions.toTypedArray()
    }

    private fun hasMeshPermissions(): Boolean {
        return getRequiredPermissions()
            .filterNot { it == Manifest.permission.POST_NOTIFICATIONS }
            .all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    }
}
