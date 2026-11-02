// Nakładki przeciągane po kadrze, nawigator powiększenia i geometria obrotu „do góry” względem patrzącego.

package com.blacksingularities.registrer.ui

import com.blacksingularities.registrer.i18n.tr
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.wrapContentSize
import com.blacksingularities.registrer.ui.theme.BlockShape
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import com.blacksingularities.registrer.ui.theme.glass
import androidx.compose.material.icons.rounded.Image
import androidx.compose.animation.core.spring
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blacksingularities.registrer.ui.theme.Palette
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Nakładka do przeciągnięcia. Pozycja zapisywana jest tak, jak widzi ją trzymający telefon
 * (0–1 w poziomie i pionie „do góry”), więc po obróceniu telefonu element zostaje np. na górze.
 * Przesuwanie zaczyna się od ruchu palcem albo krótkiego przytrzymania. W trakcie przeciągania
 * nakładka „przykleja się” do środka zdjęcia (w poziomie i pionie), jego krawędzi i krawędzi
 * obszaru – z wibracją i linią pomocniczą. Gest działa przed obrotem treści.
 */
@Composable
internal fun DraggableOverlay(
    rect: Rect,
    start: Offset,
    turn: Int,
    rotation: Float,
    haptics: Boolean,
    deletable: Boolean = true,
    /** Prostokąt zdjęcia – nakładka przyciąga się do jego środka i krawędzi. */
    photo: Rect = rect,
    /** Środek kosza (w układzie obszaru); usuwa palec upuszczony nad nim. */
    trash: Offset = Offset(-1e6f, -1e6f),
    onGuides: (Float?, Float?) -> Unit = { _, _ -> },
    onCommit: (Offset) -> Unit,
    onDragState: (Boolean, Boolean) -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var contentSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val quarter = (((rotation / 90f).roundToInt() % 4) + 4) % 4
    val swap = quarter % 2 == 1
    val boxW = (if (swap) contentSize.height else contentSize.width).toFloat()
    val boxH = (if (swap) contentSize.width else contentSize.height).toFloat()
    val margin = with(density) { 4.dp.toPx() }
    val snapRange = with(density) { 18.dp.toPx() }
    val snapGap = with(density) { 6.dp.toPx() }
    val photoNow by rememberUpdatedState(photo)
    val guidesNow by rememberUpdatedState(onGuides)
    val availW by rememberUpdatedState((rect.width - boxW - 2 * margin).coerceAtLeast(1f))
    val availH by rememberUpdatedState((rect.height - boxH - 2 * margin).coerceAtLeast(1f))
    val commit by rememberUpdatedState(onCommit)
    val dragState by rememberUpdatedState(onDragState)
    val delete by rememberUpdatedState(onDelete)
    val turnNow by rememberUpdatedState(turn)
    // Gest działa przez cały czas życia nakładki – bieżące wymiary i opcje czyta przez stan.
    val rectNow by rememberUpdatedState(rect)
    val boxWNow by rememberUpdatedState(boxW)
    val boxHNow by rememberUpdatedState(boxH)
    val deletableNow by rememberUpdatedState(deletable)
    val trashNow by rememberUpdatedState(trash)
    val trashRadius = with(density) { 38.dp.toPx() }
    val hapticsNow by rememberUpdatedState(haptics)
    // Nowy tryb lub ekran = inna zapamiętana pozycja.
    var upright by remember(start) { mutableStateOf(start) }
    var dragPos by remember { mutableStateOf<Offset?>(null) }
    val target = dragPos ?: overlayToScreen(
        upright = upright,
        turn = turn,
        area = rect,
        photo = photo,
        boxW = boxW,
        boxH = boxH,
        margin = margin,
        availW = availW,
        availH = availH,
    )
    val shown by androidx.compose.animation.core.animateOffsetAsState(
        target,
        if (dragPos != null) androidx.compose.animation.core.snap() else spring(dampingRatio = 0.8f, stiffness = 400f),
        label = "overlayPos",
    )
    val scale by animateFloatAsState(if (dragPos != null) 1.06f else 1f, spring(dampingRatio = 0.55f, stiffness = 600f), label = "dragScale")
    Box(
        Modifier
            .offset {
                IntOffset(
                    (rect.left + margin + shown.x * availW).roundToInt(),
                    (rect.top + margin + shown.y * availH).roundToInt(),
                )
            }
            .size(with(density) { boxW.toDp() }, with(density) { boxH.toDp() })
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val id = down.id
                    var started = false
                    // Pozycja palca bez przyciągania; wyświetlana pozycja „przykleja się” do celów.
                    var raw = Offset.Zero
                    // Palec w układzie kadru – o usunięciu decyduje palec nad koszem, nie środek nakładki.
                    var finger = Offset.Zero
                    var snappedX = false
                    var snappedY = false
                    var centeredX = false
                    var centeredY = false

                    /** Przyciąganie środka nakładki do środka / krawędzi zdjęcia i krawędzi obszaru. */
                    fun magnet(p: Offset): Offset {
                        val area = rectNow
                        val ph = photoNow
                        val bw = boxWNow
                        val bh = boxHNow
                        val cx = area.left + margin + p.x * availW + bw / 2f
                        val cy = area.top + margin + p.y * availH + bh / 2f
                        val xs = listOf(
                            ph.center.x, ph.left + bw / 2f + snapGap, ph.right - bw / 2f - snapGap,
                            area.left + margin + bw / 2f, area.left + margin + availW + bw / 2f,
                        )
                        val ys = listOf(
                            ph.center.y, ph.top + bh / 2f + snapGap, ph.bottom - bh / 2f - snapGap,
                            area.top + margin + bh / 2f, area.top + margin + availH + bh / 2f,
                        )
                        val tx = xs.minByOrNull { abs(it - cx) }?.takeIf { abs(it - cx) <= snapRange }
                        val ty = ys.minByOrNull { abs(it - cy) }?.takeIf { abs(it - cy) <= snapRange }
                        val nowX = tx != null
                        val nowY = ty != null
                        if (hapticsNow && ((nowX && !snappedX) || (nowY && !snappedY))) {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        }
                        snappedX = nowX
                        snappedY = nowY
                        centeredX = tx == xs[0]
                        centeredY = ty == ys[0]
                        // Linie tylko dla celów związanych ze zdjęciem (środek i krawędzie kadru).
                        guidesNow(
                            tx?.takeIf { it == xs[0] || it == xs[1] || it == xs[2] }?.let { if (it == xs[0]) it else if (it == xs[1]) ph.left else ph.right },
                            ty?.takeIf { it == ys[0] || it == ys[1] || it == ys[2] }?.let { if (it == ys[0]) it else if (it == ys[1]) ph.top else ph.bottom },
                        )
                        return Offset(
                            if (tx != null) ((tx - bw / 2f - area.left - margin) / availW).coerceIn(0f, 1f) else p.x,
                            if (ty != null) ((ty - bh / 2f - area.top - margin) / availH).coerceIn(0f, 1f) else p.y,
                        )
                    }

                    fun move(delta: Offset) {
                        if (dragPos == null) return
                        raw = Offset(
                            (raw.x + delta.x / availW).coerceIn(0f, 1f),
                            (raw.y + delta.y / availH).coerceIn(0f, 1f),
                        )
                        finger += delta
                        val next = magnet(raw)
                        dragPos = next
                        dragState(deletableNow, deletableNow && (finger - trashNow).getDistance() <= trashRadius)
                    }

                    fun begin() {
                        started = true
                        raw = overlayToScreen(
                            upright = upright,
                            turn = turnNow,
                            area = rectNow,
                            photo = photoNow,
                            boxW = boxWNow,
                            boxH = boxHNow,
                            margin = margin,
                            availW = availW,
                            availH = availH,
                        )
                        val q = normalizedQuarterTurn(turnNow)
                        centeredX = if (q % 2 == 0) isCenterAnchor(upright.x) else isCenterAnchor(upright.y)
                        centeredY = if (q % 2 == 0) isCenterAnchor(upright.y) else isCenterAnchor(upright.x)
                        dragPos = raw
                        finger = Offset(
                            rectNow.left + margin + raw.x * availW,
                            rectNow.top + margin + raw.y * availH,
                        ) + down.position
                        dragState(deletableNow, false)
                        if (hapticsNow) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    }

                    // Przeciąganie zaczyna się od razu po ruchu palcem albo po krótkim przytrzymaniu;
                    // samo dotknięcie zostaje dla treści (np. zmiana trybu histogramu).
                    var pending = Offset.Zero
                    while (!started) {
                        val remaining = OVERLAY_HOLD_MS - (currentEvent.changes.firstOrNull()?.uptimeMillis ?: down.uptimeMillis).minus(down.uptimeMillis)
                        val event = if (remaining > 0) withTimeoutOrNull(remaining) { awaitPointerEvent() } else null
                        if (event == null) {
                            begin()
                            break
                        }
                        if (event.changes.size > 1) return@awaitEachGesture
                        val change = event.changes.firstOrNull { it.id == id } ?: return@awaitEachGesture
                        if (!change.pressed || change.isConsumed) return@awaitEachGesture
                        pending += change.positionChange()
                        if (pending.getDistance() > viewConfiguration.touchSlop) {
                            change.consume()
                            begin()
                            move(pending)
                        }
                    }

                    var cancelled = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == id }
                        if (change == null) {
                            cancelled = true
                            break
                        }
                        if (!change.pressed) {
                            change.consume()
                            break
                        }
                        val delta = change.positionChange()
                        change.consume()
                        if (delta != Offset.Zero) move(delta)
                    }

                    val p = dragPos
                    dragPos = null
                    dragState(false, false)
                    guidesNow(null, null)
                    if (p == null) return@awaitEachGesture
                    if (cancelled) return@awaitEachGesture
                    if (deletableNow && (finger - trashNow).getDistance() <= trashRadius) {
                        if (hapticsNow) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        delete()
                        return@awaitEachGesture
                    }
                    // Pozycja już przyciągnięta w trakcie przeciągania – bez dodatkowego zaokrąglania.
                    var snapped = screenToUpright(p, turnNow)
                    val q = normalizedQuarterTurn(turnNow)
                    if (centeredX) {
                        snapped = if (q % 2 == 0) Offset(CENTER_ANCHOR, snapped.y) else Offset(snapped.x, CENTER_ANCHOR)
                    }
                    if (centeredY) {
                        snapped = if (q % 2 == 0) Offset(snapped.x, CENTER_ANCHOR) else Offset(CENTER_ANCHOR, snapped.y)
                    }
                    upright = snapped
                    if (hapticsNow) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    commit(snapped)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .wrapContentSize(unbounded = true)
                .onSizeChanged { contentSize = it }
                .graphicsLayer {
                    rotationZ = rotation
                    scaleX = scale
                    scaleY = scale
                },
        ) { content() }
    }
}

