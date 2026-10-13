package com.encorevpn.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sin

private fun accentFor(mode: Mode): Color = when (mode) {
    Mode.On, Mode.Connecting -> Palette.brand
    Mode.Captcha -> Palette.alert
    Mode.Off -> Palette.idle
}

/** Главный экран. */
@Composable
fun HomeScreen(
    geo: ServerGeo?,
    ping: Int?,
    onPower: () -> Unit,
    onAddKey: () -> Unit,
    onOpenCaptcha: () -> Unit,
) {
    val mode = Model.mode
    val cfg = Model.config
    val haptic = LocalHapticFeedback.current

    val headline = when (mode) {
        Mode.On -> "Обход включён"
        Mode.Connecting -> if (Model.phase == Phase.Disconnecting) "Отключаюсь" else "Подключаюсь"
        Mode.Captcha -> "Нужна капча"
        Mode.Off -> if (cfg == null) "Нет ключа" else "Обход выключен"
    }
    val caption = when (mode) {
        Mode.On -> "Трафик идёт через сервер encoreVPN, белые списки вам не мешают"
        Mode.Connecting -> "Ищем путь через звонок ВКонтакте"
        Mode.Captcha -> "ВКонтакте просит подтвердить, что вы человек. Пока этого нет, интернета нет."
        Mode.Off -> if (cfg == null) "Добавьте ключ от бота, чтобы начать" else "Нажмите на кнопку, чтобы подключиться"
    }
    val daysLeft = when {
        cfg == null -> "—"
        cfg.expires <= 0L -> "без срока"
        else -> "${max(0, ceil((cfg.expires - System.currentTimeMillis() / 1000.0) / 86400.0).toInt())} дн."
    }
    val host = cfg?.peer?.substringBefore(':')
    // последний октет скрываем, чтобы адрес сервера не светился на скриншотах
    val serverText = host?.let { h -> h.split(".").let { p -> if (p.size == 4) "${p[0]}.${p[1]}.•.•" else h } } ?: "—"
    val countryText = geo?.country ?: if (cfg == null) "—" else "Определяю…"
    val pingText = ping?.let { "$it мс" } ?: "—"

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val compact = maxHeight < 700.dp
        val minH = maxHeight
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = minH)
                .padding(start = 18.dp, end = 18.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Header(onAddKey)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PowerButton(
                    mode = mode, accent = accentFor(mode),
                    enabled = Model.phase == Phase.Idle, dimmed = cfg == null,
                    size = if (compact) 128.dp else 148.dp,
                ) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPower()
                }
                Spacer(Modifier.height(if (compact) 20.dp else 32.dp))

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Crossfade(headline, animationSpec = tween(400), label = "headline") {
                        T(it, color = Color.White, size = 34.sp, weight = FontWeight.SemiBold, align = TextAlign.Center)
                    }
                    Crossfade(caption, animationSpec = tween(400), label = "caption") {
                        T(it, Modifier.padding(horizontal = 10.dp), color = Palette.muted, size = 16.sp, align = TextAlign.Center)
                    }
                }

                SessionTimer(since = if (mode == Mode.On) Model.connectedAt else null, Modifier.padding(top = 18.dp))

                AnimatedVisibility(
                    visible = mode == Mode.Captcha,
                    enter = scaleIn(initialScale = 0.85f) + fadeIn(),
                    exit = scaleOut(targetScale = 0.85f) + fadeOut(),
                ) {
                    Box(
                        Modifier.padding(top = 14.dp)
                            .pressClickable(0.96f, onClick = onOpenCaptcha)
                            .clip(RoundedCornerShape(12.dp)).background(Palette.alert)
                            .padding(horizontal = 28.dp, vertical = 13.dp),
                    ) { T("Пройти капчу", color = Color.Black.copy(alpha = 0.85f), size = 17.sp, weight = FontWeight.SemiBold) }
                }
                AnimatedVisibility(
                    visible = mode != Mode.Captcha && cfg == null,
                    enter = scaleIn(initialScale = 0.85f) + fadeIn(),
                    exit = scaleOut(targetScale = 0.85f) + fadeOut(),
                ) {
                    Box(
                        Modifier.padding(top = 14.dp)
                            .pressClickable(0.96f, onClick = onAddKey)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(Palette.brand, Palette.brandDark)))
                            .padding(horizontal = 28.dp, vertical = 13.dp),
                    ) { T("Добавить ключ", color = Color.White, size = 17.sp, weight = FontWeight.SemiBold) }
                }
            }

            ServerCard(
                mapHeight = if (compact) 100.dp else 140.dp, mode = mode, geo = geo,
                serverText = serverText, countryText = countryText, pingText = pingText, daysText = daysLeft,
            )
        }
    }
}

