package com.hema.repostcleaner

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class RepostCleanerService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // هنا هنكتب كود البحث عن زرار "المشاركة" و"الحذف" في الخطوة الجاية
    }

    override fun onInterrupt() {
        // لو الخدمة وقفت لأي سبب
    }
}
