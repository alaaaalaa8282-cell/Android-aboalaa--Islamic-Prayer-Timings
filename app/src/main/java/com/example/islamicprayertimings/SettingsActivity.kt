package com.example.islamicprayertimings

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val spinner: Spinner = findViewById(R.id.muezzinSpinner)
        val btnBack: Button = findViewById(R.id.btnBackToMain)

        val muezzins = arrayOf(
            "منصور الزهراني",
            "محمد رفعت",
            "الحصري",
            "النقشبندي",
            "الحرم"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, muezzins)
        spinner.adapter = adapter

        val savedChoice = prefs.getString("selected_muezzin", muezzins[0])
        val savedIndex = muezzins.indexOf(savedChoice)
        if (savedIndex != -1) {
            spinner.setSelection(savedIndex)
        }

        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val selected = muezzins[position]
                prefs.edit().putString("selected_muezzin", selected).apply()
                Toast.makeText(this@SettingsActivity, "تم اختيار: $selected", Toast.LENGTH_SHORT).show()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
        }

        btnBack.setOnClickListener { finish() }
    }
}

