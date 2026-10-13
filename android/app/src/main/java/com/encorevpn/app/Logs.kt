package com.encorevpn.app

import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
private fun fmt(ms: Long): String = timeFmt.format(Date(ms))

private fun colorFor(e: LogEntry): Color {
    val t = e.text.lowercase()
    if (t.contains("ошибк") || t.contains("не удалось") || t.contains("не принят")) return Palette.alert
    if (t.contains("капч")) return Palette.warn
    return Color.White.copy(alpha = 0.92f)
}

/** Вкладка «Логи». */
@Composable
fun LogsScreen() {
    val logs = Model.logs
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(1500); copied = false } }

    val state = rememberLazyListState()
    val lastId = logs.lastOrNull()?.id
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(lastId) {
        if (logs.isEmpty()) return@LaunchedEffect
        if (first) { state.scrollToItem(logs.lastIndex); first = false }
        else state.animateScrollToItem(logs.lastIndex)
    }

    Column(Modifier.fillMaxSize().padding(start = 18.dp, end = 18.dp, bottom = 12.dp)) {
        Box(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 14.dp).heightIn(min = 44.dp), contentAlignment = Alignment.Center) {
            T("Логи", color = Color.White, size = 24.sp, weight = FontWeight.Bold, spacing = (-0.4).sp, shadow = LegibleShadow)
            Row(Modifier.align(Alignment.CenterEnd), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderButton(if (copied) Ico.Check else Ico.Copy, "Скопировать логи") {
                    if (logs.isNotEmpty()) {
                        clipboard.setText(AnnotatedString(logs.joinToString("\n") {
                            "${fmt(it.time)} [${if (it.tunnel) "туннель" else "приложение"}] ${it.text}"
                        }))
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        copied = true
                    }
                }
                HeaderButton(Ico.Trash, "Очистить логи") { Model.clearLogs() }
            }
        }

        val shape = RoundedCornerShape(22.dp)
        Box(
            Modifier.weight(1f).fillMaxWidth().clip(shape).background(Palette.glass).border(1.dp, Palette.hairline, shape),
        ) {
            if (logs.isEmpty()) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Glyph(Ico.Doc, Palette.brandLight.copy(alpha = 0.8f), 38.dp, stroke = 1.2f)
                    T("Пока пусто", color = Color.White, size = 17.sp, weight = FontWeight.SemiBold)
                    T("События подключения появятся здесь", color = Palette.muted, size = 14.sp)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), state = state, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(logs, key = { it.id }) { LogRow(it) }
                }
            }
        }
    }
}

@Composable
private fun HeaderButton(ico: Ico, label: String, onClick: () -> Unit) {
    Box(
        Modifier.semantics { contentDescription = label }
            .pressClickable(onClick = onClick)
            .size(44.dp).clip(CircleShape).background(Palette.brand.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) { Glyph(ico, Palette.brandLight, 20.dp, stroke = 1.6f) }
}

@Composable
private fun LogRow(e: LogEntry) {
    Row(verticalAlignment = Alignment.Top) {
        T(fmt(e.time), Modifier.padding(top = 1.dp), color = Palette.muted, size = 12.sp, mono = true)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.padding(top = 1.dp).clip(CircleShape).background(Palette.chip).padding(horizontal = 5.dp, vertical = 2.dp)) {
            T(if (e.tunnel) "TUN" else "APP", color = if (e.tunnel) Palette.brand2 else Palette.brandLight,
                size = 10.sp, weight = FontWeight.Bold, mono = true)
        }
        Spacer(Modifier.width(8.dp))
        SelectionContainer(Modifier.weight(1f)) {
            T(e.text, Modifier.fillMaxWidth(), color = colorFor(e), size = 13.sp, mono = true)
        }
    }
}
