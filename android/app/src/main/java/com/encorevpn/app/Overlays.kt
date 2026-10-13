package com.encorevpn.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// MARK: Ввод ключа (шторка снизу, `ImportSheet` на iOS)

@Composable
fun BoxScope.ImportOverlay(visible: Boolean, onDismiss: () -> Unit) {
    BackHandler(enabled = visible, onBack = onDismiss)

    AnimatedVisibility(visible = visible, enter = fadeIn(tween(250)), exit = fadeOut(tween(250))) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        )
    }
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically(tween(320)) { it },
        exit = slideOutVertically(tween(250)) { it },
    ) {
        ImportSheet(onDismiss)
    }
}

@Composable
private fun ImportSheet(onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var focused by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val canAdd = text.trim().isNotEmpty()

    fun add() {
        if (!canAdd) return
        val err = Model.addKey(text)
        if (err == null) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onDismiss()
        } else {
            error = err
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    Column(
        Modifier.fillMaxWidth().clip(sheetShape).background(Palette.bg).border(1.dp, Palette.hairline, sheetShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}   // не пропускать тап на фон
            .imePadding().navigationBarsPadding()
            .padding(horizontal = 22.dp).padding(top = 10.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).width(36.dp).height(5.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 12.dp)) {
            T("Ключ", color = Color.White, size = 28.sp, weight = FontWeight.Bold)
            T("Вставьте ключ, который вам выдал бот", color = Palette.muted, size = 15.sp)
        }

        // поле: вставка и очистка в одной кнопке справа
        val fieldShape = RoundedCornerShape(16.dp)
        val borderC by animateColorAsState(
            when {
                error != null -> Palette.alert.copy(alpha = 0.75f)
                focused -> Palette.brand.copy(alpha = 0.85f)
                else -> Palette.hairline
            }, tween(250), label = "fieldBorder",
        )
        Row(
            Modifier.fillMaxWidth().height(56.dp).clip(fieldShape).background(Palette.field).border(1.dp, borderC, fieldShape)
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it; if (error != null) error = null },
                modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
                cursorBrush = SolidColor(Palette.brandLight),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None, autoCorrect = false,
                    keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { add() }),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (text.isEmpty()) T("encorevpn://c/…", color = Palette.muted.copy(alpha = 0.7f), size = 17.sp, maxLines = 1)
                        inner()
                    }
                },
            )
            Box(
                Modifier.semantics { contentDescription = if (text.isEmpty()) "Вставить из буфера" else "Очистить" }
                    .pressClickable {
                        if (text.isEmpty()) text = clipboard.getText()?.text ?: "" else text = ""
                    }
                    .size(38.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (text.isEmpty()) Glyph(Ico.Clipboard, Palette.brandLight, 22.dp, stroke = 1.6f)
                else Glyph(Ico.CloseCircle, Palette.brandLight, 22.dp, cut = Palette.bg)
            }
        }

        if (error != null) T(error ?: "", color = Palette.alert, size = 14.sp)

        val btnShape = RoundedCornerShape(12.dp)
        Box(
            Modifier.fillMaxWidth().height(54.dp)
                .pressClickable(0.97f, enabled = canAdd) { add() }
                .clip(btnShape)
                .background(if (canAdd) Brush.linearGradient(listOf(Palette.brand, Palette.brandDark))
                            else SolidColor(Color.White.copy(alpha = 0.08f))),
            contentAlignment = Alignment.Center,
        ) {
            T("Добавить", color = if (canAdd) Color.White else Palette.muted, size = 17.sp, weight = FontWeight.SemiBold)
        }
    }
}

// MARK: Тосты (сверху, как в Eclipse)

@Composable
fun BoxScope.ToastHost() {
    val t = Model.toast
    val last = remember { arrayOfNulls<Toast>(1) }
    if (t != null) last[0] = t
    LaunchedEffect(t?.id) {
        if (t != null) {
            delay(if (t.kind == Toast.Kind.Error) 4500L else 2600L)
            if (Model.toast?.id == t.id) Model.dismissToast()
        }
    }
    AnimatedVisibility(
        visible = t != null,
        modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(horizontal = 16.dp).padding(top = 6.dp),
        enter = slideInVertically(spring(dampingRatio = 0.85f)) { -it } + fadeIn(),
        exit = slideOutVertically(tween(250)) { -it } + fadeOut(),
    ) {
        last[0]?.let { ToastCard(it) }
    }
}

@Composable
private fun ToastCard(t: Toast) {
    val (ico, tint) = when (t.kind) {
        Toast.Kind.Success -> Ico.CheckCircle to Palette.success
        Toast.Kind.Info -> Ico.Info to Palette.brandLight
        Toast.Kind.Error -> Ico.Warning to Palette.alert
    }
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.shadow(12.dp, shape).clip(shape).background(Palette.card).border(1.dp, tint.copy(alpha = 0.45f), shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { Model.dismissToast() }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Glyph(ico, tint, 18.dp)
        Spacer(Modifier.width(10.dp))
        T(t.text, color = Color.White, size = 14.sp, weight = FontWeight.Medium)
    }
}
