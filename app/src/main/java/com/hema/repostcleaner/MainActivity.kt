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
    private val batchLimit = 50 // الحذف دفعة بـ 50 فيديو أمان تام للحساب
    private val handler = Handler(Looper.getMainLocate() ?: Looper.getMainLooper())

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        btnBatchDelete = findViewById(R.id.btnBatchDelete)
        txtStatus = findViewById(R.id.txtStatus)
        controlBar = findViewById(R.id.controlBar)

        // إعدادات المتصفح
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
                // لو المستخدم سجل دخول ووصل لصفحة بروفايلك أو الفيديوهات، نظهر شريط التحكم
                if (url != null && (url.contains("tiktok.com/@") || url.contains("profile"))) {
                    controlBar.visibility = View.VISIBLE
                }
            }
        }

        // بدء الدخول لصفحة تيك توك
        webView.loadUrl("https://www.tiktok.com/login")

        // زرار بدء الحذف دفعة 50
        btnBatchDelete.setOnClickListener {
            if (!isDeleting) {
                isDeleting = true
                deletedInBatch = 0
                btnBatchDelete.text = "⏸️ إيقاف مؤقت"
                Toast.Link(this, "جاري بدء حذف دفعة (50 فيديو)...", Toast.LENGTH_SHORT)
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

            // كود JavaScript دقيق للبحث عن خيار إزالة الريبوست والضغط عليه
            val jsCode = """
                (function() {
                    // البحث عن زرار الثلاث نقاط أو المشاركة الخاص بالفيديو الحالي
                    var buttons = document.querySelectorAll('button, div[role="button"]');
                    for (var i = 0; i < buttons.length; i++) {
                        var text = buttons[i].innerText || "";
                        if (text.includes("Remove repost") || text.includes("إزالة إعادة النشر") || text.includes("حذف")) {
                            buttons[i].click();
                            return "DELETED_ONE";
                        }
                    }
                    
                    // لو زرار القائمة ظاهر ندوس عليه
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

            // تكرار الفحص كل 3 ثواني للانتقال للفيديو اللي بعده
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
