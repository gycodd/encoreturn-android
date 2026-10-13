package com.encorevpn.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/** Корень: две вкладки (главная и логи) с нижней панелью (`ContentView` на iOS). */
@Composable
fun EncoreApp(onPower: () -> Unit, onOpenCaptcha: () -> Unit) {
    val ctx = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var showImport by remember { mutableStateOf(false) }

    // страна сервера (из кэша или GeoIP)
    val host = Model.config?.peer?.substringBefore(':')
    var geo by remember { mutableStateOf<ServerGeo?>(null) }
    val retry = Model.mode == Mode.On && geo == null
    LaunchedEffect(host, retry) {
        if (host == null) { geo = null; return@LaunchedEffect }
        val cached = GeoCache.load(ctx, host)
        if (cached != null) { geo = cached; return@LaunchedEffect }
        val g = withContext(Dispatchers.IO) { GeoService.lookup(host) }
        if (g != null) { GeoCache.save(ctx, g, host); geo = g }
    }

    // пинг, пока подключено
    val on = Model.mode == Mode.On
    var ping by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(on) {
        if (!on) { ping = null; return@LaunchedEffect }
        val probe = PingProbe()
        delay(1500)
        while (true) {
            ping = withContext(Dispatchers.IO) { probe.sample() }
            delay(5000)
        }
    }

    Box(Modifier.fillMaxSize().background(Palette.bg)) {
        Backdrop(Model.mode)

        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (tab == 0) HomeScreen(geo, ping, onPower, onAddKey = { showImport = true }, onOpenCaptcha = onOpenCaptcha)
                else LogsScreen()
            }
            NavBar(tab) { tab = it }
        }

        ImportOverlay(showImport) { showImport = false }
        ToastHost()
    }
}

@Composable
private fun NavBar(tab: Int, onTab: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Palette.bg)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.hairline))
        Row(Modifier.fillMaxWidth().navigationBarsPadding().height(56.dp)) {
            NavItem(Ico.Shield, "Главная", tab == 0, Modifier.weight(1f)) { onTab(0) }
            NavItem(Ico.Doc, "Логи", tab == 1, Modifier.weight(1f)) { onTab(1) }
        }
    }
}

@Composable
private fun NavItem(ico: Ico, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c by animateColorAsState(if (selected) Palette.brandLight else Color(0xFF8E8E93), tween(200), label = "nav")
    Column(
        modifier.fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Glyph(ico, c, 24.dp, stroke = 1.6f)
        T(label, color = c, size = 11.sp, weight = FontWeight.Medium, modifier = Modifier.padding(top = 3.dp))
    }
}