@Composable
internal fun ZoomNavigator(bitmap: ImageBitmap, zoom: Float, baseZoom: Float) {
    val cropFraction = (baseZoom / zoom).coerceIn(0.08f, 1f)
    Box(
        Modifier
            .size(120.dp, 82.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
            .border(1.dp, Color.White.copy(alpha = 0.7f), RoundedCornerShape(8.dp)),
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = tr("Nawigator dużego zoomu", "High-zoom navigator"),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width * cropFraction
            val h = size.height * cropFraction
            val left = (size.width - w) / 2f
            val top = (size.height - h) / 2f
            val shade = Color.Black.copy(alpha = 0.34f)
            drawRect(shade, topLeft = Offset.Zero, size = Size(size.width, top))
            drawRect(shade, topLeft = Offset(0f, top + h), size = Size(size.width, size.height - top - h))
            drawRect(shade, topLeft = Offset(0f, top), size = Size(left, h))
            drawRect(shade, topLeft = Offset(left + w, top), size = Size(size.width - left - w, h))
            drawRect(
                color = Palette.Amber,
                topLeft = Offset(left, top),
                size = Size(w, h),
                style = Stroke(width = 2.dp.toPx()),
            )
            val cross = 5.dp.toPx()
            drawLine(Palette.Amber, Offset(center.x - cross, center.y), Offset(center.x + cross, center.y), 1.dp.toPx())
            drawLine(Palette.Amber, Offset(center.x, center.y - cross), Offset(center.x, center.y + cross), 1.dp.toPx())
        }
        Text(
            "${com.blacksingularities.registrer.ui.components.formatZoom(zoom)}×",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(bottomEnd = 6.dp))
                .padding(horizontal = 5.dp, vertical = 2.dp),
        )
    }
}

