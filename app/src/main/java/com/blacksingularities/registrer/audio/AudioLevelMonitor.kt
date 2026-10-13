package com.blacksingularities.registrer.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import kotlin.math.abs
import kotlin.math.max

/**
 * Podsłuch poziomu mikrofonu przed nagraniem wideo – ten sam mikrofon, którego użyje
 * rejestrator (wbudowany albo podłączony). Nic nie zapisuje; zatrzymywany przed startem
 * nagrywania, bo wtedy poziom podaje sam rejestrator.
 */
class AudioLevelMonitor {
    @Volatile var peak = 0f
        private set
    @Volatile var deviceName: String? = null
        private set
    @Volatile private var running = false
    private var thread: Thread? = null

    val isRunning: Boolean get() = running

    @SuppressLint("MissingPermission")
    fun start() {
        if (running) return
        val rate = 48_000
        val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) return
        val rec = runCatching {
            AudioRecord(MediaRecorder.AudioSource.CAMCORDER, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, max(min * 2, 4096))
        }.getOrNull() ?: return
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            return
        }
        runCatching { rec.startRecording() }
        if (rec.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            rec.release()
            return
        }
        running = true
        val t = Thread({
            runCatching { Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO) }
            val buf = ShortArray(rate / 50)
            var named = false
            while (running) {
                val n = rec.read(buf, 0, buf.size)
                if (n <= 0) continue
                var p = 0
                for (i in 0 until n) {
                    val a = abs(buf[i].toInt())
                    if (a > p) p = a
                }
                peak = p / 32768f
                if (!named) {
                    named = true
                    deviceName = runCatching { rec.routedDevice }.getOrNull()?.let { AudioEngine.describe(it) }
                }
            }
            runCatching { rec.stop() }
            rec.release()
            peak = 0f
        }, "OpenShot-level")
        thread = t
        t.start()
    }

    fun stop() {
        running = false
        thread = null
    }

    /** Zatrzymuje i czeka na zwolnienie mikrofonu (przed startem nagrywania wideo). */
    fun stopAndWait() {
        val t = thread
        stop()
        runCatching { t?.join(200) }
    }
}
