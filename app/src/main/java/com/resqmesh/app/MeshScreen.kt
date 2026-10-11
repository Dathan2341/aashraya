package com.resqmesh.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resqmesh.app.mesh.MeshMessage

data class MeshUiState(
    val peerMap: Map<String, String> = emptyMap(),
    val messages: List<MeshMessage> = emptyList(),
    val status: String = "Starting...",
    val localDisplayName: String = ""
)

private fun isEmergencyType(type: String): Boolean {
    return type == MeshMessage.TYPE_TRAPPED ||
        type == MeshMessage.TYPE_STARVING ||
        type == MeshMessage.TYPE_MEDICAL ||
        type == MeshMessage.TYPE_SOS
}

@Composable
fun MeshScreen(
    uiState: MeshUiState,
    onSend: (String) -> Unit,
    onSendSos: () -> Unit,
    onSendEmergency: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var input by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("ALL") }
    val scrollState = rememberLazyListState()
    val connectedCount = uiState.peerMap.size
    val connectedPeerNames = uiState.peerMap.values.sorted().joinToString(", ")
    val context = LocalContext.current

    val filteredMessages = remember(uiState.messages, selectedFilter) {
        if (selectedFilter == "EMERGENCY") {
            uiState.messages.filter { isEmergencyType(it.type) }
        } else {
            uiState.messages
        }
    }

    LaunchedEffect(filteredMessages.size) {
        if (filteredMessages.isNotEmpty()) {
            scrollState.animateScrollToItem(filteredMessages.lastIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF05070B),
                        Color(0xFF090D16),
                        Color(0xFF101827)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // HEADER BAR
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF111827),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A3548)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (connectedCount > 0) Color(0xFF10B981) else Color(0xFFF59E0B))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ResQMesh",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFF1B2434)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📱 ${uiState.localDisplayName}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = connectedPeerNames.ifEmpty { uiState.status },
                            fontSize = 12.sp,
                            color = if (connectedCount > 0) Color(0xFF34D399) else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFF1A2230)
                        ) {
                            Text(
                                text = "$connectedCount Peers Active",
                                fontSize = 11.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // EMERGENCY ACTIONS
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "DISTRESS SIGNALS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF64748B),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. TRAPPED
                    EmergencyActionButton(
                        icon = "🏗️",
                        title = "TRAPPED",
                        subtitle = "Debris",
                        accentColor = Color(0xFFDC2626),
                        modifier = Modifier.weight(1f),
                        onClick = { onSendEmergency(MeshMessage.TYPE_TRAPPED, "") }
                    )

                    // 2. STARVING
                    EmergencyActionButton(
                        icon = "🍞",
                        title = "STARVING",
                        subtitle = "Food",
                        accentColor = Color(0xFFD97706),
                        modifier = Modifier.weight(1f),
                        onClick = { onSendEmergency(MeshMessage.TYPE_STARVING, "") }
                    )

                    // 3. URGENT MEDICAL CARE
                    EmergencyActionButton(
                        icon = "🩺",
                        title = "MEDICAL",
                        subtitle = "Care",
                        accentColor = Color(0xFF0284C7),
                        modifier = Modifier.weight(1f),
                        onClick = { onSendEmergency(MeshMessage.TYPE_MEDICAL, "") }
                    )
                }
            }

            // FILTER TABS BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedFilter == "ALL") Color(0xFF2A374A) else Color(0xFF141B28),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedFilter == "ALL") Color(0xFF4B5563) else Color(0xFF233144)),
                    modifier = Modifier
                        .clickable { selectedFilter = "ALL" }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "All Feed (${uiState.messages.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selectedFilter == "EMERGENCY") Color(0xFF431111) else Color(0xFF141B28),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selectedFilter == "EMERGENCY") Color(0xFFF87171) else Color(0xFF233144)),
                    modifier = Modifier
                        .clickable { selectedFilter = "EMERGENCY" }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🚨 Distress Calls Only",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            // FEED LIST
            LazyColumn(
                state = scrollState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredMessages) { message ->
                    val isOwnMessage = message.sender == uiState.localDisplayName
                    val isEmergency = isEmergencyType(message.type)

                    if (isEmergency) {
                        EmergencyDistressCard(
                            message = message,
                            onOpenMap = { lat, lng ->
                                val mapUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Emergency+Location)")
                                val intent = Intent(Intent.ACTION_VIEW, mapUri)
                                context.startActivity(intent)
                            }
                        )
                    } else {
                        // Standard Chat Bubble
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
                        ) {
                            val bubbleBrush = if (isOwnMessage) {
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF4338CA),
                                        Color(0xFF312E81)
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF1B2433),
                                        Color(0xFF111827)
                                    )
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.Transparent,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isOwnMessage) Color(0xFF818CF8) else Color(0xFF334155)
                                ),
                                tonalElevation = 4.dp,
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(bubbleBrush)
                                        .clip(RoundedCornerShape(20.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = message.sender,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isOwnMessage) Color(0xFFE0E7FF) else Color(0xFF94A3B8)
                                            )
                                            Text(
                                                text = message.formattedTimestamp(),
                                                fontSize = 10.sp,
                                                color = if (isOwnMessage) Color(0xFFC7D2FE) else Color(0xFF64748B),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Text(
                                            text = message.text,
                                            fontSize = 14.sp,
                                            color = Color.White,
                                            lineHeight = 20.sp
                                        )
                                        Text(
                                            text = if (message.hops == 0) "direct • ${message.formattedTimestamp()}" else "via ${message.hops} hops • ${message.formattedTimestamp()}",
                                            fontSize = 10.sp,
                                            color = if (isOwnMessage) Color(0xFFC7D2FE) else Color(0xFF64748B),
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // BOTTOM INPUT BAR
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message to mesh...", fontSize = 13.sp, color = Color(0xFF64748B)) },
                    enabled = connectedCount > 0,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF101826),
                        unfocusedContainerColor = Color(0xFF101826),
                        focusedBorderColor = Color(0xFF8B5CF6),
                        unfocusedBorderColor = Color(0xFF2C3648),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Button(
                    onClick = {
                        val trimmed = input.trim()
                        if (trimmed.isNotEmpty()) {
                            onSend(trimmed)
                            input = ""
                        }
                    },
                    enabled = connectedCount > 0 && input.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6D28D9),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF1A2230),
                        disabledContentColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier.height(54.dp)
                ) {
                    Text("Send", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EmergencyActionButton(
    icon: String,
    title: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor),
        modifier = modifier.height(68.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            accentColor.copy(alpha = 0.22f),
                            Color(0xFF0B1020).copy(alpha = 0.9f)
                        )
                    )
                )
                .clip(RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = icon, fontSize = 16.sp)
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 9.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun EmergencyDistressCard(
    message: MeshMessage,
    onOpenMap: (Double, Double) -> Unit
) {
    val (headerTitle, headerIcon, cardBg, borderColor, badgeColor) = when (message.type) {
        MeshMessage.TYPE_TRAPPED -> Tuple5(
            "🏗️ TRAPPED UNDER DEBRIS",
            "🏗️",
            Color(0xFF3F0F12),
            Color(0xFFEF4444),
            Color(0xFFDC2626)
        )
        MeshMessage.TYPE_STARVING -> Tuple5(
            "🍞 STARVING / NEED SUPPLIES",
            "🍞",
            Color(0xFF3D2106),
            Color(0xFFF59E0B),
            Color(0xFFD97706)
        )
        MeshMessage.TYPE_MEDICAL -> Tuple5(
            "🩺 URGENT MEDICAL CARE",
            "🩺",
            Color(0xFF092938),
            Color(0xFF38BDF8),
            Color(0xFF0284C7)
        )
        else -> Tuple5(
            "🚨 EMERGENCY DISTRESS CALL",
            "🚨",
            Color(0xFF3F0F12),
            Color(0xFFEF4444),
            Color(0xFFDC2626)
        )
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
        tonalElevation = 6.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            cardBg,
                            Color(0xFF0B1020)
                        )
                    )
                )
                .clip(RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = headerTitle,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Text(
                        text = message.formattedTimestamp(),
                        fontSize = 10.sp,
                        color = Color(0xFFCBD5E1),
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Sender: ${message.sender} • ${message.formattedTimestamp()}",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1),
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = message.text,
                    fontSize = 13.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )

                if (message.lat != null && message.lng != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.35f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📍 GPS: ${String.format("%.4f", message.lat)}, ${String.format("%.4f", message.lng)}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedButton(
                                onClick = { onOpenMap(message.lat, message.lng) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                            ) {
                                Text("View Map", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class Tuple5<A, B, C, D, E>(
    val a: A, val b: B, val c: C, val d: D, val e: E
)
