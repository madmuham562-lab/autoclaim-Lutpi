package com.lutpii.catatan

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

val COLS = listOf(
    "Tanggal",
    "Jenis",
    "ID Akun",
    "Nama Akun",
    "Karakter",
    "Server",
    "Dapat VG hari ini",
    "Saldo/Sisa VG",
    "Diamond hasil tukar",
    "Saldo diamond",
    "Diamond terkirim",
    "Popularitas awal",
    "Popularitas akhir",
    "Jam"
)
private val DB_COLS = listOf("tanggal", "jenis", "acc", "nama", "karakter", "server", "dapat", "saldo", "tukar", "saldodia", "kirim", "pa", "pk", "jam")

class Db(c: Context) : SQLiteOpenHelper(c, "catatan.db", null, 1) {
    override fun onCreate(d: SQLiteDatabase) {
        d.execSQL("CREATE TABLE c(id INTEGER PRIMARY KEY AUTOINCREMENT," + DB_COLS.joinToString(",") { "$it TEXT" } + ")")
    }
    override fun onUpgrade(d: SQLiteDatabase, o: Int, n: Int) {}

    private fun cv(v: List<String>): ContentValues {
        val x = ContentValues()
        DB_COLS.forEachIndexed { i, k -> x.put(k, v.getOrElse(i) { "" }) }
        return x
    }
    fun insert(v: List<String>) { writableDatabase.insert("c", null, cv(v)) }
    fun update(id: Long, v: List<String>) { writableDatabase.update("c", cv(v), "id=?", arrayOf(id.toString())) }
    fun delete(id: Long) { writableDatabase.delete("c", "id=?", arrayOf(id.toString())) }
    fun clear() { writableDatabase.delete("c", null, null) }
    fun query(tgl: String?): List<Pair<Long, List<String>>> {
        val out = ArrayList<Pair<Long, List<String>>>()
        val cur = if (tgl == null) readableDatabase.rawQuery("SELECT * FROM c ORDER BY id", null)
        else readableDatabase.rawQuery("SELECT * FROM c WHERE tanggal=? ORDER BY id", arrayOf(tgl))
        cur.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow("id"))
                out.add(id to DB_COLS.map { k -> it.getString(it.getColumnIndexOrThrow(k)) ?: "" })
            }
        }
        return out
    }
}

