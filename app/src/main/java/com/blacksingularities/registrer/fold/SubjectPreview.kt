package com.blacksingularities.registrer.fold

import com.blacksingularities.registrer.i18n.tr
import android.app.Activity
import android.app.Presentation
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.os.Binder
import android.view.Display
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.area.WindowAreaCapability
import androidx.window.area.WindowAreaController
import androidx.window.area.WindowAreaInfo
import androidx.window.area.WindowAreaPresentationSessionCallback
import androidx.window.area.WindowAreaSessionPresenter
import androidx.window.core.ExperimentalWindowApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Podgląd dla fotografowanego: obraz z aparatu na drugim ekranie, gdy aplikacja działa na
 * głównym. Dwie drogi:
 * 1. tryb prezentacji na tylnym ekranie składanego telefonu (Jetpack WindowArea, „Dual Screen”),
 * 2. dowolny dodatkowy ekran widoczny w systemie (ekran zewnętrzny, HDMI, przesyłanie obrazu).
 * Obraz kopiowany z podglądu ok. 15 razy na sekundę; domyślnie jak w lustrze.
 */
@OptIn(ExperimentalWindowApi::class)
class SubjectPreview(
    private val activity: Activity,
    private val onAvailable: (Boolean) -> Unit,
    private val onActive: (Boolean) -> Unit,
) {
    private val controller = runCatching { WindowAreaController.getOrCreate() }.getOrNull()
    private val displays = activity.getSystemService(DisplayManager::class.java)
    private var rearToken: Binder? = null
    private var rearAvailable = false
    private var presenter: WindowAreaSessionPresenter? = null
    private var presentation: Presentation? = null
    private var loop: Job? = null
    private var owner: LifecycleOwner? = null

    private val operation = WindowAreaCapability.Operation.OPERATION_PRESENT_ON_AREA

    val isActive: Boolean get() = presenter != null || presentation != null

    private fun secondaryDisplay(): Display? = runCatching {
        displays?.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)?.firstOrNull()
            ?: displays?.displays?.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY && it.state != Display.STATE_OFF }
    }.getOrNull()

    private fun publish() = onAvailable(rearAvailable || secondaryDisplay() != null || isActive)

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = publish()
        override fun onDisplayRemoved(displayId: Int) {
            if (presentation?.display?.displayId == displayId) stop()
            publish()
        }
        override fun onDisplayChanged(displayId: Int) = publish()
    }

    /** Opis ekranów do diagnostyki („Informacje o sprzęcie”). */
    fun summary(): String = runCatching {
        val list = displays?.displays.orEmpty().joinToString("\n") { d ->
            val state = when (d.state) {
                Display.STATE_ON -> tr("wł.", "on")
                Display.STATE_OFF -> tr("wył.", "off")
                else -> tr("stan ${d.state}", "state ${d.state}")
            }
            val mode = d.mode
            "#${d.displayId} ${d.name} · ${mode.physicalWidth}×${mode.physicalHeight} · $state"
        }
        val rear = if (controller == null) tr("brak WindowArea", "no WindowArea") else if (rearAvailable) tr("tylny ekran: dostępny", "rear screen: available") else tr("tylny ekran: niedostępny", "rear screen: unavailable")
        "$list\n$rear"
    }.getOrDefault("")

    fun observe(owner: LifecycleOwner) {
        this.owner = owner
        runCatching { displays?.registerDisplayListener(displayListener, null) }
        publish()
        val c = controller ?: return
        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                runCatching {
                    c.windowAreaInfos.collect { infos ->
                        val rear = infos.firstOrNull { it.type == WindowAreaInfo.Type.TYPE_REAR_FACING }
                        val status = rear?.getCapability(operation)?.status
                        rearToken = rear?.token
                        rearAvailable = status == WindowAreaCapability.Status.WINDOW_AREA_STATUS_AVAILABLE ||
                            status == WindowAreaCapability.Status.WINDOW_AREA_STATUS_ACTIVE
                        publish()
                    }
                }
            }
        }
    }

    fun toggle(source: () -> Bitmap?, mirror: Boolean) {
        if (isActive) {
            stop()
            return
        }
        val token = rearToken
        val c = controller
        if (rearAvailable && token != null && c != null) {
            runCatching {
                c.presentContentOnWindowArea(
                    token,
                    activity,
                    ContextCompat.getMainExecutor(activity),
                    object : WindowAreaPresentationSessionCallback {
                        override fun onSessionStarted(session: WindowAreaSessionPresenter) {
                            presenter = session
                            val view = imageView(session.context, mirror)
                            session.setContentView(view)
                            startLoop(view, source)
                            onActive(true)
                        }

                        override fun onSessionEnded(t: Throwable?) {
                            presenter = null
                            loop?.cancel()
                            onActive(false)
                        }

                        override fun onContainerVisibilityChanged(isVisible: Boolean) = Unit
                    },
                )
            }.onSuccess { return }
        }
        val display = secondaryDisplay() ?: return
        runCatching {
            val p = Presentation(activity, display)
            val view = imageView(p.context, mirror)
            p.setContentView(view)
            p.setOnDismissListener {
                if (presentation === p) {
                    presentation = null
                    loop?.cancel()
                    onActive(false)
                }
            }
            p.show()
            presentation = p
            startLoop(view, source)
            onActive(true)
        }
    }

    fun stop() {
        loop?.cancel()
        loop = null
        presenter?.let { runCatching { it.close() } }
        presenter = null
        presentation?.let { runCatching { it.dismiss() } }
        presentation = null
        onActive(false)
    }

    fun release() {
        stop()
        runCatching { displays?.unregisterDisplayListener(displayListener) }
    }

    private fun imageView(context: Context, mirror: Boolean) = ImageView(context).apply {
        setBackgroundColor(Color.BLACK)
        scaleType = ImageView.ScaleType.FIT_CENTER
        scaleX = if (mirror) -1f else 1f
        keepScreenOn = true
        systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    private fun startLoop(view: ImageView, source: () -> Bitmap?) {
        loop?.cancel()
        val scope = owner?.lifecycleScope ?: return
        loop = scope.launch {
            var shown: Bitmap? = null
            while (isActive) {
                val next = runCatching { source() }.getOrNull()
                if (next != null) {
                    view.setImageBitmap(next)
                    shown?.takeIf { it !== next }?.recycle()
                    shown = next
                }
                delay(66)
            }
        }
    }
}
