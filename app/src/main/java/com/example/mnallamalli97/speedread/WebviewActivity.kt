package com.example.mnallamalli97.speedread

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.example.mnallamalli97.speedread.R.layout

class WebviewActivity : AppCompatActivity() {
  private var webView: WebView? = null
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(layout.web_view)

    webView = findViewById<WebView>(R.id.webView)
    webView!!.webViewClient = WebViewClient()
    webView!!.loadUrl("https://fair.org/take-action-now/media-contact-list/")
  }

  override fun onBackPressed() {
    if (webView!!.canGoBack()) {
      webView!!.goBack()
    } else {
      super.onBackPressed()
    }
  }

}


