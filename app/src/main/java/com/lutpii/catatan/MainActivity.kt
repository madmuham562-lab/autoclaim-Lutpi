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
    "Tanggal", "Jenis", "ID Akun", "Nama Akun", "Karakter", "Server",
    "Dapat VG hari ini", "Saldo/Sisa VG", "Diamond hasil tukar", "Saldo diamond",
    "Diamond terkirim", "Popularita
