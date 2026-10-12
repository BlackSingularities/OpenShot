package com.blacksingularities.registrer.fold

import android.app.Activity
import android.os.Binder
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.window.area.WindowAreaCapability
import androidx.window.area.WindowAreaController
import androidx.window.area.WindowAreaInfo
import androidx.window.area.WindowAreaSession
import androidx.window.area.WindowAreaSessionCallback
import androidx.window.core.ExperimentalWindowApi
import kotlinx.coroutines.launch

/**
 * Tryb „selfie głównym aparatem” na foldach: po rozłożeniu aplikacja przenosi się na ekran
 * zewnętrzny, skierowany w tę samą stronę co główne aparaty. Korzysta z systemowego trybu
 * Rear Display – dostępny tam, gdzie producent go udostępnia (np. Pixel Fold); na pozostałych
 * urządzeniach przycisk się nie pojawia.
 */
@OptIn(ExperimentalWindowApi::class)
class RearDisplay(
    private val activity: Activity,
    private val onAvailable: (Boolean) -> Unit,
    private val onActive: (Boolean) -> Unit,
) {
    private val controller = runCatching { WindowAreaController.getOrCreate() }.getOrNull()
    private var token: Binder? = null
    private var session: WindowAreaSession? = null

    private val operation = WindowAreaCapability.Operation.OPERATION_TRANSFER_ACTIVITY_TO_AREA

    fun observe(owner: LifecycleOwner) {
        val c = controller ?: return
        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                runCatching {
                    c.windowAreaInfos.collect { infos ->
                        val rear = infos.firstOrNull { it.type == WindowAreaInfo.Type.TYPE_REAR_FACING }
                        val status = rear?.getCapability(operation)?.status
                        token = rear?.token
                        val available = status == WindowAreaCapability.Status.WINDOW_AREA_STATUS_AVAILABLE ||
                            status == WindowAreaCapability.Status.WINDOW_AREA_STATUS_ACTIVE
                        onAvailable(available)
                        if (status == WindowAreaCapability.Status.WINDOW_AREA_STATUS_ACTIVE && session == null) {
                            session = rear?.getActiveSession(operation)
                            onActive(session != null)
                        }
                    }
                }
            }
        }
    }

    fun toggle() {
        val active = session
        if (active != null) {
            runCatching { active.close() }
            return
        }
        val c = controller ?: return
        val t = token ?: return
        runCatching {
            c.transferActivityToWindowArea(
                t,
                activity,
                ContextCompat.getMainExecutor(activity),
                object : WindowAreaSessionCallback {
                    override fun onSessionStarted(session: WindowAreaSession) {
                        this@RearDisplay.session = session
                        onActive(true)
                    }

                    override fun onSessionEnded(t: Throwable?) {
                        session = null
                        onActive(false)
                    }
                },
            )
        }
    }
}
