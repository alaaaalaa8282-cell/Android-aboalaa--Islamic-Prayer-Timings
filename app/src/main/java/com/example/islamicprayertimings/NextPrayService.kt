package com.example.islamicprayertimings

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import java.util.Calendar
import kotlin.concurrent.timer

class NextPrayService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var prefs: SharedPreferences
    private val CHANNEL_ID = "prayer_channel"
    private val NOTIFICATION_ID = 1

    private var prayerTimings = arrayOf("", "", "", "", "", "")
    private var prayerTimingsInSeconds = intArrayOf(0, 0, 0, 0, 0, 0)
    private var isSoundEnabled = true

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        prefs = getSharedPreferences("AppSettings", MODE_PRIVATE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            prayerTimings = intent.getStringArrayExtra("PrayerTimings") ?: prayerTimings
            prayerTimingsInSeconds = intent.getIntArrayExtra("prayerTimingsInSeconds") ?: prayerTimingsInSeconds
            isSoundEnabled = intent.getBooleanExtra("adhanSound", true)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("مواقيت الصلاة")
            .setContentText("جاري التحديث...")
            .setSmallIcon(R.drawable.ic_maghrib)
            .setOngoing(true)
            .build()
            
        startForeground(NOTIFICATION_ID, notification)
        startTimer()
        return START_STICKY
    }

    private fun startTimer() {
        timer("PrayerTimer", false, 0, 1000) {
            val now = Calendar.getInstance()
            val currentSeconds = now.get(Calendar.HOUR_OF_DAY) * 3600 + 
                                 now.get(Calendar.MINUTE) * 60 + 
                                 now.get(Calendar.SECOND)

            for (i in 0 until prayerTimingsInSeconds.size) {
                if (currentSeconds == prayerTimingsInSeconds[i]) {
                    Handler(Looper.getMainLooper()).post {
                        val prayerName = getPrayerName(i)
                        showNotification("$prayerName الآن")
                        playAdhanSound()
                    }
                }
            }
        }
    }

    private fun playAdhanSound() {
        if (!isSoundEnabled) return

        val selectedMuezzin = prefs.getString("selected_muezzin", "منصور الزهراني")
        
        val soundResId = when (selectedMuezzin) {
            "محمد رفعت" -> R.raw.adhan_mohamed_refat
            "النقشبندي" -> R.raw.adhan_elnakshbandy
            "الحصري" -> R.raw.adhan_elhosary
            "الحرم" -> R.raw.adhan_elharm
            else -> R.raw.adhan_mansour_al_zahrani // منصور الزهراني (الافتراضي)
        }

        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, soundResId)
            mediaPlayer?.setVolume(1.0f, 1.0f)
            mediaPlayer?.start()
            
            mediaPlayer?.setOnCompletionListener {
                mediaPlayer?.release()
                mediaPlayer = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    private fun getPrayerName(index: Int): String {
        val names = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")
        return names.getOrElse(index) { "الصلاة" }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Prayer Timings",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun showNotification(content: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("مواقيت الصلاة")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_maghrib)
            .setOngoing(true)
            .build()
            
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
