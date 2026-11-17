package com.hosein.snappnet.service

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.hosein.snappnet.core.FareExtractor
import com.hosein.snappnet.core.FareTracker
import com.hosein.snappnet.core.NodeFlattener
import com.hosein.snappnet.core.ScreenText
import com.hosein.snappnet.core.SnappPackages
import com.hosein.snappnet.data.AppSettings
import com.hosein.snappnet.data.AppState
import com.hosein.snappnet.data.SettingsRepository
import com.hosein.snappnet.overlay.OverlayController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Pipeline: event (main thread, cheap filter) -> scan on a dedicated high-priority thread
 * -> FareExtractor -> FareTracker (dedupe) -> OverlayController (main thread).
 *
 * No polling, no artificial delay on the "show" path. The only timers are the "did the offer really
 * disappear?" re-checks, which exist solely to avoid showing a stale fare.
 */
class SnappAccessibilityService : AccessibilityService() {

    @Volatile private var settings = AppSettings()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val main = Handler(Looper.getMainLooper())

    private lateinit var repo: SettingsRepository
    private lateinit var overlay: OverlayController
    private lateinit var thread: HandlerThread
    private lateinit var work: Handler

    // Worker-thread-only state
    private val tracker = FareTracker(missesToHide = 2)
    private val texts = ArrayList<ScreenText>(64)
    private var lastFullScanAt = 0L
    private val recheck = Runnable { scanActiveWindow() }

    private val pending = AtomicInteger(0)
    @Volatile private var dirty = false

