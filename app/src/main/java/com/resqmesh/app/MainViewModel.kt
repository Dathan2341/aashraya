package com.resqmesh.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.resqmesh.app.mesh.MeshManager
import com.resqmesh.app.ui.MeshUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val meshManager = MeshManager.getInstance(application.applicationContext)

    val uiState: StateFlow<MeshUiState> = combine(
        meshManager.peers,
        meshManager.messages,
        meshManager.status
    ) { peers, messages, status ->
        MeshUiState(
            peerMap = peers,
            messages = messages,
            status = status,
            localDisplayName = meshManager.displayName
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = MeshUiState(localDisplayName = meshManager.displayName)
    )

    fun startMesh() {
        meshManager.start()
    }

    fun sendMessage(text: String) {
        meshManager.sendMessage(text)
    }

    fun sendSosCall() {
        meshManager.sendSosCall()
    }

    fun sendEmergencyCall(categoryType: String, customNote: String = "") {
        meshManager.sendEmergencyCall(categoryType, customNote)
    }

    fun setPermissionError(message: String) {
        meshManager.setStatus("Error: $message")
    }

}