/** Obrót punktu (0–1) wokół środka o wielokrotność 90° (zgodnie z ruchem wskazówek). */
internal fun rotateUnit(p: Offset, degrees: Int): Offset {
    val dx = p.x - 0.5f
    val dy = p.y - 0.5f
    return when (((degrees % 360) + 360) % 360) {
        90 -> Offset(0.5f - dy, 0.5f + dx)
        180 -> Offset(0.5f - dx, 0.5f - dy)
        270 -> Offset(0.5f + dy, 0.5f - dx)
        else -> p
    }
}

internal fun uprightToScreen(p: Offset, turn: Int) = rotateUnit(p, turn)
internal fun screenToUpright(p: Offset, turn: Int) = rotateUnit(p, -turn)

private const val CENTER_ANCHOR = 0.5f
private const val CENTER_ANCHOR_EPSILON = 0.0001f

internal fun normalizedQuarterTurn(turn: Int) = (((turn / 90) % 4) + 4) % 4

internal fun isCenterAnchor(value: Float) = abs(value - CENTER_ANCHOR) <= CENTER_ANCHOR_EPSILON

/**
 * Zamienia pozycję zapisaną względem telefonu na ekranową, zachowując środek kadru jako
 * zakotwiczenie, a nie zwykłą współrzędną. Dzięki temu po obrocie środek poziomy staje się
 * dokładnie środkiem pionowym także wtedy, gdy środek kadru nie pokrywa się ze środkiem
 * obszaru dostępnego dla nakładek.
 */
