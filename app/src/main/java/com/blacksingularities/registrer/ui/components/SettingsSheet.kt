package com.blacksingularities.registrer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.registrer.BuildConfig
import com.blacksingularities.registrer.data.AppSettings
import com.blacksingularities.registrer.data.AspectRatioMode
import com.blacksingularities.registrer.data.AudioPreset
import com.blacksingularities.registrer.data.AudioSourceType
import com.blacksingularities.registrer.data.GridType
import com.blacksingularities.registrer.data.VideoResolution
import com.blacksingularities.registrer.data.VolumeAction
import com.blacksingularities.registrer.ui.theme.Palette
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: AppSettings,
    locked: Boolean,
    onChange: ((AppSettings) -> AppSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        containerColor = Palette.Panel,
        contentColor = Color.White,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
        ) {
            Text("Ustawienia", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (locked) {
                Text("Część opcji jest zablokowana podczas nagrywania.", color = Palette.TextDim, fontSize = 13.sp)
            }

            Section("Zdjęcia")
            Choice("Proporcje", AspectRatioMode.entries, settings.aspect, { it.label }, !locked) { v -> onChange { it.copy(aspect = v) } }
            Choice("Siatka", GridType.entries, settings.grid, { it.label }) { v -> onChange { it.copy(grid = v) } }
            Choice("Samowyzwalacz", listOf(0, 3, 10), settings.timerSeconds, { if (it == 0) "Wył." else "$it s" }) { v -> onChange { it.copy(timerSeconds = v) } }
            Choice("Klatki trybu Noc", listOf(4, 6, 8, 12), settings.nightFrames, { "×$it" }) { v -> onChange { it.copy(nightFrames = v) } }
            Toggle("Priorytet jakości", "Wolniejsza migawka, lepsza redukcja szumów", settings.maxQuality, !locked) { v -> onChange { it.copy(maxQuality = v) } }
            JpegSlider(settings.jpegQuality, !locked) { v -> onChange { it.copy(jpegQuality = v) } }
            Toggle("Poziomica", "Linia horyzontu z czujnika grawitacji", settings.level) { v -> onChange { it.copy(level = v) } }
            Toggle("Histogram na żywo", "Kontrola prześwietleń i niedoświetleń", settings.histogram, !locked) { v -> onChange { it.copy(histogram = v) } }

            Section("Wideo")
            Choice("Rozdzielczość", VideoResolution.entries, settings.videoResolution, { it.label }, !locked) { v -> onChange { it.copy(videoResolution = v) } }
            Toggle("HDR 10-bit (HLG)", "Jeśli obsługuje urządzenie", settings.videoHdr, !locked) { v -> onChange { it.copy(videoHdr = v) } }
            Toggle("Stabilizacja obrazu", "Cyfrowa / optyczna stabilizacja wideo", settings.stabilization, !locked) { v -> onChange { it.copy(stabilization = v) } }
            Toggle("Dźwięk w wideo", null, settings.videoAudio, !locked) { v -> onChange { it.copy(videoAudio = v) } }
            Toggle("Kinowy zoom", "Wolny, płynny najazd podczas nagrywania", settings.cinematicZoom) { v -> onChange { it.copy(cinematicZoom = v) } }

            Section("Dyktafon")
            Choice("Jakość", AudioPreset.entries, settings.audioPreset, { it.label }, !locked) { v -> onChange { it.copy(audioPreset = v) } }
            Text(settings.audioPreset.description, color = Palette.TextDim, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
            Choice("Źródło", AudioSourceType.entries, settings.audioSource, { it.label }, !locked) { v -> onChange { it.copy(audioSource = v) } }
            Text(settings.audioSource.description, color = Palette.TextDim, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))

            Section("Ogólne")
            Choice("Klawisze głośności", VolumeAction.entries, settings.volumeAction, { it.label }) { v -> onChange { it.copy(volumeAction = v) } }
            Toggle("Dźwięk migawki", null, settings.shutterSound) { v -> onChange { it.copy(shutterSound = v) } }
            Toggle("Wibracje", "Haptyka pokręteł i poziomicy", settings.haptics) { v -> onChange { it.copy(haptics = v) } }

            Spacer(Modifier.height(18.dp))
            Text(
                "Registrer ${BuildConfig.VERSION_NAME} · zdjęcia i wideo w DCIM/Registrer, nagrania w Recordings/Registrer",
                color = Palette.TextFaint,
                fontSize = 11.sp,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title.uppercase(),
        color = Palette.Amber,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.6.sp,
        modifier = Modifier.padding(top = 22.dp, bottom = 6.dp),
    )
}

@Composable
private fun <T> Choice(
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    enabled: Boolean = true,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.padding(vertical = 6.dp)) {
        Text(title, fontSize = 15.sp, color = if (enabled) Color.White else Palette.TextFaint)
        Row(
            Modifier
                .padding(top = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            options.forEach { o ->
                val sel = o == selected
                Text(
                    label(o),
                    color = if (sel) Palette.Ink else if (enabled) Color.White else Palette.TextFaint,
                    fontSize = 13.sp,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (sel) Palette.Amber else Palette.PanelHigh)
                        .clickable(enabled = enabled) { onSelect(o) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun Toggle(title: String, subtitle: String?, value: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onChange(!value) }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = if (enabled) Color.White else Palette.TextFaint)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = Palette.TextDim)
        }
        Switch(
            checked = value,
            onCheckedChange = onChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = Palette.Amber, checkedThumbColor = Palette.Ink),
        )
    }
}

@Composable
private fun JpegSlider(value: Int, enabled: Boolean, onCommit: (Int) -> Unit) {
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column(Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Jakość JPEG", fontSize = 15.sp, modifier = Modifier.weight(1f), color = if (enabled) Color.White else Palette.TextFaint)
            Text("${local.roundToInt()}%", color = Palette.Amber, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onCommit(local.roundToInt()) },
            valueRange = 70f..100f,
            steps = 29,
            enabled = enabled,
            colors = SliderDefaults.colors(thumbColor = Palette.Amber, activeTrackColor = Palette.Amber),
        )
    }
}
