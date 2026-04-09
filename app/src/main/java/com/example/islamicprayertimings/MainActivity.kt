package com.example.islamicprayertimings

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.edit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import okhttp3.Call
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity(){
    private val aladhanAPI = "https://api.aladhan.com/v1/timingsByCity?"
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var prayersRecyclerView: RecyclerView
    private lateinit var prayerAdapter: PrayerAdapter

    private lateinit var country : TextView
    private lateinit var city : TextView

    private lateinit var updateBtn : Button
    private lateinit var switch24h: SwitchCompat
    private lateinit var switchSound: SwitchCompat

    private var prayerTimings24 = Array<String>(6) {""}
    private var prayerTimings12 = Array<String>(6) {""}
    private var prayerTimingsInSeconds = IntArray(6)
    
    private val prayerNames = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")
    private val prayerIcons = listOf(
        R.drawable.ic_fajr,
        R.drawable.ic_sunrise,
        R.drawable.ic_dhuhr,
        R.drawable.ic_asr,
        R.drawable.ic_maghrib,
        R.drawable.ic_isha
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        refUI()
        setupRecyclerView()
        setupListeners()
        getNotificationsPerm()
        loadSharedPreferences()
        getTimings()
    }

    override fun onDestroy() {
        super.onDestroy()
        saveSharedPreferences()
    }

    private fun refUI(){
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        prayersRecyclerView = findViewById(R.id.prayersRecyclerView)

        country = findViewById<TextView>(R.id.Country)
        city = findViewById<TextView>(R.id.City)

        updateBtn = findViewById<Button>(R.id.button)
        switch24h = findViewById<SwitchCompat>(R.id.switch_24hour)
        switchSound = findViewById<SwitchCompat>(R.id.switch_sound)
    }
    
    private fun setupRecyclerView() {
        prayersRecyclerView.layoutManager = LinearLayoutManager(this)
        prayerAdapter = PrayerAdapter(emptyList())
        prayersRecyclerView.adapter = prayerAdapter
    }

    private fun updatePrayerList() {
        val prayers = mutableListOf<PrayerItem>()
        val timings = if (switch24h.isChecked) prayerTimings12 else prayerTimings24
        
        for (i in 0 until 6) {
            prayers.add(PrayerItem(
                name = prayerNames[i],
                time = timings[i],
                iconResId = prayerIcons[i]
            ))
        }
        
        prayerAdapter = PrayerAdapter(prayers)
        prayersRecyclerView.adapter = prayerAdapter
    }

    private fun getNotificationsPerm(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 100)
            }
        }
    }

    private fun setupListeners() {
        swipeRefreshLayout.setOnRefreshListener {
            getTimings()
        }
        updateBtn.setOnClickListener {
            swipeRefreshLayout.isRefreshing = true
            getTimings()
        }
        switch24h.setOnCheckedChangeListener {_, isChecked ->
            updatePrayerList()
            startNextPrayService()
        }
        switchSound.setOnCheckedChangeListener { _,isChecked ->
            startNextPrayService()
        }
    }

    private fun saveSharedPreferences() {
        val sharedPreferences = getSharedPreferences("prefs", MODE_PRIVATE)
        sharedPreferences.edit {
            putString("country", country.text.toString().trim())
            putString("city", city.text.toString().trim())

            putString("fajr", prayerTimings24[0])
            putString("sunrise", prayerTimings24[1])
            putString("dhuhr", prayerTimings24[2])
            putString("asr", prayerTimings24[3])
            putString("maghrib", prayerTimings24[4])
            putString("isha", prayerTimings24[5])

            putBoolean("s24", switch24h.isChecked)
            putBoolean("s_sound", switchSound.isChecked)
        }
    }

    private fun loadSharedPreferences(){
        val sharedPreferences = getSharedPreferences("prefs", MODE_PRIVATE)
        country.text = sharedPreferences.getString("country", "Egypt")
        city.text = sharedPreferences.getString("city", "Cairo")

        prayerTimings24[0] = sharedPreferences.getString("fajr", "").toString()
        prayerTimings24[1] = sharedPreferences.getString("sunrise", "").toString()
        prayerTimings24[2] = sharedPreferences.getString("dhuhr", "").toString()        prayerTimings24[3] = sharedPreferences.getString("asr", "").toString()
        prayerTimings24[4] = sharedPreferences.getString("maghrib", "").toString()
        prayerTimings24[5] = sharedPreferences.getString("isha", "").toString()

        for (i in 0 until  6) if (prayerTimings24[0].isNotEmpty()) {
            prayerTimings12[i] = convertTo12HourFormat(prayerTimings24[i])
            val parts = prayerTimings24[i].split(":")
            prayerTimingsInSeconds[i] = parts[0].toInt() * 3600 + parts[1].toInt() * 60
        }

        switch24h.isChecked = sharedPreferences.getBoolean("s24", true)
        switchSound.isChecked = sharedPreferences.getBoolean("s_sound", false)
        
        updatePrayerList()
    }

    private fun getTimings(){
        val client = okhttp3.OkHttpClient()
        val request = okhttp3.Request.Builder().url(aladhanAPI + "city=" + city.text.trim() +
                "&country=" + country.text.trim()).build()
        client.newCall(request).enqueue(object : okhttp3.Callback{
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "No internet connection", Toast.LENGTH_SHORT).show()
                    swipeRefreshLayout.isRefreshing = false
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Failed to retrieve prayer timings", Toast.LENGTH_LONG).show()
                            swipeRefreshLayout.isRefreshing = false
                        }
                        return
                    }
                    val body = response.body?.string()
                    if (body.isNullOrEmpty()) return

                    try{
                        val jsonResponse = JSONObject(body)
                        val data = jsonResponse.getJSONObject("data")
                        val timings = data.getJSONObject("timings")

                        prayerTimings24[0] = toDefaultLocale(timings.getString("Fajr"))
                        prayerTimings24[1] = toDefaultLocale(timings.getString("Sunrise"))
                        prayerTimings24[2] = toDefaultLocale(timings.getString("Dhuhr"))
                        prayerTimings24[3] = toDefaultLocale(timings.getString("Asr"))
                        prayerTimings24[4] = toDefaultLocale(timings.getString("Maghrib"))                        prayerTimings24[5] = toDefaultLocale(timings.getString("Isha"))

                        for (i in 0 until  6){
                            prayerTimings12[i] = convertTo12HourFormat(prayerTimings24[i])

                            val parts = prayerTimings24[i].split(":")
                            prayerTimingsInSeconds[i] = parts[0].toInt() * 3600 + parts[1].toInt() * 60
                        }

                        runOnUiThread {
                            updatePrayerList()
                            swipeRefreshLayout.isRefreshing = false
                            saveSharedPreferences()
                        }
                    } catch (_: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Error parsing response", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        })
        startNextPrayService()
    }

    fun convertTo12HourFormat(time24: String): String {
        if (time24.isBlank()) return ""
        return try {
            val inputFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
            val date = inputFormat.parse(time24)
            outputFormat.format(date!!)
        } catch (_: Exception) {
            ""
        }
    }

    fun toDefaultLocale(time24: String): String {
        if (time24.isBlank()) return ""
        return try{
            val inputFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val date = inputFormat.parse(time24)
            outputFormat.format(date!!)
        } catch (_: Exception) {
            ""
        }
    }
    
    fun startNextPrayService(){        if(prayerTimings24[0] != ""){
            val serviceIntent = Intent(this@MainActivity, NextPrayService::class.java).apply {
                if(switch24h.isChecked){
                    putExtra("PrayerTimings", prayerTimings24)
                }
                else{
                    putExtra("PrayerTimings", prayerTimings12)
                }
                putExtra("prayerTimingsInSeconds", prayerTimingsInSeconds)
                putExtra("adhanSound", switchSound.isChecked)
            }
            startService(serviceIntent)
        }
    }
}
