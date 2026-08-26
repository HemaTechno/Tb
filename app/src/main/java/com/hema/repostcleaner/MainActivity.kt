package com.hema.repostcleaner

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var btnBatchDelete: Button
    private lateinit var txtStatus: TextView
    private lateinit var controlBar: LinearLayout
    
    private var isDeleting = false
    private var deletedInBatch = 0
    private val batchLimit = 50 
    private val handler = Handler(Looper.getMainLooper())

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        btnBatchDelete = findViewById(R.id.btnBatchDelete)
        txtStatus = findViewById(R.id.txtStatus)
        controlBar = findViewById(R.id.controlBar)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                if (url.startsWith("snssdk") || url.startsWith("tiktok://")) {
                    return true 
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url != null && (url.contains("tiktok.com/@") || url.contains("profile"))) {
                    controlBar.visibility = View.VISIBLE
                }
            }
        }

        webView.loadUrl("https://www.tiktok.com/login")

        btnBatchDelete.setOnClickListener {
            if (!isDeleting) {
                isDeleting = true
                deletedInBatch = 0
                btnBatchDelete.text = "⏸️ إيقاف مؤقت"
                Toast.makeText(this, "جاري بدء حذف دفعة (50 فيديو)...", Toast.LENGTH_SHORT).show()
                startBatchDeletion()
            } else {
                stopDeletion("تم إيقاف الحذف بواسطة المستخدم.")
            }
        }
    }

    private val deleteRunnable = object : Runnable {
        override fun run() {
            if (!isDeleting) return

            if (deletedInBatch >= batchLimit) {
                stopDeletion("✅ تم بنجاح حذف دفعة الـ 50 فيديو! ارتاح شوية واضغط تاني لو حابب.")
                return
            }

            val jsCode = """
                (function() {
                    var buttons = document.querySelectorAll('button, div[role="button"]');
                    for (var i = 0; i < buttons.length; i++) {
                        var text = buttons[i].innerText || "";
                        if (text.includes("Remove repost") || text.includes("إزالة إعادة النشر") || text.includes("حذف")) {
                            buttons[i].click();
                            return "DELETED_ONE";
                        }
                    }
                    
                    var moreBtn = document.querySelector('[data-e2e="share-icon"], [data-e2e="more-icon"]');
                    if (moreBtn) {
                        moreBtn.click();
                        return "OPENED_MENU";
                    }
                    
                    return "SEARCHING";
                })();
            """.trimIndent()

            webView.evaluateJavascript(jsCode) { result ->
                if (result != null && result.contains("DELETED_ONE")) {
                    deletedInBatch++
                    txtStatus.text = "تم حذف: $deletedInBatch / $batchLimit في هذه الدفعة"
                }
            }

            handler.postDelayed(this, 3000)
        }
    }

    private fun startBatchDeletion() {
        handler.post(deleteRunnable)
    }

    private fun stopDeletion(message: String) {
        isDeleting = false
        handler.removeCallbacks(deleteRunnable)
        btnBatchDelete.text = "🗑️ ابدأ حذف دفعة (50 فيديو)"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
