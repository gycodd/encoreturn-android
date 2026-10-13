package com.encorevpn.app

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Режим экрана: то же, что `Mode` в iOS-версии. */
enum class Mode { Off, Connecting, Captcha, On }

fun hexColor(hex: Int, alpha: Float = 1f): Color =
    Color((hex shr 16) and 0xFF, (hex shr 8) and 0xFF, hex and 0xFF).copy(alpha = alpha)

/** Палитра: глубокий индиго и фиолетовый акцент (1:1 с iOS). */
object Palette {
    val brand = hexColor(0x7C5CFC)
    val brandDark = hexColor(0x6040E0)
    val brandLight = hexColor(0xC4ABFF)
    val brand2 = hexColor(0xA07CFF)
    val brandDeep = hexColor(0x140F28)
    val muted = hexColor(0x9B90CC)
    val chip = hexColor(0x7C5CFC, 0.18f)
    val card = hexColor(0x15121F)
    val glass = hexColor(0x0F0C20, 0.82f)
    val field = Color.Black.copy(alpha = 0.28f)
    val alert = hexColor(0xFF8A8A)
    val idle = hexColor(0x7C5CFC)
    val hairline = Color.White.copy(alpha = 0.09f)
    val bg = hexColor(0x0D0B1A)
    val accent = brand
    val success = hexColor(0x4ADE80)
    val warn = hexColor(0xFACC15)
    val captchaBottom = hexColor(0xE05F6F)
}

/** Текст без Material: BasicText + нужные параметры. */
@Composable
fun T(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: TextUnit = 16.sp,
    weight: FontWeight = FontWeight.Normal,
    mono: Boolean = false,
    align: TextAlign = TextAlign.Start,
    spacing: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    shadow: Shadow? = null,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = size,
            fontWeight = weight,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            textAlign = align,
            letterSpacing = spacing,
            shadow = shadow,
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Лёгкая тень под текстом, чтобы читался поверх анимированного фона (`legible()` в iOS). */
val LegibleShadow = Shadow(color = Color.Black.copy(alpha = 0.65f), offset = androidx.compose.ui.geometry.Offset(0f, 2f), blurRadius = 10f)

/** Мягкое «вдавливание» при нажатии (`PressStyle` в iOS). */
fun Modifier.pressClickable(scale: Float = 0.94f, enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(
        targetValue = if (pressed) scale else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "press",
    )
    this
        .graphicsLayer { scaleX = sc; scaleY = sc }
        .clickable(interactionSource = src, indication = null, enabled = enabled, onClick = onClick)
}
