package com.hosein.snappnet.service

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import com.hosein.snappnet.core.TreeNode

/** Adapts AccessibilityNodeInfo to the testable TreeNode interface. The Rect is shared (single-threaded use). */
class AndroidTreeNode(
    private val node: AccessibilityNodeInfo,
    private val screenW: Int,
    private val screenH: Int,
    private val rect: Rect = Rect(),
) : TreeNode {
    override val text: CharSequence? get() = node.text
    override val description: CharSequence? get() = node.contentDescription
    override val isVisible: Boolean get() = node.isVisibleToUser
    override val childCount: Int get() = node.childCount

    override val visibleArea: Long
        get() {
            node.getBoundsInScreen(rect)
            val w = minOf(rect.right, screenW) - maxOf(rect.left, 0)
            val h = minOf(rect.bottom, screenH) - maxOf(rect.top, 0)
            return if (w > 0 && h > 0) w.toLong() * h else 0L
        }

    override fun childAt(index: Int): TreeNode? = node.getChild(index)?.let { AndroidTreeNode(it, screenW, screenH, rect) }

    @Suppress("DEPRECATION")
    override fun release() {
        if (Build.VERSION.SDK_INT < 33) node.recycle()
    }
}