internal fun overlayToScreen(
    upright: Offset,
    turn: Int,
    area: Rect,
    photo: Rect,
    boxW: Float,
    boxH: Float,
    margin: Float,
    availW: Float,
    availH: Float,
): Offset {
    val rotated = uprightToScreen(upright, turn)
    val centerX = ((photo.center.x - boxW / 2f - area.left - margin) / availW).coerceIn(0f, 1f)
    val centerY = ((photo.center.y - boxH / 2f - area.top - margin) / availH).coerceIn(0f, 1f)
    val q = normalizedQuarterTurn(turn)
    val screenXCentered = if (q % 2 == 0) isCenterAnchor(upright.x) else isCenterAnchor(upright.y)
    val screenYCentered = if (q % 2 == 0) isCenterAnchor(upright.y) else isCenterAnchor(upright.x)
    return Offset(
        if (screenXCentered) centerX else rotated.x,
        if (screenYCentered) centerY else rotated.y,
    )
}

/** Po tylu ms przytrzymania nakładka daje się przesuwać także bez wcześniejszego ruchu. */
private const val OVERLAY_HOLD_MS = 280L

/**
 * Warstwa ułożona w prostokącie kadru, obrócona o [turn] stopni. Przy obrocie o 90°/270°
 * szerokość i wysokość zamieniają się, więc treść wypełnia kadr tak, jak widzi go użytkownik.
 * Zmiana orientacji to krótkie przenikanie zamiast „pływającego” tekstu.
 */
@Composable
internal fun UprightFrame(rect: Rect, turn: Int, content: @Composable BoxScope.() -> Unit) {
    val density = LocalDensity.current
    if (rect.width <= 0f) return
    val alpha = remember { Animatable(1f) }
    var shown by remember { mutableIntStateOf(turn) }
    LaunchedEffect(turn) {
        if (turn == shown) return@LaunchedEffect
        alpha.animateTo(0f, tween(110))
        shown = turn
        alpha.animateTo(1f, tween(200))
    }
    val swap = shown % 180 != 0
    val w = with(density) { (if (swap) rect.height else rect.width).toDp() }
    val h = with(density) { (if (swap) rect.width else rect.height).toDp() }
    Box(
        Modifier
            .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
            .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() }),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .requiredSize(w, h)
                .graphicsLayer {
                    rotationZ = shown.toFloat()
                    this.alpha = alpha.value
                },
            content = content,
        )
    }
}

@Composable
internal fun BoxScope.FrameInfo(ui: Ui) {
    val vm = ui.vm
    Column(
        Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (vm.focusLocked) Badge(tr("Ostrość i ekspozycja zablokowane  ✕", "Focus and exposure locked  ✕"), color = Palette.Amber, onClick = vm::unlockFocus)
                if (vm.intervalRunning) {
                    val total = vm.cfg.intervalCount
                    Badge(tr("Interwał · ${vm.intervalShot}${if (total > 0) "/$total" else ""}", "Interval · ${vm.intervalShot}${if (total > 0) "/$total" else ""}"), color = Palette.Amber)
                }
                if (vm.rearDisplayActive) Badge(tr("Autoportret głównym aparatem", "Selfie with the main camera"), color = Palette.Amber)
                if (vm.tlRunning) TimelapseBadge(vm)
            }
        }
        AnimatedVisibility(
            visible = vm.message != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                vm.message ?: "",
                color = Color.White,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .glass(RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }

    AnimatedVisibility(
        visible = vm.countdown != null,
        enter = fadeIn() + scaleIn(initialScale = 1.6f),
        exit = fadeOut() + scaleOut(targetScale = 0.6f),
        modifier = Modifier.align(Alignment.Center),
    ) {
        Text("${vm.countdown ?: ""}", fontSize = 120.sp, fontWeight = FontWeight.Thin, color = Color.White)
    }
    vm.processing?.let { p ->
        Text(
            p.label,
            color = Palette.Amber,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
                .glass(BlockShape)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}
