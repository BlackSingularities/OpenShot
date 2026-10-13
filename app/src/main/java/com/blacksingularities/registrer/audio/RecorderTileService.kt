package com.blacksingularities.registrer.audio

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.blacksingularities.registrer.MainActivity

/**
 * Kafelek „Dyktafon” w szybkich ustawieniach: dotknięcie otwiera aplikację i od razu zaczyna
 * nagrywać, a podczas nagrywania – kończy nagranie bez otwierania aplikacji.
 */
class RecorderTileService : TileService() {
    private val listener = object : AudioEngine.Listener {
        override fun onStateChanged() = refresh()
        override fun onFinished(result: AudioEngine.Finished) = refresh()
    }

    override fun onStartListening() {
        super.onStartListening()
        AudioEngine.get(this).addListener(listener)
        refresh()
    }

    override fun onStopListening() {
        AudioEngine.get(this).removeListener(listener)
        super.onStopListening()
    }

    private fun refresh() {
        val tile = qsTile ?: return
        val active = AudioEngine.get(this).isActive
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Dyktafon"
        tile.subtitle = if (active) "Nagrywa – dotknij, by zakończyć" else "Dotknij, by nagrywać"
        runCatching { tile.updateTile() }
    }

    override fun onClick() {
        super.onClick()
        val engine = AudioEngine.get(this)
        if (engine.isActive) {
            engine.stop()
            refresh()
            return
        }
        val intent = Intent(this, MainActivity::class.java)
            .setAction(MainActivity.ACTION_RECORD_AUDIO)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 7, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
