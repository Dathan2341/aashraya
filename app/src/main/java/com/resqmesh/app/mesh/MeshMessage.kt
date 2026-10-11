package com.resqmesh.app.mesh

import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

data class MeshMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String,
    val text: String,
    val ttl: Int,
    val hops: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = TYPE_CHAT,
    val lat: Double? = null,
    val lng: Double? = null
) {
    fun formattedTimestamp(): String {
        val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .format(formatter)
    }

    fun toJson(): String = JSONObject().apply {
        put("id", id)
        put("sender", sender)
        put("text", text)
        put("ttl", ttl)
        put("hops", hops)
        put("timestamp", timestamp)
        put("type", type)
        lat?.let { put("lat", it) }
        lng?.let { put("lng", it) }
    }.toString()

    companion object {
        const val TYPE_CHAT = "CHAT"
        const val TYPE_TRAPPED = "TRAPPED"
        const val TYPE_STARVING = "STARVING"
        const val TYPE_MEDICAL = "MEDICAL"
        const val TYPE_SOS = "SOS"

        fun fromJson(json: String): MeshMessage? {
            return try {
                val obj = JSONObject(json)
                val latVal = if (obj.has("lat") && !obj.isNull("lat")) obj.optDouble("lat") else null
                val lngVal = if (obj.has("lng") && !obj.isNull("lng")) obj.optDouble("lng") else null

                MeshMessage(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    sender = obj.optString("sender", "unknown"),
                    text = obj.optString("text", ""),
                    ttl = obj.optInt("ttl", 5),
                    hops = obj.optInt("hops", 0),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    type = obj.optString("type", TYPE_CHAT),
                    lat = latVal,
                    lng = lngVal
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
