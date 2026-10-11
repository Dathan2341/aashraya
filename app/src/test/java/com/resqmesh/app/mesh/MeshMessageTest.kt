package com.resqmesh.app.mesh

import org.junit.Assert.assertTrue
import org.junit.Test

class MeshMessageTest {
    @Test
    fun formattedTimestamp_returns_non_empty_time() {
        val message = MeshMessage(
            sender = "alice",
            text = "hello",
            ttl = 5,
            hops = 0,
            timestamp = 1710000000000L
        )

        val formatted = message.formattedTimestamp()

        assertTrue(formatted.isNotBlank())
        assertTrue(formatted.contains(":"))
    }
}
