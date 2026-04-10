package com.example.islamicprayertimings

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val aladhanAPI = "https://api.aladhan.com/v1/timingsByCity?"
    
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var prayersRecyclerView: RecyclerView
    private lateinit var prayerAdapter: PrayerAdapter
    private lateinit var countryText: EditText
    private lateinit var cityText: EditText
    private lateinit var updateBtn: Button
    private lateinit var settingsButton: Button
    private lateinit var switch24h: SwitchCompat
    private lateinit var switchSound: SwitchCompat

    private var prayerTimings24 = arrayOf("", "", "", "", "", "")
    private var prayerTimings12 = arrayOf("", "", "", "", "", "")
    private var prayerTimingsInSeconds = intArrayOf(0, 0, 0, 0, 0, 0)
    
    private val prayerNames = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")
    private val prayerIcons = listOf(
        R.drawable.ic_fajr,
        R.drawable.ic_sunrise,
        R.drawable.ic_dhuhr,
        R.drawable.ic_asr,
        R.drawable.ic_maghrib,
        R.drawable.ic_isha    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        supportActionBar?.hide()
        initViews()
        setupRecyclerView()
        setupListeners()
        checkNotificationPermission()
        loadPreferences()
        fetchPrayerTimes()
    }

    override fun onDestroy() {
        super.onDestroy()
        savePreferences()
    }

    private fun initViews() {
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        prayersRecyclerView = findViewById(R.id.prayersRecyclerView)
        countryText = findViewById(R.id.Country)
        cityText = findViewById(R.id.City)
        updateBtn = findViewById(R.id.button)
        settingsButton = findViewById(R.id.settingsButton)
        switch24h = findViewById(R.id.switch_24hour)
        switchSound = findViewById(R.id.switch_sound)
    }

    private fun setupRecyclerView() {
        prayersRecyclerView.layoutManager = LinearLayoutManager(this)
        prayerAdapter = PrayerAdapter(emptyList())
        prayersRecyclerView.adapter = prayerAdapter
    }

    private fun setupListeners() {
        swipeRefreshLayout.setOnRefreshListener { fetchPrayerTimes() }
        
        updateBtn.setOnClickListener {
            swipeRefreshLayout.isRefreshing = true
            fetchPrayerTimes()
        }

        settingsButton.setOnClickListener {
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
        }

        switch24h.setOnCheckedChangeListener { _, _ ->
            updatePrayerList() 
            startNextPrayService()
        }

        switchSound.setOnCheckedChangeListener { _, _ ->
            startNextPrayService()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
            }
        }
    }

    private fun updatePrayerList() {
        val timings = if (switch24h.isChecked) prayerTimings12 else prayerTimings24
        val prayerItems = prayerNames.mapIndexed { index, name ->
            PrayerItem(name = name, time = timings[index], iconResId = prayerIcons[index])
        }
        prayerAdapter = PrayerAdapter(prayerItems)
        prayersRecyclerView.adapter = prayerAdapter
    }

    private fun savePreferences() {
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        prefs.edit {
            putString("country", countryText.text?.toString()?.trim() ?: "")
            putString("city", cityText.text.toString()?.trim() ?: "")
            for (i in 0..5) putString("prayer_$i", prayerTimings24[i])
            putBoolean("s24", switch24h.isChecked)
            putBoolean("s_sound", switchSound.isChecked)
        }
    }

    private fun loadPreferences() {
        val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        countryText.setText = prefs.getString("country", "Egypt"))
        cityText.setText = prefs.getString("city", "Cairo")) 
        
        for (i in 0..5) {
            prayerTimings24[i] = prefs.getString("prayer_$i", "").orEmpty()
        }
        
        convertTimings()
        switch24h.isChecked = prefs.getBoolean("s24", true)
        switchSound.isChecked = prefs.getBoolean("s_sound", false)
        updatePrayerList()
    }
    private fun fetchPrayerTimes() {
        val client = OkHttpClient()
        val city = cityText.text.toString().trim()
        val country = countryText.text.toString().trim()
        
        val request = Request.Builder()
            .url("$aladhanAPI&city=$city&country=$country")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "فشل الاتصال", Toast.LENGTH_SHORT).show()
                    swipeRefreshLayout.isRefreshing = false
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "فشل جلب البيانات", Toast.LENGTH_SHORT).show()
                            swipeRefreshLayout.isRefreshing = false
                        }
                        return
                    }

                    val body = response.body?.string().orEmpty()
                    try {
                        val json = JSONObject(body)
                        val timingsJson = json.getJSONObject("data").getJSONObject("timings")
                        
                        prayerTimings24[0] = timingsJson.getString("Fajr")
                        prayerTimings24[1] = timingsJson.getString("Sunrise")
                        prayerTimings24[2] = timingsJson.getString("Dhuhr")
                        prayerTimings24[3] = timingsJson.getString("Asr")
                        prayerTimings24[4] = timingsJson.getString("Maghrib")
                        prayerTimings24[5] = timingsJson.getString("Isha")

                        runOnUiThread {
                            convertTimings()
                            updatePrayerList()
                            swipeRefreshLayout.isRefreshing = false
                            savePreferences()
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "خطأ في البيانات", Toast.LENGTH_SHORT).show()
                            swipeRefreshLayout.isRefreshing = false                        }
                    }
                }
            }
        })
        startNextPrayService()
    }

    private fun convertTimings() {
        for (i in 0..5) {
            prayerTimings12[i] = convertTo12HourFormat(prayerTimings24[i])
            val parts = prayerTimings24[i].split(":")
            if (parts.size >= 2) {
                prayerTimingsInSeconds[i] = ((parts[0].toIntOrNull() ?: 0) * 3600) + ((parts[1].toIntOrNull() ?: 0) * 60)
            }
        }
    }

    private fun convertTo12HourFormat(time24: String): String {
        if (time24.isBlank()) return ""
        return try {
            val inputFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val outputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            val date = inputFormat.parse(time24)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            time24
        }
    }

    private fun startNextPrayService() {
        if (prayerTimings24[0].isNotBlank()) {
            val intent = Intent(this, NextPrayService::class.java).apply {
                putExtra("PrayerTimings", if (switch24h.isChecked) prayerTimings24 else prayerTimings12)
                putExtra("prayerTimingsInSeconds", prayerTimingsInSeconds)
                putExtra("adhanSound", switchSound.isChecked)
            }
            startService(intent)
        }
    }
}
