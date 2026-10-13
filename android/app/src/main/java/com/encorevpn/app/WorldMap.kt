package com.encorevpn.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.floor

/** Маска суши из WorldData (шаг 1.5°). */
private val land: Array<BooleanArray> by lazy {
    Array(WorldData.ROWS) { r ->
        val hex = WorldData.ROWS_HEX[r]
        BooleanArray(WorldData.COLS) { c ->
            val nib = Character.digit(hex[c / 4], 16)
            (nib shr (3 - c % 4)) and 1 == 1
        }
    }
}

/**
 * Карта сервера из точек: офлайн, без ключей и сторонних SDK (на iOS здесь Apple Maps).
 * Пока место неизвестно, показывает обзор без метки; когда оно найдено, плавно наезжает на сервер.
 */
@Composable
fun ServerMap(geo: ServerGeo?, lit: Boolean, modifier: Modifier = Modifier) {
    val ease = tween<Float>(1400, easing = FastOutSlowInEasing)
    val clat by animateFloatAsState(geo?.lat?.toFloat() ?: 35f, ease, label = "clat")
    val clon by animateFloatAsState(geo?.lon?.toFloat() ?: 15f, ease, label = "clon")
    val span by animateFloatAsState(if (geo != null) 50f else 130f, ease, label = "span")
    val litA by animateFloatAsState(if (lit) 1f else 0.6f, tween(500), label = "lit")
    val pulse by rememberInfiniteTransition(label = "pulse")
        .animateFloat(0f, 1f, infiniteRepeatable(tween(1900, easing = LinearEasing)), label = "p")

    Canvas(modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        drawRect(Brush.verticalGradient(listOf(Palette.brandDeep, Palette.bg)))

        val ppd = w / span                                  // пикселей на градус
        val halfLon = w / 2f / ppd
        val halfLat = h / 2f / ppd
        val step = WorldData.STEP.toFloat()
        val c0 = floor((clon - halfLon - step - WorldData.LEFT.toFloat()) / step).toInt().coerceIn(0, WorldData.COLS - 1)
        val c1 = ceil((clon + halfLon + step - WorldData.LEFT.toFloat()) / step).toInt().coerceIn(0, WorldData.COLS - 1)
        val r0 = floor((WorldData.TOP.toFloat() - (clat + halfLat + step)) / step).toInt().coerceIn(0, WorldData.ROWS - 1)
        val r1 = ceil((WorldData.TOP.toFloat() - (clat - halfLat - step)) / step).toInt().coerceIn(0, WorldData.ROWS - 1)

        val dot = maxOf(0.9.dp.toPx(), step * ppd * 0.30f)
        val dotColor = Palette.brandLight.copy(alpha = 0.50f)
        for (r in r0..r1) {
            val row = land[r]
            val lat = WorldData.TOP.toFloat() - (r + 0.5f) * step
            val y = h / 2f - (lat - clat) * ppd
            for (c in c0..c1) {
                if (!row[c]) continue
                val lon = WorldData.LEFT.toFloat() + (c + 0.5f) * step
                drawCircle(dotColor, dot, Offset(w / 2f + (lon - clon) * ppd, y))
            }
        }
        drawRect(Palette.brandDeep.copy(alpha = 0.16f))

        if (geo != null) {
            val p = Offset(w / 2f + (geo.lon.toFloat() - clon) * ppd, h / 2f - (geo.lat.toFloat() - clat) * ppd)
            val base = 8.dp.toPx()
            val acc = Palette.accent
            // пульсирующее кольцо (как PinMarker на iOS)
            val ringScale = 0.7f + (2.8f - 0.7f) * pulse
            val ringAlpha = 0.9f * (1f - pulse) * litA
            drawCircle(acc.copy(alpha = ringAlpha), base * ringScale, p, style = Stroke(width = 1.5.dp.toPx()))
            drawCircle(acc.copy(alpha = 0.35f * litA), base, p)
            drawCircle(Color.White.copy(alpha = litA), 3.5.dp.toPx(), p)
        }
    }
}
