package com.encorevpn.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.sin

/** Фон: свечение под режим, плавающие пятна, волны и вращающиеся стеклянные кубы. */
@Composable
fun Backdrop(mode: Mode) {
    val idleA by animateFloatAsState(if (mode == Mode.Off) 1f else 0f, tween(800), label = "idleA")
    val onA by animateFloatAsState(if (mode == Mode.On || mode == Mode.Connecting) 1f else 0f, tween(800), label = "onA")
    val capA by animateFloatAsState(if (mode == Mode.Captcha) 1f else 0f, tween(800), label = "capA")
    val inf = rememberInfiniteTransition(label = "float")
    val float by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "f")

    Box(Modifier.fillMaxSize().background(Palette.bg)) {
        Canvas(Modifier.fillMaxSize()) {
            // медленно плавающие пятна света
            val r1 = 210.dp.toPx() * (1f + 0.05f * float)
            val c1 = Offset(110.dp.toPx(), 70.dp.toPx() - 30.dp.toPx() * float)
            drawCircle(Brush.radialGradient(listOf(hexColor(0x7C5CFC, 0.15f), Color.Transparent), center = c1, radius = r1), r1, c1)
            val r2 = 180.dp.toPx() * (1.05f - 0.05f * float)
            val c2 = Offset(size.width - 60.dp.toPx(), size.height - 180.dp.toPx() - 30.dp.toPx() * (1f - float))
            drawCircle(Brush.radialGradient(listOf(hexColor(0xA07CFF, 0.10f), Color.Transparent), center = c2, radius = r2), r2, c2)

            // свечение за кнопкой: слои перетекают друг в друга
            val gc = Offset(size.width * 0.5f, size.height * 0.34f)
            val gr = 330.dp.toPx()
            fun glow(color: Color, strength: Float, a: Float) {
                if (a <= 0.001f) return
                drawCircle(Brush.radialGradient(listOf(color.copy(alpha = strength * a), Color.Transparent), center = gc, radius = gr), gr, gc)
            }
            glow(Palette.idle, 0.10f, idleA)
            glow(Palette.brand, 0.30f, onA)
            glow(Palette.alert, 0.22f, capA)
        }

        Scene(connected = mode == Mode.On)

        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color.Black.copy(alpha = 0.22f))
            val rr = 340.dp.toPx()
            drawRect(Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.40f), Color.Transparent), center = center, radius = rr))
        }
    }
}

private class WaveLine(val yFrac: Double, val yJitter: Double, val phase: Double, val speed: Double,
                       val amp: Double, val freq: Double, val opacity: Double)

private class CubeSpec(val x: Double, val y: Double, val s: Double, val spin: Double, val floatDur: Double)

private class SeededRng(var state: ULong) {
    fun next(): Double {
        state = state * 6364136223846793005uL + 1442695040888963407uL
        return ((state shr 33) and 0xFFFFFFuL).toDouble() / 16777216.0
    }
}

/** Скорость вращения кубов: при подключении разгон, потом спокойный ход. */
private object SceneMotion {
    const val IDLE = 0.25
    const val PEAK = 5.0
    const val CRUISE = 0.7
    const val RAMP_TIME = 1.4
    const val RAMP_TAU = 0.55
    const val SETTLE_TAU = 3.5
    const val IDLE_TAU = 1.5
    private var phase = 0.0
    private var speed = IDLE
    private var last: Double? = null
    private var connectedAt: Double? = null
    private var wasConnected = false

    fun update(now: Double, connected: Boolean): Double {
        if (connected && !wasConnected) connectedAt = now
        if (!connected) connectedAt = null
        wasConnected = connected
        val dt = (now - (last ?: now)).coerceIn(0.0, 0.25)
        last = now
        val target: Double
        val tau: Double
        val ca = connectedAt
        if (ca != null) {
            if (now - ca < RAMP_TIME) { target = PEAK; tau = RAMP_TAU } else { target = CRUISE; tau = SETTLE_TAU }
        } else { target = IDLE; tau = IDLE_TAU }
        speed += (target - speed) * (1 - exp(-dt / tau))
        phase += speed * dt
        return phase
    }
}

private val waveLines: List<WaveLine> = run {
    val r = SeededRng(20260404uL)
    (0 until 16).map { i ->
        WaveLine(
            yFrac = i / 16.0, yJitter = r.next() * 15, phase = r.next() * 2 * PI,
            speed = (0.0025 + r.next() * 0.004) * 60, amp = 16 + r.next() * 35,
            freq = 0.0035 + r.next() * 0.003, opacity = 0.04 + r.next() * 0.08,
        )
    }
}

private val cubeSpecs = listOf(
    CubeSpec(0.06, 0.12, 90.0, 34.0, 9.0),
    CubeSpec(0.84, 0.10, 130.0, 42.0, 11.0),
    CubeSpec(0.10, 0.62, 130.0, 48.0, 12.0),
    CubeSpec(0.88, 0.74, 90.0, 36.0, 10.0),
    CubeSpec(0.46, 0.90, 60.0, 26.0, 7.0),
)

