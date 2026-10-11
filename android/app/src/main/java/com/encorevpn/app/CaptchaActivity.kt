package com.encorevpn.app

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient

/** Ручная капча VK: страница, которую поднимает ядро на localhost:8765. */
class CaptchaActivity : Activity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val w = WebView(this)
        w.settings.javaScriptEnabled = true
        w.settings.domStorageEnabled = true
        w.webViewClient = WebViewClient()
        w.loadUrl("http://localhost:8765")
        setContentView(w)
    }
}
