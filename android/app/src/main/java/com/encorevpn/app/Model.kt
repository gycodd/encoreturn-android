package com.encorevpn.app

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/** Строка лога: источник «приложение» или «ядро» (на iOS это APP / TUN). */
class LogEntry(val id: Long, val time: Long, val tunnel: Boolean, val text: String)

class Toast(val id: Long, val text: String, val kind: Kind) {
    enum class Kind { Success, Info, Error }
}

enum class Phase { Idle, Connecting, Disconnecting }

/**
 * Состояние для UI (аналог VPNManager на iOS). Саму «оболочку» (Vpn, Cfg, ядро) не меняет:
 * только читает Vpn.up / Vpn.logText и вызывает Vpn.start / Vpn.stop, как раньше делал MainActivity.
 */
object Model {
    var config by mutableStateOf<Cfg?>(null); private set
    var up by mutableStateOf(false); private set
    var phase by mutableStateOf(Phase.Idle); private set
    var captchaPending by mutableStateOf(false); private set
    var connectedAt by mutableStateOf<Long?>(null); private set
    var toast by mutableStateOf<Toast?>(null); private set
    val logs = mutableStateListOf<LogEntry>()

    val mode: Mode
        get() = when {
            phase == Phase.Disconnecting -> Mode.Connecting
            phase == Phase.Connecting -> if (captchaPending) Mode.Captcha else Mode.Connecting
            up -> if (captchaPending) Mode.Captcha else Mode.On
            else -> Mode.Off
        }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var started = false
    private var seq = 0L
    private var prefs: android.content.SharedPreferences? = null
    private var deadLogged = false
    private var prevLines: List<String> = emptyList()

    fun init(ctx: Context) {
        if (started) return
        started = true
        val app = ctx.applicationContext
        prefs = app.getSharedPreferences("p", Context.MODE_PRIVATE)
        prefs?.getString("link", null)?.takeIf { it.isNotBlank() }?.let { config = Cfg.parse(it) }
        up = Vpn.up
        if (up) connectedAt = System.currentTimeMillis()
        log("Приложение запущено")
        scope.launch { poll() }
    }

    // MARK: Логи

    fun log(text: String, tunnel: Boolean = false, time: Long = System.currentTimeMillis()) {
        logs.add(LogEntry(seq++, time, tunnel, text))
        if (logs.size > 500) repeat(logs.size - 500) { logs.removeAt(0) }
    }

    fun clearLogs() = logs.clear()

    /** Забираем из Vpn.logText только новые строки (там скользящее окно на 3000 символов). */
    private fun pullCoreLog() {
        val text = Vpn.logText
        var lines = text.split("\n").filter { it.isNotBlank() }
        if (text.length >= 3000 && lines.isNotEmpty()) lines = lines.drop(1)   // первая строка могла обрезаться
        if (lines.isEmpty()) { prevLines = lines; return }
        var k = minOf(prevLines.size, lines.size)
        while (k > 0 && prevLines.takeLast(k) != lines.take(k)) k--
        for (l in lines.drop(k)) log(l, tunnel = true)
        prevLines = lines
    }

    // MARK: Тосты

    private var toastSeq = 0L
    fun showToast(text: String, kind: Toast.Kind = Toast.Kind.Success) { toast = Toast(++toastSeq, text, kind) }
    fun dismissToast() { toast = null }

    // MARK: Опрос состояния

    private suspend fun poll() {
        var tick = 0
        while (scope.isActive) {
            delay(500)
            tick++
            val nowUp = Vpn.up
            if (nowUp != up) onUpChanged(nowUp)

            if (nowUp && !Vpn.alive()) {
                if (!deadLogged) {
                    deadLogged = true
                    log("Ядро остановилось, см. лог")
                    showToast("Ядро остановилось", Toast.Kind.Error)
                }
            } else deadLogged = false

            if (tick % 2 == 0) pullCoreLog()
            if (tick % 4 == 0) checkCaptcha()
        }
    }

    private fun onUpChanged(nowUp: Boolean) {
        up = nowUp
        if (nowUp) {
            connectedAt = System.currentTimeMillis()
            log("Подключено")
            showToast("Обход подключён", Toast.Kind.Success)
        } else {
            connectedAt = null
            captchaPending = false
            log("Отключено")
            showToast("Обход отключён", Toast.Kind.Info)
        }
    }

    private suspend fun checkCaptcha() {
        if (!(up || phase == Phase.Connecting)) { if (captchaPending) captchaPending = false; return }
        val open = withContext(Dispatchers.IO) {
            try { Socket().use { it.connect(InetSocketAddress("127.0.0.1", 8765), 500); true } } catch (_: Exception) { false }
        }
        if (open != captchaPending) {
            captchaPending = open
            log(if (open) "ВКонтакте просит капчу" else "Капча пройдена")
            if (open) showToast("Нужна капча", Toast.Kind.Info)
        }
    }

    // MARK: Ключ

    /** null = ключ принят, иначе текст ошибки. */
    fun addKey(raw: String): String? {
        val cfg = Cfg.parse(raw)
        if (cfg == null) { log("Ключ не принят: неверный формат"); return "Это не ключ encoreTurn" }
        if (cfg.expired) { log("Ключ не принят: срок истёк"); return "Срок ключа истёк" }
        prefs?.edit()?.putString("link", raw)?.apply()
        config = cfg
        log("Ключ добавлен: ${cfg.name}")
        showToast("Ключ добавлен", Toast.Kind.Success)
        return null
    }

    // MARK: Действия кнопки питания

    /** true, если нужно сначала спросить у системы разрешение VPN (решает MainActivity). */
    fun canConnect(): Boolean {
        val cfg = config
        if (cfg == null) {
            log("Подключение невозможно: нет ключа")
            showToast("Нет ключа. Добавьте его кнопкой вверху", Toast.Kind.Error)
            return false
        }
        if (cfg.expired) {
            log("Подключение невозможно: срок ключа истёк")
            showToast("Срок ключа истёк", Toast.Kind.Error)
            return false
        }
        return true
    }

    fun permissionDenied() {
        log("Нужно разрешение VPN")
        showToast("Нужно разрешение VPN", Toast.Kind.Error)
    }

    fun connect(ctx: Context) {
        val cfg = config ?: return
        if (phase != Phase.Idle) return
        phase = Phase.Connecting
        log("Запрос подключения")
        val app = ctx.applicationContext
        scope.launch {
            val err = withContext(Dispatchers.IO) {
                try { Vpn.start(app, cfg); null } catch (e: Throwable) { e.message ?: e.toString() }
            }
            phase = Phase.Idle
            if (Vpn.up != up) onUpChanged(Vpn.up)
            if (err != null) {
                log("Ошибка: $err")
                showToast("Не удалось подключиться", Toast.Kind.Error)
            }
        }
    }

    fun disconnect() {
        if (phase != Phase.Idle) return
        phase = Phase.Disconnecting
        log("Запрос отключения")
        scope.launch {
            withContext(Dispatchers.IO) { Vpn.stop() }
            phase = Phase.Idle
            if (Vpn.up != up) onUpChanged(Vpn.up)
        }
    }
}