// MARK: Шапка

@Composable
private fun Header(onAddKey: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(top = 6.dp).heightIn(min = 44.dp), contentAlignment = Alignment.Center) {
        Row {
            T("encore", color = Color.White, size = 24.sp, weight = FontWeight.Bold, spacing = (-0.4).sp, shadow = LegibleShadow)
            T("Turn", color = Palette.brandLight, size = 24.sp, weight = FontWeight.Bold, spacing = (-0.4).sp, shadow = LegibleShadow)
        }
        Box(
            Modifier.align(Alignment.CenterEnd)
                .semantics { contentDescription = "Добавить или заменить ключ" }
                .pressClickable(onClick = onAddKey)
                .size(44.dp).clip(CircleShape).background(Palette.brand.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) { Glyph(Ico.LinkPlus, Palette.brandLight, 22.dp, stroke = 1.6f) }
    }
}

// MARK: Карточка сервера

@Composable
private fun ServerCard(
    mapHeight: Dp, mode: Mode, geo: ServerGeo?,
    serverText: String, countryText: String, pingText: String, daysText: String,
) {
    val shape = RoundedCornerShape(30.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Palette.glass).border(1.dp, Palette.hairline, shape).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            T("СЕРВЕР", Modifier.weight(1f), color = Palette.muted, size = 13.sp, weight = FontWeight.SemiBold, spacing = 3.sp)
            T(serverText, color = Palette.muted, size = 14.sp, mono = true)
        }
        val mapShape = RoundedCornerShape(18.dp)
        ServerMap(
            geo = geo, lit = mode == Mode.On,
            modifier = Modifier.fillMaxWidth().height(mapHeight).clip(mapShape)
                .border(BorderStroke(1.dp, Palette.brand.copy(alpha = 0.25f)), mapShape),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                InfoChip(text = countryText) { T(geo?.flag ?: "🌐", size = 17.sp) }
            }
            InfoChip(text = pingText) { Glyph(Ico.Waves, Palette.brandLight, 18.dp, stroke = 1.6f) }
            InfoChip(text = daysText) { Glyph(Ico.Calendar, Palette.brandLight, 17.dp, stroke = 1.7f) }
        }
    }
}

@Composable
private fun InfoChip(text: String, leading: @Composable () -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(Palette.chip).padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Spacer(Modifier.width(8.dp))
        T(text, color = Color.White, size = 15.sp, weight = FontWeight.Medium, maxLines = 1)
    }
}

// MARK: Таймер сессии

@Composable
private fun SessionTimer(since: Long?, modifier: Modifier = Modifier) {
    val now by produceState(System.currentTimeMillis()) {
        while (true) { value = System.currentTimeMillis(); delay(1000) }
    }
    val a by animateFloatAsState(if (since == null) 0.45f else 1f, tween(500), label = "timerA")
    val secs = if (since == null) 0 else max(0, ((now - since) / 1000).toInt())
    val label = if (secs >= 3600) "%d:%02d:%02d".format(secs / 3600, (secs / 60) % 60, secs % 60)
    else "%02d:%02d".format(secs / 60, secs % 60)
    Box(
        modifier.graphicsLayer { alpha = a }.clip(CircleShape).background(Palette.chip)
            .padding(horizontal = 26.dp, vertical = 10.dp),
    ) { T(label, color = Color.White.copy(alpha = 0.92f), size = 22.sp, weight = FontWeight.Medium, mono = true) }
}

// MARK: Большая кнопка

