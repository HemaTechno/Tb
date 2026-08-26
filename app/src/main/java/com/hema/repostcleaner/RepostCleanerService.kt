package com.hema.repostcleaner

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

class RepostCleanerService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Toast.makeText(this, "تم تنشيط أداة Hema Repost Cleaner بنجاح!", Toast.LENGTH_LONG).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val rootNode = rootInActiveWindow ?: return
        
        // البحث عن زرار المشاركة أو الخيارات في تيك توك (سهم المشاركة أو الثلاث نقاط)
        findAndClickByText(rootNode, "إعادة نشر")
        findAndClickByText(rootNode, "Remove repost") // لو اللغات أجنبية
        findAndClickByText(rootNode, "إزالة إعادة النشر")
    }

    private fun findAndClickByText(node: AccessibilityNodeInfo, text: String) {
        if (node.text != null && node.text.toString().contains(text, ignoreCase = true)) {
            var clickableNode: AccessibilityNodeInfo? = node
            while (clickableNode != null) {
                if (clickableNode.isClickable) {
                    clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Toast.makeText(this, "تم مسح ريبوست بنجاح بواسطة هيما", Toast.LENGTH_SHORT).show()
                    break
                }
                clickableNode = clickableNode.parent
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            findAndClickByText(child, text)
            child.recycle()
        }
    }

    override fun onInterrupt() {
        Toast.makeText(this, "توقفت خدمة الحذف", Toast.LENGTH_SHORT).show()
    }
}
