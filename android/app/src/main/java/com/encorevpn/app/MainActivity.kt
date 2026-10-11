package com.encorevpn.app

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.*
import java.net.InetSocketAddress
import java.net.Socket

class MainActivity : Activity() {
    private val ui = Handler(Looper.getMainLooper())
    private lateinit var link: EditText
    private lateinit var status: TextView
    private lateinit var btn: Button
    private lateinit var captcha: Button
    private lateinit var logv: TextView
    private var busy = false

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val pad = (20 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad * 2, pad, pad) }
        link = EditText(this).apply { hint = "encorevpn://c/..."; setText(prefs().getString("link", "")); maxLines = 4 }
        status = TextView(this).apply { textSize = 18f; gravity = Gravity.CENTER; setPadding(0, pad, 0, pad) }
        btn = Button(this).apply { setOnClickListener { toggle() } }
        captcha = Button(this).apply {
            text = "Пройти капчу VK"; visibility = View.GONE
            setOnClickListener { startActivity(Intent(this@MainActivity, CaptchaActivity::class.java)) }
        }
        root.addView(link); root.addView(status); root.addView(btn); root.addView(captcha)
        logv = TextView(this).apply { textSize = 11f; setTextIsSelectable(true) }
        root.addView(ScrollView(this).apply { addView(logv) })
        setContentView(root)
        render(if (Vpn.up) "Подключено" else "Вставьте ссылку и нажмите «Подключиться»")
        pollCaptcha()
    }

    private fun prefs() = getSharedPreferences("p", MODE_PRIVATE)
    private fun render(msg: String) { status.text = msg; btn.text = if (Vpn.up) "Отключиться" else "Подключиться"; btn.isEnabled = !busy }

    private fun toggle() {
        if (Vpn.up) { busy = true; render("Отключаюсь…"); bg { Vpn.stop(); busy = false; ui.post { render("Отключено") } }; return }
        val cfg = Cfg.parse(link.text.toString())
        if (cfg == null) { render("Это не ссылка encoreTurn"); return }
        if (cfg.expired) { render("Срок ключа истёк"); return }
        prefs().edit().putString("link", link.text.toString()).apply()
        val i = VpnService.prepare(this)
        if (i != null) startActivityForResult(i, 1) else connect(cfg)
    }

    override fun onActivityResult(req: Int, res: Int, d: Intent?) {
        if (req == 1) { if (res == RESULT_OK) Cfg.parse(link.text.toString())?.let { connect(it) } else render("Нужно разрешение VPN") }
    }

    private fun connect(cfg: Cfg) {
        busy = true; render("Подключаюсь…")
        bg {
            val err = try { Vpn.start(applicationContext, cfg); null } catch (e: Throwable) { e.message ?: e.toString() }
            busy = false
            ui.post { render(if (err == null) "Подключено: ${cfg.name}" else "Ошибка: $err") }
        }
    }

    private fun pollCaptcha() {
        bg {
            while (!isFinishing) {
                Thread.sleep(2000)
                val open = Vpn.up && try { Socket().use { it.connect(InetSocketAddress("127.0.0.1", 8765), 500); true } } catch (_: Exception) { false }
                val dead = Vpn.up && !Vpn.alive()
                ui.post { captcha.visibility = if (open) View.VISIBLE else View.GONE; logv.text = Vpn.logText; if (dead) status.text = "Ядро остановилось, см. лог ниже" }
            }
        }
    }

    private fun bg(f: () -> Unit) = Thread(f).start()
}
