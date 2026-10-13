package com.blacksingularities.registrer

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.blacksingularities.registrer.data.ScreenRotation
import com.blacksingularities.registrer.fold.RearDisplay
import com.blacksingularities.registrer.ui.CameraScreen
import com.blacksingularities.registrer.ui.CameraViewModel
import com.blacksingularities.registrer.ui.components.PermissionScreen
import com.blacksingularities.registrer.ui.theme.OpenShotTheme

open class MainActivity : ComponentActivity() {
    private val vm: CameraViewModel by viewModels()
    private lateinit var rearDisplay: RearDisplay
    private lateinit var subjectPreview: com.blacksingularities.registrer.fold.SubjectPreview

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.blacksingularities.registrer.i18n.Lang.apply(vm.global.language, this)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            isAppearanceLightStatusBars = false
        }

        rearDisplay = RearDisplay(
            activity = this,
            onAvailable = { vm.rearDisplayAvailable = it },
            onActive = { vm.onRearDisplayChanged(it) },
        )
        rearDisplay.observe(this)
        subjectPreview = com.blacksingularities.registrer.fold.SubjectPreview(
            activity = this,
            onAvailable = { vm.subjectPreviewAvailable = it },
            onActive = { vm.subjectPreviewActive = it },
        )
        subjectPreview.observe(this)
        if (savedInstanceState == null) {
            vm.handleShortcut(intent?.action)
            handleCameraIntent(intent)
        }

        setContent {
            OpenShotTheme {
                // Zlecenie innej aplikacji zakończone – wynik i powrót do niej.
                val external = vm.externalResult
                LaunchedEffect(external) {
                    if (external != null) {
                        setResult(external.first, external.second)
                        finish()
                    }
                }
                var cameraGranted by remember { mutableStateOf(isGranted(Manifest.permission.CAMERA)) }
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                    cameraGranted = result[Manifest.permission.CAMERA] == true || isGranted(Manifest.permission.CAMERA)
                    vm.refreshLocation()
                }
                val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                    vm.refreshLocation()
                }
                val request = {
                    val perms = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                    // Powiadomienie „zaktualizowano – dotknij, aby otworzyć”, gdy system nie pozwoli otworzyć aparatu sam.
                    if (android.os.Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
                    if (vm.global.geotag) perms += listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    launcher.launch(perms.toTypedArray())
                }
                LaunchedEffect(Unit) {
                    val notificationsMissing = android.os.Build.VERSION.SDK_INT >= 33 &&
                        !isGranted(Manifest.permission.POST_NOTIFICATIONS)
                    if (!cameraGranted || !isGranted(Manifest.permission.RECORD_AUDIO) || notificationsMissing) request()
                }
                // Włączenie lokalizacji w ustawieniach prosi o zgodę, jeśli jej brakuje.
                LaunchedEffect(vm.global.geotag, cameraGranted) {
                    val missing = !isGranted(Manifest.permission.ACCESS_FINE_LOCATION) && !isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
                    if (vm.global.geotag && cameraGranted && missing) {
                        locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                }
                // Stały ekran (obracają się tylko elementy) albo pełny obrót – niezależnie od blokady systemu.
                // Podczas nagrywania i przetwarzania orientacja jest zablokowana.
                val hold = vm.holdOrientation
                LaunchedEffect(vm.global.screenRotation, hold) {
                    requestedOrientation = when {
                        vm.global.screenRotation == ScreenRotation.FIXED -> ActivityInfo.SCREEN_ORIENTATION_NOSENSOR
                        hold -> ActivityInfo.SCREEN_ORIENTATION_LOCKED
                        else -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                    }
                }
                if (cameraGranted) {
                    CameraScreen(
                        vm,
                        onToggleRearDisplay = rearDisplay::toggle,
                        onToggleSubjectPreview = { source -> subjectPreview.toggle(source, vm.global.subjectMirror) },
                    )
                } else {
                    PermissionScreen(onRequest = request)
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        vm.handleShortcut(intent.action)
        handleCameraIntent(intent)
    }

    /** Aparat z ekranu blokady oraz zdjęcie / film na zlecenie innej aplikacji. */
    private fun handleCameraIntent(intent: android.content.Intent?) {
        when (intent?.action) {
            android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE -> {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            }
            android.provider.MediaStore.ACTION_IMAGE_CAPTURE -> vm.startExternalCapture(false, outputUri(intent))
            android.provider.MediaStore.ACTION_VIDEO_CAPTURE -> vm.startExternalCapture(true, outputUri(intent))
        }
    }

    private fun outputUri(intent: android.content.Intent): android.net.Uri? {
        val extra = if (android.os.Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(android.provider.MediaStore.EXTRA_OUTPUT, android.net.Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(android.provider.MediaStore.EXTRA_OUTPUT)
        }
        return extra ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
    }

    private fun isGranted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (vm.onVolumeKey(keyCode == KeyEvent.KEYCODE_VOLUME_UP, event.repeatCount)) return true
        }
        if (keyCode == KeyEvent.KEYCODE_CAMERA && event.repeatCount == 0) {
            vm.onShutter()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onStart() {
        super.onStart()
        if (::subjectPreview.isInitialized) vm.displaySummary = subjectPreview.summary()
        com.blacksingularities.registrer.update.UpdateManager.clearUpdatedNotification(this)
    }

    override fun onDestroy() {
        if (::subjectPreview.isInitialized) subjectPreview.release()
        super.onDestroy()
    }

    override fun onStop() {
        super.onStop()
        if (::subjectPreview.isInitialized) subjectPreview.stop()
        vm.onBackground()
    }

    companion object {
        const val ACTION_RECORD_AUDIO = "com.blacksingularities.registrer.action.RECORD_AUDIO"
        const val ACTION_VIDEO = "com.blacksingularities.registrer.action.VIDEO"
        const val ACTION_SELFIE = "com.blacksingularities.registrer.action.SELFIE"
    }
}
