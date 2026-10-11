package com.encorevpn.app

import android.content.Context
import android.util.Base64
import android.util.Log
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import org.json.JSONObject

/** Ссылка encorevpn://c/<base64url(json)> - тот же формат, что и на iOS. */
class Cfg(j: JSONObject) {
    private val wg = j.getJSONObject("wg")
    val name = j.optString("name", "encoreTurn").ifEmpty { "encoreTurn" }
    val peer = j.getString("peer")
    val vkLink = j.getString("vk_link")
    val threads = j.optInt("threads", 8).coerceIn(1, 16)
    val udp = j.optJSONArray("flags")?.toString()?.contains("-udp") ?: true
    val expires = j.optLong("expires", 0)
    val expired get() = expires > 0 && expires < System.currentTimeMillis() / 1000

    fun wgQuick(pkg: String): String {
        var s = "[Interface]\nPrivateKey = ${wg.getString("private_key")}\nAddress = ${wg.getString("address")}\n"
        s += "DNS = ${wg.optString("dns", "1.1.1.1").ifEmpty { "1.1.1.1" }}\nMTU = ${wg.optInt("mtu", 1280)}\n"
        s += "ExcludedApplications = $pkg\n\n[Peer]\nPublicKey = ${wg.getString("server_pubkey")}\n"
        val psk = wg.optString("psk", "")
        if (psk.isNotEmpty()) s += "PresharedKey = $psk\n"
        return s + "AllowedIPs = 0.0.0.0/0, ::/0\nEndpoint = 127.0.0.1:9000\nPersistentKeepalive = 25\n"
    }

    companion object {
        fun parse(raw: String): Cfg? = try {
            val i = raw.indexOf("encorevpn://c/", ignoreCase = true)
            var b = raw.substring(i + 14).trim().takeWhile { it !in "?# \n\r" }
            b += "=".repeat((4 - b.length % 4) % 4)
            Cfg(JSONObject(String(Base64.decode(b, Base64.URL_SAFE))))
        } catch (e: Exception) { null }
    }
}

object Vpn {
    private var backend: GoBackend? = null
    private var proc: Process? = null
    @Volatile var up = false
    @Volatile var logText = ""
    fun alive() = proc?.isAlive == true
    private val tunnel = object : Tunnel {
        override fun getName() = "encore"
        override fun onStateChange(newState: Tunnel.State) { up = newState == Tunnel.State.UP }
    }

    /** Вызывать из фонового потока. Бросает исключение при ошибке. */
    fun start(ctx: Context, c: Cfg) {
        stop(); logText = ""
        val b = backend ?: GoBackend(ctx.applicationContext).also { backend = it }
        val exe = ctx.applicationInfo.nativeLibraryDir + "/libencorecore.so"
        val args = mutableListOf(exe, "-listen", "127.0.0.1:9000", "-peer", c.peer, "-vk-link", c.vkLink, "-n", c.threads.toString())
        if (c.udp) args += "-udp"
        val p = ProcessBuilder(args).directory(ctx.filesDir).redirectErrorStream(true).start()
        proc = p
        Thread { try { p.inputStream.bufferedReader().forEachLine { Log.d("EncoreCore", it); logText = (logText + it + "\n").takeLast(3000) } } catch (_: Exception) {} }.start()
        Thread.sleep(800)
        check(p.isAlive) { "ядро не запустилось (см. logcat: EncoreCore)" }
        try {
            b.setState(tunnel, Tunnel.State.UP, Config.parse(c.wgQuick(ctx.packageName).byteInputStream()))
        } catch (e: Exception) { stop(); throw e }
    }

    fun stop() {
        try { backend?.setState(tunnel, Tunnel.State.DOWN, null) } catch (_: Exception) {}
        proc?.destroy(); proc = null; up = false
    }
}