class MainActivity : Activity() {
    private lateinit var db: Db
    private lateinit var list: ListView
    private lateinit var info: TextView
    private lateinit var btnTgl: Button
    private var filter: String? = null
    private var rows = listOf<Pair<Long, List<String>>>()
    private val items = ArrayList<String>()
    private lateinit var adapter: ArrayAdapter<String>
    private val fmtTgl = SimpleDateFormat("dd-MM-yyyy", Locale.US)

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        db = Db(this)
        filter = fmtTgl.format(Date())

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(36), dp(12), dp(8)) }
        root.addView(TextView(this).apply { text = "Catatan Lutpii"; textSize = 22f; setTypeface(null, Typeface.BOLD) })

        btnTgl = Button(this).apply {
            setOnClickListener { pilihTanggal() }
            setOnLongClickListener { filter = null; refresh(); true }
        }
        root.addView(btnTgl)
        root.addView(TextView(this).apply { text = "Tap: pilih tanggal  •  Tahan: tampilkan semua"; textSize = 11f })

        val bar1 = LinearLayout(this)
        bar1.addView(btn("+ Tambah") { form(null, null) })
        bar1.addView(btn("Export CSV") { exportCsv() })
        bar1.addView(btn("Import CSV") { pick("text/*", 1) })
        root.addView(bar1)
        val bar2 = LinearLayout(this)
        bar2.addView(btn("Backup JSON") { backupJson() })
        bar2.addView(btn("Restore JSON") { pick("*/*", 2) })
        root.addView(bar2)

        info = TextView(this).apply { textSize = 13f; setPadding(0, dp(8), 0, dp(8)) }
        root.addView(info)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        list = ListView(this).apply { this.adapter = this@MainActivity.adapter }
        list.setOnItemClickListener { _, _, pos, _ -> aksi(rows[pos]) }
        root.addView(list, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        refresh()
    }

    private fun btn(t: String, f: () -> Unit) = Button(this).apply {
        text = t; textSize = 11f; isAllCaps = false
        layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        setOnClickListener { f() }
    }

    private fun refresh() {
        rows = db.query(filter)
        btnTgl.text = "Tanggal: " + (filter ?: "SEMUA")
        items.clear()
        var vg = 0; var tukar = 0; var kirim = 0
        for ((_, r) in rows) {
            vg += r[6].toIntOrNull() ?: 0
            tukar += r[8].toIntOrNull() ?: 0
            kirim += r[10].toIntOrNull() ?: 0
            val tgl = if (filter == null) r[0] + " " else ""
            items.add("$tgl${r[13]} • ${r[3]} • ${r[4]} • S${r[5]}\n${r[1]} • VG +${r[6].ifEmpty { "0" }} (sisa ${r[7].ifEmpty { "-" }}) • tukar ${r[8].ifEmpty { "0" }} • kirim ${r[10].ifEmpty { "0" }}")
        }
        adapter.notifyDataSetChanged()
        info.text = "${rows.size} baris  |  Dapat VG: $vg  |  Diamond tukar: $tukar  |  Terkirim: $kirim"
    }

    private fun pilihTanggal() {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            c.set(y, m, d); filter = fmtTgl.format(c.time); refresh()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun aksi(p: Pair<Long, List<String>>) {
        AlertDialog.Builder(this).setItems(arrayOf("Edit", "Duplikat", "Hapus")) { _, w ->
            when (w) {
                0 -> form(p.first, p.second)
                1 -> { db.insert(p.second); refresh() }
                2 -> AlertDialog.Builder(this).setMessage("Hapus baris ini?")
                    .setPositiveButton("Hapus") { _, _ -> db.delete(p.first); refresh() }
                    .setNegativeButton("Batal", null).show()
            }
        }.show()
    }

    private fun form(id: Long?, v: List<String>?) {
        val now = Date()
        val def = v ?: listOf(fmtTgl.format(now), "Claim saja", "", "", "", "", "", "", "0", "", "0", "", "", SimpleDateFormat("HH:mm", Locale.US).format(now))
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(8)) }
        val ets = COLS.mapIndexed { i, label ->
            EditText(this).apply {
                hint = label; setText(def[i]); setSingleLine()
                if (i in listOf(6, 7, 8, 9, 10)) inputType = InputType.TYPE_CLASS_NUMBER
            }.also { box.addView(it) }
        }
        val sv = ScrollView(this).apply { addView(box) }
        AlertDialog.Builder(this).setTitle(if (id == null) "Tambah catatan" else "Edit catatan").setView(sv)
            .setPositiveButton("Simpan") { _, _ ->
                val vals = ets.map { it.text.toString().trim() }
                if (id == null) db.insert(vals) else db.update(id, vals)
                refresh()
            }.setNegativeButton("Batal", null).show()
    }

    private fun esc(s: String) = if (s.contains(';') || s.contains('"') || s.contains('\n')) "\"" + s.replace("\"", "\"\"") + "\"" else s

    private fun saveDownload(name: String, mime: String, content: String) {
        val cv = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, "Download")
        }
        val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
        if (uri == null) { toast("Gagal menyimpan file"); return }
        contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
        toast("Tersimpan di folder Download: $name")
    }

    private fun exportCsv() {
        val sb = StringBuilder("\uFEFF").append(COLS.joinToString(";")).append("\r\n")
        for ((_, r) in rows) sb.append(r.joinToString(";") { esc(it) }).append("\r\n")
        val suffix = filter?.let { val p = it.split("-"); p[2] + p[1] + p[0] } ?: "semua"
        saveDownload("catatan_$suffix.csv", "text/csv", sb.toString())
    }

    private fun backupJson() {
        val arr = JSONArray()
        for ((_, r) in db.query(null)) {
            val o = JSONObject()
            DB_COLS.forEachIndexed { i, k -> o.put(k, r[i]) }
            arr.put(o)
        }
        val root = JSONObject().put("app", "CatatanLutpii").put("versi", "1").put("dibuat", System.currentTimeMillis()).put("catatan", arr)
        saveDownload("catatan_backup.json", "application/json", root.toString())
    }

    private fun pick(mime: String, code: Int) {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(Intent.CATEGORY_OPENABLE); type = mime }
        startActivityForResult(i, code)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        val uri: Uri = data?.data ?: return
        if (res != RESULT_OK) return
        val text = try { contentResolver.openInputStream(uri)!!.bufferedReader(Charsets.UTF_8).readText().removePrefix("\uFEFF") }
        catch (e: Exception) { toast("Gagal membaca file"); return }
        try {
            if (req == 1) importCsv(text) else restoreJson(text)
        } catch (e: Exception) { toast("File tidak valid: ${e.message}") }
    }

    private fun parseCsv(t: String): List<List<String>> {
        val out = ArrayList<List<String>>(); var row = ArrayList<String>(); val cur = StringBuilder(); var q = false; var i = 0
        while (i < t.length) {
            val ch = t[i]
            if (q) {
                if (ch == '"') { if (i + 1 < t.length && t[i + 1] == '"') { cur.append('"'); i++ } else q = false } else cur.append(ch)
            } else when (ch) {
                '"' -> q = true
                ';' -> { row.add(cur.toString()); cur.setLength(0) }
                '\r' -> {}
                '\n' -> { row.add(cur.toString()); cur.setLength(0); out.add(row); row = ArrayList() }
                else -> cur.append(ch)
            }
            i++
        }
        if (cur.isNotEmpty() || row.isNotEmpty()) { row.add(cur.toString()); out.add(row) }
        return out
    }

    private fun importCsv(t: String) {
        val data = parseCsv(t).filter { it.size >= 2 }.drop(1)
        data.forEach { db.insert(it) }
        filter = data.firstOrNull()?.get(0) ?: filter
        refresh(); toast("${data.size} baris diimpor")
    }

    private fun restoreJson(t: String) {
        val arr = JSONObject(t).getJSONArray("catatan")
        AlertDialog.Builder(this).setMessage("Restore akan MENGGANTI semua catatan sekarang dengan ${arr.length()} baris dari backup. Lanjut?")
            .setPositiveButton("Ganti") { _, _ ->
                db.clear()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    db.insert(DB_COLS.map { o.optString(it, "") })
                }
                refresh(); toast("Restore selesai")
            }.setNegativeButton("Batal", null).show()
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}
