package com.blacksingularities.registrer.audio

import com.blacksingularities.registrer.i18n.tr
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat

/**
 * Usługa pierwszoplanowa dyktafonu: utrzymuje nagrywanie przy zgaszonym ekranie i po wyjściu
 * z aplikacji, a powiadomienie pozwala wstrzymać, wznowić, dodać znacznik i zakończyć nagranie.
 * Samo nagrywanie prowadzi [AudioEngine]; usługa kończy się razem z nim.
 */
class AudioRecordService : Service() {
    private val engine by lazy { AudioEngine.get(this) }

    private val listener = object : AudioEngine.Listener {
        override fun onStateChanged() = refresh()
        override fun onFinished(result: AudioEngine.Finished) = finish()
    }

    override fun onCreate() {
        super.onCreate()
        engine.addListener(listener)
    }

    override fun onDestroy() {
        engine.removeListener(listener)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForeground musi paść przy każdym starcie, także gdy nagranie właśnie się skończyło.
        runCatching { goForeground() }
        when (intent?.action) {
            ACTION_PAUSE -> engine.pause()
            ACTION_RESUME -> engine.resume()
            ACTION_STOP -> engine.stop()
            ACTION_MARK -> engine.addMarker()
        }
        if (engine.status == RecorderStatus.IDLE) finish()
        return START_NOT_STICKY
    }

    private fun goForeground() {
        val n = build()
        if (Build.VERSION.SDK_INT >= 30) {
            startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, n)
        }
    }

    private fun refresh() {
        if (engine.status == RecorderStatus.IDLE) return
        runCatching { getSystemService(NotificationManager::class.java)?.notify(NOTIFICATION_ID, build()) }
    }

    private fun finish() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun action(action: String, code: Int): PendingIntent = PendingIntent.getService(
        this,
        code,
        Intent(this, AudioRecordService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun build(): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        nm?.createNotificationChannel(
            NotificationChannel(CHANNEL, tr("Nagrywanie dźwięku", "Recording audio"), NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            },
        )
        val status = engine.status
        val paused = status == RecorderStatus.PAUSED
        val finishing = status == RecorderStatus.FINISHING
        val launch = packageManager.getLaunchIntentForPackage(packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        val open = launch?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val details = buildList {
            add(engine.formatLabel)
            engine.deviceName?.let { add(it) }
            engine.markerCount.takeIf { it > 0 }?.let { add(tr("znaczniki: $it", "markers: $it")) }
            if (engine.silent) add(tr("cisza – pomijana", "silence – skipped"))
        }.filter { it.isNotBlank() }.joinToString(" · ")
        val b = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(
                when {
                    finishing -> tr("Zapisywanie nagrania…", "Saving the recording…")
                    paused -> tr("Nagrywanie wstrzymane · ${formatDuration(engine.recordedMs)}", "Recording paused · ${formatDuration(engine.recordedMs)}")
                    else -> tr("Nagrywanie dźwięku", "Recording audio")
                },
            )
            .setContentText(details)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setShowWhen(!paused && !finishing)
            .setUsesChronometer(!paused && !finishing)
            .setWhen(System.currentTimeMillis() - engine.recordedMs)
        open?.let { b.setContentIntent(it) }
        if (!finishing) {
            if (paused) {
                b.addAction(Notification.Action.Builder(Icon.createWithResource(this, android.R.drawable.ic_media_play), tr("Wznów", "Resume"), action(ACTION_RESUME, 1)).build())
            } else {
                b.addAction(Notification.Action.Builder(Icon.createWithResource(this, android.R.drawable.ic_media_pause), tr("Pauza", "Pause"), action(ACTION_PAUSE, 2)).build())
            }
            b.addAction(Notification.Action.Builder(Icon.createWithResource(this, android.R.drawable.star_on), tr("Znacznik", "Marker"), action(ACTION_MARK, 3)).build())
            b.addAction(Notification.Action.Builder(Icon.createWithResource(this, android.R.drawable.ic_menu_save), tr("Zakończ", "Stop"), action(ACTION_STOP, 4)).build())
        }
        if (Build.VERSION.SDK_INT >= 31) b.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        return b.build()
    }

    companion object {
        private const val CHANNEL = "recording"
        private const val NOTIFICATION_ID = 4107
        private const val ACTION_PAUSE = "com.blacksingularities.registrer.audio.PAUSE"
        private const val ACTION_RESUME = "com.blacksingularities.registrer.audio.RESUME"
        private const val ACTION_STOP = "com.blacksingularities.registrer.audio.STOP"
        private const val ACTION_MARK = "com.blacksingularities.registrer.audio.MARK"

        /** Uruchamia usługę dla trwającego nagrania; bez niej nagrywanie działa tylko przy otwartej aplikacji. */
        fun start(context: Context) {
            runCatching { ContextCompat.startForegroundService(context, Intent(context, AudioRecordService::class.java)) }
        }

        private fun formatDuration(ms: Long): String {
            val s = ms / 1000
            return if (s >= 3600) String.format(java.util.Locale.US, "%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60)
            else String.format(java.util.Locale.US, "%02d:%02d", s / 60, s % 60)
        }
    }
}
