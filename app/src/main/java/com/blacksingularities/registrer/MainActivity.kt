package com.blacksingularities.registrer

import android.Manifest
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
import com.blacksingularities.registrer.ui.CameraScreen
import com.blacksingularities.registrer.ui.CameraViewModel
import com.blacksingularities.registrer.ui.components.PermissionScreen
import com.blacksingularities.registrer.ui.theme.RegistrerTheme

class MainActivity : ComponentActivity() {
    private val vm: CameraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            isAppearanceLightStatusBars = false
        }

        setContent {
            RegistrerTheme {
                var cameraGranted by remember { mutableStateOf(isGranted(Manifest.permission.CAMERA)) }
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                    cameraGranted = result[Manifest.permission.CAMERA] == true || isGranted(Manifest.permission.CAMERA)
                }
                val request = {
                    launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                }
                LaunchedEffect(Unit) {
                    if (!cameraGranted || !isGranted(Manifest.permission.RECORD_AUDIO)) request()
                }
                if (cameraGranted) CameraScreen(vm) else PermissionScreen(onRequest = request)
            }
        }
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

    override fun onStop() {
        super.onStop()
        vm.onBackground()
    }
}
