package com.encorevpn.app

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** Где находится сервер (аналог ServerGeo на iOS). */
data class ServerGeo(val countryCode: String, val country: String, val city: String?, val lat: Double, val lon: Double) {
    /** Флаг-эмодзи из двухбуквенного кода страны. */
    val flag: String
        get() {
            val cc = countryCode.uppercase()
            if (cc.length != 2) return "🌐"
            return String(Character.toChars(127397 + cc[0].code)) + String(Character.toChars(127397 + cc[1].code))
        }
}

/**
 * GeoIP: основной источник ipwho.is, запасной ipapi.co (оба по HTTPS, без ключа).
 * Внимание: приложение исключено из туннеля (ExcludedApplications), поэтому запрос идёт напрямую.
 */
object GeoService {
    fun lookup(host: String): ServerGeo? = viaIpWho(host) ?: viaIpApi(host)

    private fun get(url: String): JSONObject? = try {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 6000; c.readTimeout = 6000
        c.setRequestProperty("User-Agent", "encoreTurn-android")
        try { JSONObject(c.inputStream.bufferedReader().readText()) } finally { c.disconnect() }
    } catch (_: Exception) { null }

    private fun localized(code: String, fallback: String?): String {
        val n = Locale("", code.uppercase()).getDisplayCountry(Locale("ru"))
        return if (n.isNullOrBlank() || n.equals(code, ignoreCase = true)) (fallback ?: code.uppercase()) else n
    }

    private fun viaIpWho(host: String): ServerGeo? {
        val j = get("https://ipwho.is/$host?lang=ru") ?: return null
        if (!j.optBoolean("success", true)) return null
        val cc = j.optString("country_code"); if (cc.length != 2 || !j.has("latitude") || !j.has("longitude")) return null
        return ServerGeo(cc.uppercase(), localized(cc, j.optString("country")), j.optString("city").ifEmpty { null },
            j.getDouble("latitude"), j.getDouble("longitude"))
    }

    private fun viaIpApi(host: String): ServerGeo? {
        val j = get("https://ipapi.co/$host/json/") ?: return null
        if (j.optBoolean("error", false)) return null
        val cc = j.optString("country_code"); if (cc.length != 2 || !j.has("latitude") || !j.has("longitude")) return null
        return ServerGeo(cc.uppercase(), localized(cc, j.optString("country_name")), j.optString("city").ifEmpty { null },
            j.getDouble("latitude"), j.getDouble("longitude"))
    }
}

/** Запоминаем страну сервера на 30 дней, чтобы показывать её сразу. */
object GeoCache {
    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences("geo", Context.MODE_PRIVATE)

    fun load(ctx: Context, host: String): ServerGeo? {
        val raw = prefs(ctx).getString("encore.geo.$host", null) ?: return null
        return try {
            val j = JSONObject(raw)
            if (System.currentTimeMillis() - j.getLong("date") > 30L * 86400_000) null
            else ServerGeo(j.getString("cc"), j.getString("country"), j.optString("city").ifEmpty { null }, j.getDouble("lat"), j.getDouble("lon"))
        } catch (_: Exception) { null }
    }

    fun save(ctx: Context, g: ServerGeo, host: String) {
        val j = JSONObject().put("cc", g.countryCode).put("country", g.country).put("city", g.city ?: "")
            .put("lat", g.lat).put("lon", g.lon).put("date", System.currentTimeMillis())
        prefs(ctx).edit().putString("encore.geo.$host", j.toString()).apply()
    }
}

/**
 * Пинг: HEAD на gstatic generate_204. Первый запрос «прогревает» соединение (TCP+TLS),
 * замеряется второй, поэтому установка соединения в цифру не попадает.
 * Приложение исключено из туннеля, так что это задержка до интернета напрямую, а не через VPN.
 */
class PingProbe {
    private var warmed = false

    private fun head(): Boolean = try {
        val c = URL("https://www.gstatic.com/generate_204").openConnection() as HttpURLConnection
        c.requestMethod = "HEAD"; c.connectTimeout = 3000; c.readTimeout = 3000; c.useCaches = false
        val ok = c.responseCode in 200..399
        ok
    } catch (_: Exception) { false }

    /** Миллисекунды или null, если ответа нет. */
    fun sample(): Int? {
        if (!warmed) { if (!head()) return null; warmed = true }
        val t0 = System.nanoTime()
        if (!head()) { warmed = false; return null }
        return maxOf(1L, (System.nanoTime() - t0) / 1_000_000L).toInt()
    }
}