    // Main-thread-only package cache (avoids string matching on every event)
    private var lastPkg: String? = null
    private var lastPkgIsSnapp = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        repo = SettingsRepository(this)
        overlay = OverlayController(this) { x, y -> scope.launch { repo.setOverlayPosition(x, y) } }
        thread = HandlerThread("snappnet-scan", Process.THREAD_PRIORITY_URGENT_DISPLAY).also { it.start() }
        work = Handler(thread.looper)
        scope.launch {
            repo.settings.collect { s ->
                settings = s
                overlay.applySettings(s)
                if (!s.detectionEnabled) clearState()
            }
        }
        AppState.serviceRunning.value = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !settings.detectionEnabled) return
        val s = settings
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return // our own overlay

        if (!isSnapp(pkg)) {
            if (s.debugMode) AppState.diag.update { it.copy(otherPackage = pkg) }
            // Snapp Box may show its offer as an overlay above other apps, so a change in the foreground
            // app does not mean the offer is gone. Re-scan Snapp windows and let the miss counter decide.
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && !SnappPackages.isTransient(pkg)) {
                enqueue(null, true)
            }
            return
        }

        if (s.debugMode) AppState.diag.update { it.copy(events = it.events + 1, snappPackage = pkg) }
        val type = event.eventType
        val full = type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        if (!full && type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return

        val node = if (full) null else (event.source ?: return)
        enqueue(node, full)
    }

    private fun isSnapp(pkg: String): Boolean {
        if (pkg == lastPkg) return lastPkgIsSnapp
        lastPkg = pkg
        lastPkgIsSnapp = SnappPackages.isSnappBox(pkg)
        return lastPkgIsSnapp
    }

    /** At most two scans in flight; if events pile up, remember to re-scan the whole window once. */
    private fun enqueue(node: AccessibilityNodeInfo?, full: Boolean) {
        if (pending.get() >= 2) {
            dirty = true
            node?.let { release(it) }
            return
        }
        pending.incrementAndGet()
        work.post {
            try {
                process(node, full)
            } finally {
                pending.decrementAndGet()
            }
            if (dirty) { dirty = false; scanActiveWindow() }
        }
    }

    // ---- worker thread ----

    private fun process(start: AccessibilityNodeInfo?, full: Boolean) {
        texts.clear()
        if (full || start == null) {
            start?.let { release(it) }
            collectSnappWindows()
        } else {
            val node = climb(start)
            try {
                NodeFlattener.collect(wrap(node), texts)
            } finally {
                release(node)
            }
        }

        val fare = FareExtractor.extract(texts)
        if (settings.debugMode) {
            val snap = texts.take(80).map { "${it.text}   [${it.area}]" }
            AppState.diag.update { it.copy(scans = it.scans + 1, texts = snap, extractedFare = fare?.rial) }
        }
        if (fare != null) {
            work.removeCallbacks(recheck)
            if (tracker.onFare(fare.rial)) publish(fare.rial)
            return
        }

        if (full) {
            lastFullScanAt = SystemClock.uptimeMillis()
            if (tracker.onMiss()) {
                work.removeCallbacks(recheck)
                main.post { overlay.hide() }
            } else if (tracker.hasFare) {
                work.postDelayed(recheck, RECHECK_DELAY_MS) // confirm disappearance even if no more events arrive
            }
        } else {
            // Partial subtree had no fare. Fall back to a full scan, rate-limited so event bursts stay cheap.
            val now = SystemClock.uptimeMillis()
            if (now - lastFullScanAt >= FULL_SCAN_MIN_INTERVAL_MS) {
                lastFullScanAt = now
                scanActiveWindow()
            }
        }
    }

    private fun wrap(n: AccessibilityNodeInfo): AndroidTreeNode {
        val dm = resources.displayMetrics
        return AndroidTreeNode(n, dm.widthPixels, dm.heightPixels)
    }

    private fun scanActiveWindow() = process(null, full = true)

    /**
     * Reads every window that belongs to Snapp Box (its main window AND any overlay popup it draws over
     * other apps), not just the focused one. Needs flagRetrieveInteractiveWindows.
     */
    private fun collectSnappWindows() {
        var found = false
        val debug = settings.debugMode
        val seen = if (debug) StringBuilder() else null
        for (w in windows) {
            val root = w.root
            if (root != null) {
                val p = root.packageName?.toString()
                seen?.append(p)?.append(' ')
                if (p != null && SnappPackages.isSnappBox(p)) {
                    NodeFlattener.collect(wrap(root), texts)
                    found = true
                }
                release(root)
            }
            recycleWindow(w)
        }
        if (!found) { // fallback for devices that expose no window list
            val root = rootInActiveWindow
            if (root != null) {
                val p = root.packageName?.toString()
                if (p != null && SnappPackages.isSnappBox(p)) NodeFlattener.collect(wrap(root), texts)
                release(root)
            }
        }
        if (seen != null) AppState.diag.update { it.copy(windows = seen.toString().trim()) }
    }

    @Suppress("DEPRECATION")
    private fun recycleWindow(w: android.view.accessibility.AccessibilityWindowInfo) {
        if (Build.VERSION.SDK_INT < 33) w.recycle()
    }

    /** Give a changed leaf a little context (label/currency are usually siblings) without scanning everything. */
    private fun climb(start: AccessibilityNodeInfo): AccessibilityNodeInfo {
        var cur = start
        repeat(CLIMB_LEVELS) {
            val parent = cur.parent ?: return cur
            release(cur)
            cur = parent
        }
        return cur
    }

    private fun publish(fareRial: Long) {
        if (settings.overlayEnabled) main.post { overlay.show(fareRial) }
    }

    private fun clearState() {
        if (!::work.isInitialized) return
        work.removeCallbacks(recheck)
        work.post { tracker.reset() }
        main.post { overlay.hide() }
    }

    @Suppress("DEPRECATION")
    private fun release(n: AccessibilityNodeInfo) {
        if (Build.VERSION.SDK_INT < 33) n.recycle()
    }

    override fun onInterrupt() = clearState()

    override fun onDestroy() {
        AppState.serviceRunning.value = false
        if (::work.isInitialized) {
            work.removeCallbacksAndMessages(null)
            thread.quitSafely()
        }
        if (::overlay.isInitialized) overlay.destroy()
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val CLIMB_LEVELS = 3
        const val FULL_SCAN_MIN_INTERVAL_MS = 400L
        const val RECHECK_DELAY_MS = 150L
    }
}
