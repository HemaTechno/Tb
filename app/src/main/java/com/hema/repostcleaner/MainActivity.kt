package com.hema.repostcleaner

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var btnStartCleaning: Button
    private lateinit var txtCounter: TextView
    private var isCleaning = false
    private var deletedCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private val maxLimit = 200

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        btnStartCleaning = findViewById(R.id.btnStartCleaning)
        txtCounter = findViewById(R.id.txtCounter)

        // إعدادات المتصفح الداخلي
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadWithOverviewMode = true
        webView.settings.useWideViewPort = true
        
        // السماح بحفظ ملفات تعريف الارتباط (عشان تسجيل الدخول يفضل محفوظ)
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // لو المستخدم فتح صفحة الملف الشخصي أو الريبوستات، نقدر نساعده
            }
        }

        // فتح تيك توك (صفحة تسجيل الدخول أو الملف الشخصي)
        webView.loadUrl("https://www.tiktok.com/login")

        btnStartCleaning.setOnClickListener {
            if (!isCleaning) {
                isCleaning = true
                btnStartCleaning.text = "⏹️ إيقاف التنظيف"
                btnStartCleaning.setBackgroundColor(0xFF888888.toInt())
                Toast.makeText(this, "بدء عملية التنظيف بذكاء...", Toast.LENGTH_SHORT).show()
                startAutoCleaning()
            } else {
                stopCleaning("تم إيقاف التنظيف بواسطة المستخدم.")
            }
        }
    }

    private val cleaningRunnable = object : Runnable {
        override fun run() {
            if (!isCleaning) return

            if (deletedCount >= maxLimit) {
                stopCleaning("⚠️ تم الوصول للحد الأقصى اليومي (200 ريبوست) للحفاظ على الحساب.")
                return
            }

            // حقن كود JavaScript للبحث عن زرار الحذف أو إزالة إعادة النشر في صفحة تيك توك
            val jsCode = """
                (function() {
                    // البحث عن أزرار المشاركة أو الثلاث نقاط أو زر إزالة الريبوست
                    var buttons = document.querySelectorAll('button, div[role="button"]');
                    for (var i = 0; i < buttons.length; i++) {
                        var text = buttons[i].innerText || "";
                        if (text.includes("Remove repost") || text.includes("إزالة إعادة النشر") || text.includes("حذف")) {
                            buttons[i].click();
                            return "DELETED";
                        }
                    }
                    
                    // محاولة الضغط على قائمة الخيارات لو موجودة
                    var moreOptions = document.querySelector('[data-e2e="share-icon"], [data-e2e="more-icon"]');
                    if (moreOptions) {
                        moreOptions.click();
                        return "CLICKED_MORE";
                    }
                    
                    return "NOT_FOUND";
                })();
            """.trimIndent()

            webView.evaluateJavascript(jsCode) { result ->
                if (result != null && result.contains("DELETED")) {
                    deletedCount++
                    txtCounter.text = "الممسوح: $deletedCount / $maxLimit"
                }
            }

            // تكرار العملية كل 3 ثواني عشان الموقع يلحق يحمل الفيديو اللي بعده
            handler.postDelayed(this, 3000)
        }
    }

    private fun startAutoCleaning() {
        handler.post(cleaningRunnable)
    }

    private fun stopCleaning(message: String) {
        isCleaning = false
        handler.removeCallbacks(cleaningRunnable)
        btnStartCleaning.text = "🚀 ابدأ التنظيف التلقائي (الحد 200)"
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
