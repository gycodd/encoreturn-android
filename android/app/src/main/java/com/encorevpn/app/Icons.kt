package com.encorevpn.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp

/** Значки, нарисованные вручную на сетке 24x24 в духе SF Symbols (без внешних зависимостей). */
enum class Ico { Shield, Doc, LinkPlus, Copy, Check, Trash, Power, Calendar, Waves, Clipboard, CloseCircle, Info, Warning, CheckCircle }

@Composable
fun Glyph(ico: Ico, tint: Color, size: Dp, modifier: Modifier = Modifier, stroke: Float = 1.7f, cut: Color = Palette.card) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 24f
        withTransform({ scale(scaleX = s, scaleY = s, pivot = Offset.Zero) }) {
            drawIco(ico, tint, stroke, cut)
        }
    }
}

private fun DrawScope.drawIco(ico: Ico, c: Color, w: Float, cut: Color) {
    val st = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, col: Color = c, width: Float = w) =
        drawLine(col, Offset(x1, y1), Offset(x2, y2), strokeWidth = width, cap = StrokeCap.Round)
    fun rr(x: Float, y: Float, wd: Float, ht: Float, r: Float, style: androidx.compose.ui.graphics.drawscope.DrawStyle = st, col: Color = c) =
        drawRoundRect(col, Offset(x, y), Size(wd, ht), CornerRadius(r, r), style = style)

    when (ico) {
        Ico.Shield -> {
            val outline = Path().apply {
                moveTo(12f, 3f); lineTo(19f, 6f); lineTo(19f, 12f)
                cubicTo(19f, 16.5f, 16f, 19.5f, 12f, 21f)
                cubicTo(8f, 19.5f, 5f, 16.5f, 5f, 12f)
                lineTo(5f, 6f); close()
            }
            val half = Path().apply {
                moveTo(12f, 3f); lineTo(5f, 6f); lineTo(5f, 12f)
                cubicTo(5f, 16.5f, 8f, 19.5f, 12f, 21f); close()
            }
            drawPath(half, c, style = Fill)
            drawPath(outline, c, style = st)
        }
        Ico.Doc -> {
            rr(5f, 3f, 14f, 18f, 2.6f)
            line(8.6f, 9f, 15.4f, 9f); line(8.6f, 13f, 15.4f, 13f); line(8.6f, 17f, 12.6f, 17f)
        }
        Ico.LinkPlus -> {
            rotate(-45f, Offset(10.5f, 13.5f)) {
                rr(3.2f, 11f, 9f, 5f, 2.5f)
                rr(8.8f, 11f, 9f, 5f, 2.5f)
            }
            line(18f, 3.2f, 18f, 8.8f); line(15.2f, 6f, 20.8f, 6f)
        }
        Ico.Copy -> {
            val back = Path().apply {
                moveTo(9f, 3.5f); lineTo(6.5f, 3.5f); quadraticBezierTo(4f, 3.5f, 4f, 6f)
                lineTo(4f, 13.5f); quadraticBezierTo(4f, 16f, 6.5f, 16f); lineTo(9f, 16f)
            }
            drawPath(back, c, style = st)
            rr(9f, 8f, 11f, 13f, 2.4f)
        }
        Ico.Check -> {
            val p = Path().apply { moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7f) }
            drawPath(p, c, style = Stroke(width = w * 1.15f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        Ico.Trash -> {
            line(4f, 6.5f, 20f, 6.5f)
            val handle = Path().apply { moveTo(9.5f, 6.5f); lineTo(9.5f, 4.5f); lineTo(14.5f, 4.5f); lineTo(14.5f, 6.5f) }
            drawPath(handle, c, style = st)
            val body = Path().apply {
                moveTo(6.5f, 6.5f); lineTo(7.4f, 19f); quadraticBezierTo(7.5f, 20.5f, 9f, 20.5f)
                lineTo(15f, 20.5f); quadraticBezierTo(16.5f, 20.5f, 16.6f, 19f); lineTo(17.5f, 6.5f)
            }
            drawPath(body, c, style = st)
            line(10.2f, 10.5f, 10.2f, 16.5f); line(13.8f, 10.5f, 13.8f, 16.5f)
        }
        Ico.Power -> {
            drawArc(c, startAngle = -50f, sweepAngle = 280f, useCenter = false,
                topLeft = Offset(4.8f, 4.8f), size = Size(14.4f, 14.4f), style = st)
            line(12f, 3f, 12f, 11.6f)
        }
        Ico.Calendar -> {
            rr(4f, 5.5f, 16f, 15f, 3.2f)
            line(4f, 10.2f, 20f, 10.2f)
            line(8.5f, 3.4f, 8.5f, 7f); line(15.5f, 3.4f, 15.5f, 7f)
        }
        Ico.Waves -> {
            drawCircle(c, radius = 1.9f, center = Offset(12f, 12f))
            for (r in floatArrayOf(5f, 8.6f)) {
                drawArc(c, -42f, 84f, false, Offset(12f - r, 12f - r), Size(2 * r, 2 * r), style = st)
                drawArc(c, 138f, 84f, false, Offset(12f - r, 12f - r), Size(2 * r, 2 * r), style = st)
            }
        }
        Ico.Clipboard -> {
            rr(5f, 5f, 14f, 16f, 2.6f)
            rr(9f, 3f, 6f, 4f, 1.6f)
        }
        Ico.CloseCircle -> {
            drawCircle(c, radius = 9.5f, center = Offset(12f, 12f))
            line(8.8f, 8.8f, 15.2f, 15.2f, cut, 1.9f); line(15.2f, 8.8f, 8.8f, 15.2f, cut, 1.9f)
        }
        Ico.Info -> {
            drawCircle(c, radius = 9.5f, center = Offset(12f, 12f))
            drawCircle(cut, radius = 1.2f, center = Offset(12f, 7.9f))
            line(12f, 11f, 12f, 16.6f, cut, 2f)
        }
        Ico.Warning -> {
            val tri = Path().apply { moveTo(12f, 3.8f); lineTo(21.2f, 19.8f); lineTo(2.8f, 19.8f); close() }
            drawPath(tri, c, style = Fill)
            drawPath(tri, c, style = Stroke(width = 2f, join = StrokeJoin.Round))
            line(12f, 9.6f, 12f, 14.2f, cut, 2f)
            drawCircle(cut, radius = 1.15f, center = Offset(12f, 17f))
        }
        Ico.CheckCircle -> {
            drawCircle(c, radius = 9.5f, center = Offset(12f, 12f))
            val p = Path().apply { moveTo(7.6f, 12.3f); lineTo(10.9f, 15.4f); lineTo(16.6f, 9f) }
            drawPath(p, cut, style = Stroke(width = 2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
