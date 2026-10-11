package com.resqmesh.app.mesh

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

class LocationHelper(private val context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(onLocationResult: (Double?, Double?) -> Unit) {
        try {
            val cancellationTokenSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    Log.d(TAG, "Fused location acquired: ${location.latitude}, ${location.longitude}")
                    onLocationResult(location.latitude, location.longitude)
                } else {
                    fallbackToLastKnownLocation(onLocationResult)
                }
            }.addOnFailureListener { e ->
                Log.w(TAG, "FusedLocation failed, attempting fallback", e)
                fallbackToLastKnownLocation(onLocationResult)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting location", e)
            fallbackToLastKnownLocation(onLocationResult)
        }
    }

    @SuppressLint("MissingPermission")
    private fun fallbackToLastKnownLocation(onLocationResult: (Double?, Double?) -> Unit) {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    Log.d(TAG, "Last known location acquired: ${location.latitude}, ${location.longitude}")
                    onLocationResult(location.latitude, location.longitude)
                } else {
                    fallbackToLocationManager(onLocationResult)
                }
            }.addOnFailureListener {
                fallbackToLocationManager(onLocationResult)
            }
        } catch (_: Exception) {
            fallbackToLocationManager(onLocationResult)
        }
    }

    @SuppressLint("MissingPermission")
    private fun fallbackToLocationManager(onLocationResult: (Double?, Double?) -> Unit) {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val gpsLoc = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val netLoc = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            val bestLoc = gpsLoc ?: netLoc
            if (bestLoc != null) {
                Log.d(TAG, "LocationManager fallback acquired: ${bestLoc.latitude}, ${bestLoc.longitude}")
                onLocationResult(bestLoc.latitude, bestLoc.longitude)
            } else {
                Log.w(TAG, "No location available")
                onLocationResult(null, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "LocationManager fallback failed", e)
            onLocationResult(null, null)
        }
    }

    companion object {
        private const val TAG = "ResQMesh-Location"
    }
}