@Composable
private fun PowerButton(mode: Mode, accent: Color, enabled: Boolean, dimmed: Boolean, size: Dp, onClick: () -> Unit) {
    val active = mode == Mode.On || mode == Mode.Connecting
    val spinning = mode == Mode.Connecting || mode == Mode.Captcha
    val tw = tween<Float>(550)
    val activeA by animateFloatAsState(if (active) 1f else 0f, tw, label = "act")
    val capA by animateFloatAsState(if (mode == Mode.Captcha) 1f else 0f, tw, label = "cap")
    val glowA by animateFloatAsState(if (mode == Mode.Off) 0.12f else 0.35f, tw, label = "glow")
    val spinA by animateFloatAsState(if (spinning) 1f else 0f, tween(300), label = "spinA")
    val rootA by animateFloatAsState(if (enabled && !dimmed) 1f else 0.45f, tween(300), label = "rootA")
    val accentC by animateColorAsState(accent, tween(550), label = "accent")
    val iconC by animateColorAsState(if (mode == Mode.Off) Palette.brandLight else Color.White, tween(550), label = "icon")
    val borderC by animateColorAsState(if (mode == Mode.Captcha) Palette.alert else Palette.brand, tween(550), label = "border")

    // волны: один раз плавно появляются при подключении и плавно гаснут
    var burst by remember { mutableIntStateOf(0) }
    var lastMode by remember { mutableStateOf(Mode.Off) }
    val burstT = remember { Animatable(0f) }
    val duration = 2.8f
    val stagger = 0.45f
    val total = duration + stagger * 3 + 0.2f
    LaunchedEffect(mode) {
        if (mode == Mode.Connecting && lastMode == Mode.Off) burst++
        lastMode = mode
    }
    LaunchedEffect(burst) {
        if (burst > 0) {
            burstT.snapTo(0f)
            burstT.animateTo(total, tween((total * 1000).toInt(), easing = LinearEasing))
        }
    }

    val btn = size
    val halo = btn * 240f / 170f
    Box(
        Modifier
            .size(size)
            .graphicsLayer { alpha = rootA }
            .semantics { contentDescription = if (mode == Mode.Off) "Подключиться" else "Отключиться" }
            .pressClickable(0.95f, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // свечение, тонкое кольцо и волны
        Canvas(Modifier.requiredSize(halo)) {
            val c = center
            val r = this.size.minDimension / 2f
            val g = accentC.copy(alpha = glowA)
            drawCircle(
                Brush.radialGradient(0f to g, 0.55f to g.copy(alpha = glowA * 0.7f), 1f to Color.Transparent, center = c, radius = r * 1.35f),
                r * 1.35f, c,
            )
            drawCircle(Palette.brandLight.copy(alpha = 0.35f), btn.toPx() * 200f / 170f / 2f, c, style = Stroke(1.dp.toPx()))
            val e = burstT.value
            for (i in 0 until 3) {
                val p = ((e - i * stagger) / duration).coerceIn(0f, 1f)
                if (p <= 0f || p >= 1f) continue
                drawCircle(
                    Palette.brandLight.copy(alpha = (sin(PI * p) * 0.4).toFloat()),
                    r * (1.05f + p * 0.85f), c, style = Stroke(1.2.dp.toPx()),
                )
            }
        }

        // тело кнопки: три слоя с плавным перетеканием
        Canvas(Modifier.size(size)) {
            val r = this.size.minDimension / 2f
            drawCircle(Brush.verticalGradient(listOf(Palette.card, Palette.card)), r)
            drawCircle(Brush.verticalGradient(listOf(Palette.brand, Palette.brandDark)), r, alpha = activeA)
            drawCircle(Brush.verticalGradient(listOf(Palette.alert, Palette.captchaBottom)), r, alpha = capA)
            drawCircle(borderC, r - 0.75.dp.toPx(), style = Stroke(1.5.dp.toPx()))
        }

        if (spinA > 0.01f) SpinRing(size, spinA)

        Glyph(Ico.Power, iconC, size * 0.46f, stroke = 1.9f)
    }
}

/** Дуга с угасающим хвостом кометы. */
@Composable
private fun SpinRing(size: Dp, alpha: Float) {
    val angle by rememberInfiniteTransition(label = "spin")
        .animateFloat(0f, 360f, infiniteRepeatable(tween(1150, easing = LinearEasing), RepeatMode.Restart), label = "angle")
    Canvas(Modifier.size(size)) {
        val inset = 4.dp.toPx()
        val sw = 5.dp.toPx()
        val c = center
        val brush = Brush.sweepGradient(
            0f to Color.White.copy(alpha = 0f),
            0.72f to Color.White.copy(alpha = 0.95f),
            1f to Color.White.copy(alpha = 0.95f),
            center = c,
        )
        rotate(angle, pivot = c) {
            drawArc(
                brush = brush, startAngle = 0f, sweepAngle = 0.72f * 360f, useCenter = false,
                topLeft = Offset(inset + sw / 2, inset + sw / 2),
                size = Size(this.size.width - 2 * inset - sw, this.size.height - 2 * inset - sw),
                alpha = alpha, style = Stroke(sw, cap = StrokeCap.Round),
            )
        }
    }
}
