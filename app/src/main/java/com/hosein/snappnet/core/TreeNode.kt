package com.hosein.snappnet.core

/** Minimal view of an accessibility node, so the traversal can be unit-tested without Android. */
interface TreeNode {
    val text: CharSequence?
    val description: CharSequence?
    val isVisible: Boolean
    /** On-screen area of this node in px², clipped to the display. */
    val visibleArea: Long
    val childCount: Int
    fun childAt(index: Int): TreeNode?
    fun release()
}

/** Bounded depth-first text collection: node text first, content description as fallback. */
object NodeFlattener {
    const val MAX_NODES = 500
    const val MAX_DEPTH = 40

    /** The root is NOT released; children are released after being visited. */
    fun collect(root: TreeNode, out: MutableList<ScreenText>) {
        walk(root, 0, intArrayOf(0), out)
    }

    private fun walk(node: TreeNode, depth: Int, budget: IntArray, out: MutableList<ScreenText>) {
        if (budget[0] >= MAX_NODES || depth > MAX_DEPTH) return
        budget[0]++
        if (!node.isVisible) return
        val t = node.text
        if (!t.isNullOrBlank()) {
            out.add(ScreenText(t.toString(), node.visibleArea))
        } else {
            val d = node.description
            if (!d.isNullOrBlank()) out.add(ScreenText(d.toString(), node.visibleArea))
        }
        for (i in 0 until node.childCount) {
            val c = node.childAt(i) ?: continue
            try { walk(c, depth + 1, budget, out) } finally { c.release() }
        }
    }
}
