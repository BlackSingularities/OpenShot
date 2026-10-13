package com.blacksingularities.registrer.ui.theme

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** Źródło rozmycia – żywy obraz kamery. Brak (null) = przyciemniona półprzezroczystość. */
val LocalHaze = staticCompositionLocalOf<HazeState?> { null }

/** Czy wykonywać haptykę przy dotyku elementów interfejsu. */
val LocalHaptics = staticCompositionLocalOf { true }

/** Wewnątrz szklanego paska narzędzi przyciski są płaskie (bez własnego szkła). */
val LocalToolbar = staticCompositionLocalOf { false }

/** Dymki z nazwą elementu pokazywane po przytrzymaniu. */
val LocalHints = staticCompositionLocalOf<HintState?> { null }

enum class GlassLevel(val tint: Color, val blur: Dp) {
    /** Przyciski i plakietki nad kadrem. */
    Thin(Color(0x2E10131A), 18.dp),
    /** Panele sterowania. */
    Regular(Color(0x5210131A), 26.dp),
    /** Arkusze z dużą ilością tekstu (ustawienia). */
    Thick(Color(0xB012141B), 34.dp),
}

/** Aktywny element to pełny bursztynowy blok z ciemną ikoną. */
private val ActiveTint = Color(0xF2FFB547)

/** Kształt bloków: prostokąt z małym zaokrągleniem zamiast kół i pigułek. */
val BlockShape = RoundedCornerShape(4.dp)

/**
 * Szkło: rozmyty obraz kamery pod spodem, delikatne zabarwienie, ziarno i obwódka ze
 * światłem padającym z góry. Bez rozmycia (Android < 12) – przyciemniona półprzezroczystość.
 */
@Composable
fun Modifier.glass(
    shape: Shape,
    level: GlassLevel = GlassLevel.Thin,
    active: Boolean = false,
    border: Boolean = true,
): Modifier {
    val haze = LocalHaze.current
    val tint by animateColorAsState(if (active) ActiveTint else level.tint, tween(220), label = "glassTint")
    val clipped = this.clip(shape)
    val blurred = if (haze != null) {
        clipped.hazeEffect(haze) {
            blurRadius = level.blur
            backgroundColor = Color.Black
            tints = listOf(HazeTint(tint))
            noiseFactor = 0f
        }
    } else {
        clipped.background(tint.copy(alpha = (tint.alpha + 0.35f).coerceAtMost(0.9f)))
    }
    // Surowe bloki – bez obwódek i podświetleń krawędzi.
    return blurred
}

/** Stan dymków: tekst i prostokąt elementu (w układzie okna), przy którym dymek się pojawia. */
@Stable
class HintState {
    var text by mutableStateOf<String?>(null)
        private set
    var anchor by mutableStateOf(Rect.Zero)
        private set
    var token by mutableStateOf(0)
        private set

    fun show(text: String, anchor: Rect) {
        this.text = text
        this.anchor = anchor
        token++
    }

    fun hide() {
        text = null
    }
}

/** Zapewnia szkło, haptykę i warstwę dymków całemu drzewu interfejsu. */
@Composable
fun GlassScope(haze: HazeState?, haptics: Boolean, content: @Composable BoxScope.() -> Unit) {
    val hints = remember { HintState() }
    CompositionLocalProvider(LocalHaze provides haze, LocalHaptics provides haptics, LocalHints provides hints) {
        Box(Modifier.fillMaxSize()) {
            content()
            HintLayer(hints)
        }
    }
}

