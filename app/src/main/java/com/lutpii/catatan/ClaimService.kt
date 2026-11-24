package com.lutpii.catatan

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.os.Build
import android.view.Display
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class ClaimService : AccessibilityService() {
    companion object { @Volatile var inst: ClaimService? = null }

    private var panel: LinearLayout? = null
    private var running = false

    override fun onServiceConnected() { inst = this; showPanel() }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onUnbind(intent: Intent?): Boolean {
        hidePanel(); inst = null
        return super.onUnbind(intent)
    }

    fun tap(x: Float, y: Float, done: (Boolean) -> Unit = {}) {
        val p = Path().apply { moveTo(x, y) }
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p, 0, 60)).build()
        val ok = dispatchGesture(g, object : GestureResultCallback() {
            override fun onCompleted(d: GestureDescription?) { done(true) }
            override fun onCancelled(d: GestureDescription?) { done(false) }
        }, null)
        if (!ok) done(false)
    }

    fun shot(done: (Bitmap?) -> Unit) {
        if (Build.VERSION.SDK_INT < 30) { done(null); return }
        takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, object : TakeScreenshotCallback {
            override fun onSuccess(r: ScreenshotResult) {
                val hb = r.hardwareBuffer
                val bm = Bitmap.wrapHardwareBuffer(hb, r.colorSpace)
                hb.close()
                done(bm?.copy(Bitmap.Config.ARGB_8888, false))
            }
            override fun onFailure(code: Int) { done(null) }
        })
    }

    private fun tesLayar(st: TextView) {
        shot { bm ->
            if (bm == null) { st.text = "Screenshot gagal (butuh Android 11+)"; return@shot }
            val w = bm.width
            val h = bm.height
            st.text = "Layar terbaca ${w}x$h, mengetuk tengah..."
            tap(w / 2f, h / 2f) { ok ->
                st.text = "Layar ${w}x$h - ketuk " + (if (ok) "berhasil" else "gagal")
            }
        }
    }

    private fun showPanel() {
        if (panel != null) return
        val d = resources.displayMetrics.density
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.argb(190, 20, 20, 30))
            setPadding((8 * d).toInt(), (6 * d).toInt(), (8 * d).toInt(), (6 * d).toInt())
        }
        val st = TextView(this).apply {
            setTextColor(Color.WHITE); textSize = 11f; text = "Bot Lutpii siap (Tahap 1)"
        }
        val row = LinearLayout(this)
        val b1 = Button(this).apply { text = "Mulai"; textSize = 11f; isAllCaps = false }
        b1.setOnClickListener {
            running = !running
            b1.text = if (running) "Stop" else "Mulai"
            st.text = if (running) "Jalan (belum ada langkah klaim)" else "Berhenti"
        }
        val b2 = Button(this).apply { text = "Tes"; textSize = 11f; isAllCaps = false }
        b2.setOnClickListener { tesLayar(st) }
        row.addView(b1)
        row.addView(b2)
        box.addView(st)
        box.addView(row)
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = 16
        lp.y = 16
        (getSystemService(WINDOW_SERVICE) as WindowManager).addView(box, lp)
        panel = box
    }

    private fun hidePanel() {
        panel?.let {
            try { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it) } catch (e: Exception) {}
        }
        panel = null
    }
}
