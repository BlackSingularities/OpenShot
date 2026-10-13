package com.blacksingularities.registrer.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat

/**
 * Pozycja do oznaczania zdjęć i filmów. Korzysta z systemowego LocationManager (bez usług
 * Google – działa także na telefonach Huawei). Nasłuch tylko przy widocznej aplikacji,
 * rzadko i z dokładnością wystarczającą do mapy zdjęć.
 */
class LocationTracker(private val context: Context) {
    private val manager = context.getSystemService(LocationManager::class.java)
    private var listening = false

    private val listener = LocationListener { update(it) }

    private fun granted(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun providers(): List<String> {
        val m = manager ?: return emptyList()
        val wanted = buildList {
            if (Build.VERSION.SDK_INT >= 31) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.PASSIVE_PROVIDER)
        }
        return wanted.filter { p -> runCatching { m.isProviderEnabled(p) }.getOrDefault(false) }
    }

    /** Lepsza z dwóch pozycji: świeższa, a przy podobnym wieku – dokładniejsza. */
    private fun better(a: Location?, b: Location?): Location? {
        if (a == null) return b
        if (b == null) return a
        val ageA = SystemClock.elapsedRealtimeNanos() - a.elapsedRealtimeNanos
        val ageB = SystemClock.elapsedRealtimeNanos() - b.elapsedRealtimeNanos
        if (abs(ageA - ageB) > 120_000_000_000L) return if (ageA < ageB) a else b
        return if (a.accuracy <= b.accuracy) a else b
    }

    private fun abs(v: Long) = if (v < 0) -v else v

    private fun update(location: Location) {
        MediaSaver.location = better(location, MediaSaver.location)
    }

    @SuppressLint("MissingPermission")
    fun start() {
        val m = manager ?: return
        if (listening || !granted()) return
        val available = providers()
        available.forEach { p -> runCatching { m.getLastKnownLocation(p) }.getOrNull()?.let { update(it) } }
        for (p in available) {
            if (p == LocationManager.PASSIVE_PROVIDER) continue
            runCatching { m.requestLocationUpdates(p, 30_000L, 25f, listener, Looper.getMainLooper()) }
        }
        listening = true
    }

    fun stop() {
        if (!listening) return
        runCatching { manager?.removeUpdates(listener) }
        listening = false
    }

    /** Wyłączenie oznaczania – usuwa też zapamiętaną pozycję. */
    fun disable() {
        stop()
        MediaSaver.location = null
    }
}
