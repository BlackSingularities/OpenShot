package com.blacksingularities.registrer

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings

/**
 * Domyślny aparat. Android nie ma osobnej roli „aparat” – domyślność to zapamiętany wybór
 * „Zawsze” dla intencji aparatu. Sprawdzamy, kto ją obsługuje, i pomagamy ją ustawić:
 * gdy nikt nie jest domyślny – systemowy wybór z opcją „Zawsze”, gdy inna aplikacja –
 * jej ustawienia, by wyczyścić „Otwieraj domyślnie”.
 */
object DefaultCamera {
    sealed class Status {
        object Ours : Status()
        object None : Status()
        data class Other(val packageName: String, val label: String) : Status()
    }

    private fun cameraIntent() = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)

    fun status(context: Context): Status {
        val pm = context.packageManager
        val info = runCatching { pm.resolveActivity(cameraIntent(), PackageManager.MATCH_DEFAULT_ONLY) }.getOrNull()
            ?: return Status.None
        val pkg = info.activityInfo?.packageName ?: return Status.None
        return when {
            pkg == context.packageName -> Status.Ours
            // Systemowy ekran wyboru = brak zapamiętanego wyboru.
            pkg == "android" || info.activityInfo.name.contains("ResolverActivity") -> Status.None
            else -> {
                val label = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
                Status.Other(pkg, label)
            }
        }
    }

    /** Systemowy wybór aparatu (z „Zawsze”) albo ustawienia aplikacji, która jest domyślna. */
    fun request(context: Context) {
        val intent = when (val s = status(context)) {
            is Status.Other -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", s.packageName, null))
            else -> cameraIntent()
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