@Composable
private fun HintLayer(hints: HintState) {
    val density = LocalDensity.current
    var bubble by remember { mutableStateOf(IntSize.Zero) }
    var rootOffset by remember { mutableStateOf(Rect.Zero) }
    LaunchedEffect(hints.token) {
        if (hints.text != null) {
            delay(1600)
            hints.hide()
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { rootOffset = it.boundsInRoot() },
    ) {
        val text = hints.text
        AnimatedVisibility(
            visible = text != null,
            enter = fadeIn(tween(140)) + scaleIn(initialScale = 0.85f),
            exit = fadeOut(tween(160)) + scaleOut(targetScale = 0.92f),
            modifier = Modifier.offset {
                val a = hints.anchor
                val gap = with(density) { 10.dp.roundToPx() }
                val x = (a.center.x - rootOffset.left - bubble.width / 2f).roundToInt()
                val above = (a.top - rootOffset.top - bubble.height - gap).roundToInt()
                val y = if (above > gap) above else (a.bottom - rootOffset.top + gap).roundToInt()
                val maxX = (rootOffset.width - bubble.width - gap).roundToInt().coerceAtLeast(gap)
                IntOffset(x.coerceIn(gap, maxX), y)
            },
        ) {
            Text(
                text ?: "",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .onSizeChanged { bubble = it }
                    .glass(RoundedCornerShape(4.dp), GlassLevel.Regular)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

/**
 * Reaktywna powierzchnia dotyku: sprężyste wciśnięcie, haptyka przy dotknięciu i po
 * przytrzymaniu dymek z nazwą ([label]). [onLongPress] zastępuje dymek, jeśli podany.
 */
@Composable
fun Modifier.reactive(
    label: String?,
    enabled: Boolean = true,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier {
    val haptic = LocalHapticFeedback.current
    val haptics = LocalHaptics.current
    val hints = LocalHints.current
    var pressed by remember { mutableStateOf(false) }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val click by rememberUpdatedState(onClick)
    val long by rememberUpdatedState(onLongPress)
    val scale by animateFloatAsState(
        if (pressed) 0.86f else 1f,
        spring(dampingRatio = 0.45f, stiffness = 700f),
        label = "press",
    )
    return this
        .onGloballyPositioned { bounds = it.boundsInRoot() }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = if (enabled) 1f else 0.38f
        }
        .then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier)
        .pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = {
                    pressed = true
                    if (haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    tryAwaitRelease()
                    pressed = false
                },
                onLongPress = {
                    if (haptics) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val l = long
                    if (l != null) l() else if (label != null) hints?.show(label, bounds)
                },
                onTap = { click() },
            )
        }
}

/** Okrągły szklany przycisk z ikoną; zmiana ikony – płynne przenikanie z obrotem. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    label: String,
    rotation: Float,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    active: Boolean = false,
    enabled: Boolean = true,
    tint: Color? = null,
    badge: String? = null,
    level: GlassLevel = GlassLevel.Thin,
    onLongPress: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val iconTint by animateColorAsState(tint ?: if (active) Palette.Ink else Color.White, tween(200), label = "iconTint")
    Box(
        modifier
            .reactive(label, enabled, onLongPress, onClick)
            .size(size)
            .buttonSurface(BlockShape, level, active),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.graphicsLayer { rotationZ = rotation }, contentAlignment = Alignment.Center) {
            AnimatedContent(
                targetState = icon,
                transitionSpec = { (fadeIn(tween(160)) + scaleIn(initialScale = 0.6f)) togetherWith (fadeOut(tween(120)) + scaleOut(targetScale = 0.6f)) },
                label = "icon",
            ) { ic ->
                Icon(ic, null, tint = iconTint, modifier = Modifier.size(size * 0.5f))
            }
            if (badge != null) {
                Text(
                    badge,
                    color = Palette.Ink,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 9.dp, y = 7.dp)
                        .background(Palette.Amber, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp),
                )
            }
        }
    }
}

/** Tło przycisku: własne szkło, a w pasku narzędzi – płaskie, z bursztynem dla aktywnych. */
@Composable
private fun Modifier.buttonSurface(shape: Shape, level: GlassLevel, active: Boolean): Modifier {
    if (!LocalToolbar.current) return glass(shape, level, active = active)
    val bg by animateColorAsState(if (active) ActiveTint else Color.Transparent, tween(200), label = "flatBg")
    return this.clip(shape).background(bg)
}

/**
 * Okrągły przycisk z wartością (np. „4K” nad „2160”, „24” nad „kl/s”). Mieści się w kole,
 * więc po obróceniu telefonu nic się nie obcina.
 */
@Composable
fun GlassValueButton(
    value: String,
    unit: String?,
    label: String,
    rotation: Float,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val color by animateColorAsState(if (active) Palette.Ink else Color.White, tween(200), label = "valueColor")
    Box(
        modifier
            .reactive(label, enabled, null, onClick)
            .size(size)
            .buttonSurface(BlockShape, GlassLevel.Thin, active),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.layout.Column(
            Modifier.graphicsLayer { rotationZ = rotation },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedContent(
                targetState = value,
                transitionSpec = { (fadeIn(tween(150)) + scaleIn(initialScale = 0.7f)) togetherWith (fadeOut(tween(100)) + scaleOut(targetScale = 0.7f)) },
                label = "value",
            ) { v ->
                Text(
                    v,
                    color = color,
                    fontSize = if (v.length > 3) 11.sp else 14.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    style = androidx.compose.ui.text.TextStyle(lineHeight = 14.sp),
                )
            }
            if (unit != null) {
                Text(
                    unit,
                    color = color.copy(alpha = 0.7f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                    maxLines = 1,
                    style = androidx.compose.ui.text.TextStyle(lineHeight = 9.sp),
                )
            }
        }
    }
}

/** Szklany pasek narzędzi: jedna kapsuła z płaskimi przyciskami w środku. */
@Composable
fun GlassToolbar(
    vertical: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalToolbar provides true) {
        if (vertical) {
            androidx.compose.foundation.layout.Column(
                modifier
                    .glass(RoundedCornerShape(6.dp), GlassLevel.Regular)
                    .padding(4.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { content() }
        } else {
            Row(
                modifier
                    .glass(RoundedCornerShape(6.dp), GlassLevel.Regular)
                    .padding(4.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { content() }
        }
    }
}

/** Szklana pigułka z krótkim tekstem (np. „4K”, „60”), opcjonalnie z ikoną. */
@Composable
fun GlassChip(
    text: String,
    label: String,
    rotation: Float,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val color by animateColorAsState(if (active) Palette.Ink else Color.White, tween(200), label = "chipText")
    Box(
        modifier
            .reactive(label, enabled, null, onClick)
            .height(44.dp)
            .glass(BlockShape, active = active)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(Modifier.graphicsLayer { rotationZ = rotation }, verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
            }
            AnimatedContent(
                targetState = text,
                transitionSpec = { (fadeIn(tween(150)) + scaleIn(initialScale = 0.8f)) togetherWith fadeOut(tween(100)) },
                label = "chip",
            ) { t ->
                Text(t, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
