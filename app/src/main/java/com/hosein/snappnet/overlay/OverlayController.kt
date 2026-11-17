package com.hosein.snappnet.overlay

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.hosein.snappnet.R
import com.hosein.snappnet.core.FareCalculator
import com.hosein.snappnet.core.NumberNormalizer
import com.hosein.snappnet.data.AppSettings

/**
 * Floating net-earnings card. Main thread only.
 *
 * Uses TYPE_ACCESSIBILITY_OVERLAY: available to an enabled AccessibilityService without SYSTEM_ALERT_WINDOW and,
 * unlike TYPE_APPLICATION_OVERLAY, it does not trip an app's "obscured touch" protection, so the Snapp Box accept
 * button keeps working. The window is created once; later it is only shown/hidden, restyled and re-texted.
 */
class OverlayController(
    private val service: AccessibilityService,
    private val onMoved: (x: Int, y: Int) -> Unit,
) {
    private val wm = service.getSystemService(WindowManager::class.java)
    private val dm = service.resources.displayMetrics
    private val font: Typeface = runCatching { service.resources.getFont(R.font.shabnam_fd) }.getOrDefault(Typeface.DEFAULT)

    private var settings = AppSettings()
    private var gross: Long? = null // always in rial, as shown by Snapp Box
    private var minimized = false
    private var attached = false

    private lateinit var card: LinearLayout
    private lateinit var title: TextView
    private lateinit var net: TextView
    private lateinit var grossTv: TextView
    private lateinit var deductTv: TextView
    private lateinit var toggle: TextView

    private val lp = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply { gravity = Gravity.TOP or Gravity.START }

    private fun dp(v: Int) = (v * dm.density).toInt()
    private fun opaque(rgb: Int) = 0xFF000000.toInt() or rgb

    fun applySettings(s: AppSettings) {
        settings = s
        if (!s.overlayEnabled) { hide(); return }
        if (attached) {
            applyStyle()
            gross?.let { render(it) }
        }
    }

    fun show(fareRial: Long) {
        gross = fareRial
        if (!settings.overlayEnabled) return
        if (!attached && !attach()) return
        render(fareRial)
        card.visibility = View.VISIBLE
    }

    fun hide() {
        gross = null
        if (attached) card.visibility = View.GONE
    }

    fun destroy() {
        if (attached) {
            runCatching { wm.removeViewImmediate(card) }
            attached = false
        }
    }

    private fun render(fareRial: Long) {
        val inToman = settings.displayInToman
        val b = FareCalculator.breakdown(fareRial, FareCalculator.DEFAULT_RATE_PERCENT, inToman)
        val unit = service.getString(if (inToman) R.string.toman else R.string.rial)
        net.text = "${NumberNormalizer.format(b.net, persianDigits = true)} $unit"
        grossTv.text = service.getString(R.string.overlay_gross, NumberNormalizer.format(b.gross, true), unit)
        deductTv.text = service.getString(R.string.overlay_deductions, NumberNormalizer.format(b.deductionPercent.toLong(), true))

        val netRial = FareCalculator.netEarnings(fareRial)
        val low = settings.lowThresholdToman > 0 && netRial < settings.lowThresholdToman.toLong() * 10
        net.setTextColor(if (low) LOW_COLOR else opaque(settings.style.netColor))

        grossTv.visibility = if (!minimized && settings.showGross) View.VISIBLE else View.GONE
        deductTv.visibility = if (!minimized && settings.showDeductions) View.VISIBLE else View.GONE
        toggle.text = if (minimized) "+" else "–"
    }

    private fun applyStyle() {
        val st = settings.style
        card.background = GradientDrawable().apply {
            setColor((((st.bgAlpha * 255) / 100) shl 24) or st.bgColor)
            cornerRadius = dp(st.corner).toFloat()
            setStroke(dp(1), 0x33FFFFFF)
        }
        card.setPadding(dp(st.padding), dp(maxOf(2, st.padding / 3)), dp(st.padding), dp(st.padding))
        val text = opaque(st.textColor)
        for (tv in listOf(title, grossTv, deductTv)) {
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, st.textSize.toFloat())
            tv.setTextColor(text)
        }
        toggle.setTextSize(TypedValue.COMPLEX_UNIT_SP, (st.textSize + 6).toFloat())
        toggle.setTextColor(text)
        net.setTextSize(TypedValue.COMPLEX_UNIT_SP, st.netSize.toFloat())
        title.visibility = if (settings.showTitle) View.VISIBLE else View.GONE
        lp.width = if (st.widthDp > 0) dp(st.widthDp) else WindowManager.LayoutParams.WRAP_CONTENT
        if (attached) runCatching { wm.updateViewLayout(card, lp) }
    }

    private fun attach(): Boolean {
        buildViews()
        minimized = settings.startMinimized
        applyStyle()
        lp.x = if (settings.overlayX >= 0) settings.overlayX else dp(16)
        lp.y = if (settings.overlayY >= 0) settings.overlayY else dp(96)
        return try {
            wm.addView(card, lp)
            attached = true
            true
        } catch (e: RuntimeException) { // WindowManager.BadTokenException etc.
            false
        }
    }

    private fun buildViews() {
        card = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            visibility = View.GONE
        }
        val header = LinearLayout(service).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        title = label().apply { text = service.getString(R.string.overlay_title) }
        toggle = label().apply {
            gravity = Gravity.CENTER
            setPadding(dp(10), 0, dp(10), 0)
            setOnClickListener { minimized = !minimized; gross?.let { render(it) } }
        }
        header.addView(title, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        header.addView(toggle)

        net = label().apply { setTypeface(font, Typeface.BOLD) }
        grossTv = label()
        deductTv = label()

        card.addView(header)
        card.addView(net)
        card.addView(grossTv)
        card.addView(deductTv)
        card.setOnTouchListener(DragListener())
    }

    private fun label() = TextView(service).apply {
        typeface = font
        textDirection = View.TEXT_DIRECTION_RTL
        gravity = Gravity.START
        includeFontPadding = false
    }

    private inner class DragListener : View.OnTouchListener {
        private var startX = 0
        private var startY = 0
        private var downX = 0f
        private var downY = 0f

        override fun onTouch(v: View, e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = lp.x; startY = lp.y; downX = e.rawX; downY = e.rawY
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = (startX + (e.rawX - downX).toInt()).coerceIn(0, maxOf(0, dm.widthPixels - v.width))
                    lp.y = (startY + (e.rawY - downY).toInt()).coerceIn(0, maxOf(0, dm.heightPixels - v.height))
                    wm.updateViewLayout(v, lp)
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    onMoved(lp.x, lp.y)
                    return true
                }
            }
            return false
        }
    }

    private companion object {
        val LOW_COLOR = 0xFFFF5252.toInt()
    }
}