@Composable
private fun Scene(connected: Boolean) {
    var t by remember { mutableDoubleStateOf(0.0) }
    var spin by remember { mutableDoubleStateOf(0.0) }
    val conn by rememberUpdatedState(connected)

    LaunchedEffect(Unit) {
        var lastFrame = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { n ->
                if (n - lastFrame >= 33_000_000L) {      // ~30 кадров/с, как в iOS
                    lastFrame = n
                    val now = n / 1_000_000_000.0
                    spin = SceneMotion.update(now, conn)
                    t = now
                }
            }
        }
    }

    Canvas(Modifier.fillMaxSize()) {
        val tt = t
        val sp = spin
        // рисуем в «точках» (dp), как в iOS
        scale(density, density, pivot = Offset.Zero) {
            drawLines(tt)
            drawCubes(tt, sp)
        }
    }
}

private fun DrawScope.drawLines(t: Double) {
    val w = size.width / density
    val h = size.height / density
    val stroke = Stroke(width = 1f)
    for (l in waveLines) {
        val path = Path()
        val baseY = h * l.yFrac + l.yJitter
        val ph = l.phase + l.speed * t
        path.moveTo(0f, (baseY + sin(ph) * l.amp).toFloat())
        var x = 0.0
        while (x <= w) {
            path.lineTo(x.toFloat(), (baseY + sin(x * l.freq + ph) * l.amp).toFloat())
            x += 4.0
        }
        drawPath(path, Color(140 / 255f, 100 / 255f, 1f, l.opacity.toFloat()), style = stroke)
    }
}

private fun rotate3(v: Triple<Double, Double, Double>, ax: Double, ay: Double, az: Double): Triple<Double, Double, Double> {
    var (x, y, z) = v
    val cz = cos(az); val sz = sin(az)
    run { val nx = x * cz - y * sz; val ny = x * sz + y * cz; x = nx; y = ny }
    val cy = cos(ay); val sy = sin(ay)
    run { val nx = x * cy + z * sy; val nz = -x * sy + z * cy; x = nx; z = nz }
    val cx = cos(ax); val sx = sin(ax)
    run { val ny = y * cx - z * sx; val nz = y * sx + z * cx; y = ny; z = nz }
    return Triple(x, y, z)
}

private val cubeSigns = listOf(
    Triple(-1.0, -1.0, -1.0), Triple(1.0, -1.0, -1.0), Triple(1.0, 1.0, -1.0), Triple(-1.0, 1.0, -1.0),
    Triple(-1.0, -1.0, 1.0), Triple(1.0, -1.0, 1.0), Triple(1.0, 1.0, 1.0), Triple(-1.0, 1.0, 1.0),
)
private val cubeFaces = listOf(
    intArrayOf(0, 1, 2, 3), intArrayOf(4, 5, 6, 7), intArrayOf(0, 1, 5, 4),
    intArrayOf(3, 2, 6, 7), intArrayOf(0, 3, 7, 4), intArrayOf(1, 2, 6, 5),
)

private fun DrawScope.drawCubes(t: Double, spin: Double) {
    val persp = 900.0
    val w = (size.width / density).toDouble()
    val hh = (size.height / density).toDouble()
    val ox = w / 2
    val oy = hh / 2
    val count = if (w < 700) 3 else cubeSpecs.size
    val fillA = hexColor(0xA07CFF, 0.20f)
    val fillB = hexColor(0x7C5CFC, 0.06f)
    val edge = hexColor(0xC4ABFF, 0.45f)
    val halo = hexColor(0x7C5CFC, 0.10f)

    for (i in 0 until count) {
        val c = cubeSpecs[i]
        val fp = (t + i * 1.3) / c.floatDur
        val ph = fp - floor(fp)
        val tri = if (ph < 0.5) ph * 2 else (1 - ph) * 2
        val eased = tri * tri * (3 - 2 * tri)
        val q = ((spin + i * 5) / c.spin) % 1.0
        val ax = 2 * PI * q
        val ay = 2 * PI * q
        val az = PI * q
        val half = c.s / 2
        val cx = c.x * w + half
        val cy = c.y * hh + half - 34 * eased

        val px = DoubleArray(8); val py = DoubleArray(8); val pz = DoubleArray(8)
        for ((k, sg) in cubeSigns.withIndex()) {
            val r = rotate3(Triple(sg.first * half, sg.second * half, sg.third * half), ax, ay, az)
            val kk = persp / (persp - r.third)
            px[k] = ox + (cx + r.first - ox) * kk
            py[k] = oy + (cy + r.second - oy) * kk
            pz[k] = r.third
        }
        val order = cubeFaces.sortedBy { f -> f.sumOf { pz[it] } }
        for (f in order) {
            val path = Path()
            path.moveTo(px[f[0]].toFloat(), py[f[0]].toFloat())
            for (j in 1 until f.size) path.lineTo(px[f[j]].toFloat(), py[f[j]].toFloat())
            path.close()
            val b = path.getBounds()
            drawPath(path, halo, style = Stroke(width = 5f))   // мягкое свечение граней
            drawPath(path, Brush.linearGradient(listOf(fillA, fillB), start = Offset(b.left, b.top), end = Offset(b.right, b.bottom)))
            drawPath(path, edge, style = Stroke(width = 1f))
        }
    }
}
