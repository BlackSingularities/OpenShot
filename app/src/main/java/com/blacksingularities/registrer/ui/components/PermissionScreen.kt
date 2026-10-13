package com.blacksingularities.registrer.ui.components

import com.blacksingularities.registrer.i18n.tr
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.registrer.ui.theme.Palette
import com.blacksingularities.registrer.R

@Composable
fun PermissionScreen(onRequest: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.Ink)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.openshot_icon_foreground),
            contentDescription = tr("Ikona OpenShot", "OpenShot icon"),
            modifier = Modifier.size(112.dp),
        )
        Spacer(Modifier.height(28.dp))
        Text("OpenShot", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(
            tr("Aby robić zdjęcia, filmować i nagrywać dźwięk, aplikacja potrzebuje dostępu do aparatu i mikrofonu. Nic nie opuszcza Twojego telefonu.", "To take photos, shoot video and record sound, the app needs access to the camera and microphone. Nothing leaves your phone."),
            color = Palette.TextDim,
            textAlign = TextAlign.Center,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(containerColor = Palette.Amber, contentColor = Palette.Ink),
        ) { Text(tr("Zezwól na dostęp", "Allow access"), fontWeight = FontWeight.Bold) }
        TextButton(onClick = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            runCatching { context.startActivity(intent) }
        }) { Text(tr("Otwórz ustawienia systemu", "Open system settings"), color = Palette.TextDim) }
    }
}
