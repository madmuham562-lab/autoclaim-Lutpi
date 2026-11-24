package com.lutpii.catatan

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class BotActivity : Activity() {
    private lateinit var st: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val d = resources.displayMetrics.density
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * d).toInt(), (40 * d).toInt(), (16 * d).toInt(), (16 * d).toInt())
        }
        root.addView(TextView(this).apply {
            text = "Bot Lutpii"; textSize = 22f; setTypeface(null, Typeface.BOLD)
        })
        st = TextView(this).apply {
            textSize = 15f; setPadding(0, (12 * d).toInt(), 0, (12 * d).toInt())
        }
        root.addView(st)
        root.addView(Button(this).apply {
            text = "Buka Pengaturan Aksesibilitas"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })
        root.addView(TextView(this).apply {
            textSize = 13f
            setPadding(0, (12 * d).toInt(), 0, 0)
            text = "Cara aktifkan:\n" +
                "1. Tap tombol di atas, cari 'Bot Lutpii' di Aplikasi terinstal/Layanan.\n" +
                "2. Nyalakan, lalu izinkan.\n" +
                "3. Panel melayang akan muncul di pojok kiri atas layar.\n\n" +
                "Kalau tombol nyala abu-abu (Pengaturan terbatas): buka Info Aplikasi Catatan Lutpii, " +
                "tap titik tiga di kanan atas, pilih 'Izinkan pengaturan terbatas', lalu ulangi."
        })
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        st.text = if (ClaimService.inst != null)
            "Status: AKTIF, panel melayang tampil di layar"
        else "Status: belum aktif"
    }
}
