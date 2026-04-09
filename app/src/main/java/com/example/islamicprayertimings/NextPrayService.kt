package com.example.islamicprayertimings

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.util.*
import kotlin.concurrent.timer

class NextPrayService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var prefs: SharedPreferences
    private val CHANNEL_ID = "prayer_channel"
    private val NOTIFICATION_ID = 1

    // بيانات المواقيت
    private var prayerTimings = arrayOf("", "", "", "", "", "")
    private var prayerTimingsInSeconds = intArrayOf(0, 0, 0, 0, 0, 0)
    private var isSoundEnabled = true
    private var is24HourFormat = true

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        prefs = getSharedPreferences("AppSettings", MODE_PRIVATE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // استقبال البيانات من MainActivity
        if (intent != null) {
            prayerTimings = intent.getStringArrayExtra("PrayerTimings") ?: prayerTimings
            prayerTimingsInSeconds = intent.getIntArrayExtra("prayerTimingsInSeconds") ?: prayerTimingsInSeconds
            isSoundEnabled = intent.getBooleanExtra("adhanSound", true)
        }

        // تشغيل الخدمة في المقدمة (Foreground)
        startForeground(NOTIFICATION_ID, createNotification("جاري التحديث..."))

        // بدء عداد الوقت
        startTimer()

        return START_STICKY
    }

    private fun startTimer() {        timer("PrayerTimer", false, 0, 1000) {
            val now = Calendar.getInstance()
            val currentSeconds = now.get(Calendar.HOUR_OF_DAY) * 3600 + 
                                 now.get(Calendar.MINUTE) * 60 + 
                                 now.get(Calendar.SECOND)

            // التحقق من وقت الصلاة
            for (i in 0 until prayerTimingsInSeconds.size) {
                if (currentSeconds == prayerTimingsInSeconds[i]) {
                    runOnUiThread {
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

        // قراءة اختيار المؤذن من الإعدادات
        val selectedMuezzin = prefs.getString("selected_muezzin", "منصور الزهراني")
        
        // تحديد الملف الصوتي بناءً على الاختيار
        val soundResId = when (selectedMuezzin) {
            "محمد رفعت" -> R.raw.adhan_mohamed_refat
            "النقشبندي" -> R.raw.adhan_elnakshbandy
            "الإمام الحسيني" -> R.raw.adhan_elhosary
            "عبدالباسط عبدالصمد" -> R.raw.adhan_elharm
            else -> R.raw.adhan_mansour_al_zahrani // الافتراضي
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

    private fun getPrayerName(index: Int): String {        val names = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")
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

    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("مواقيت الصلاة")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_maghrib) // يمكنك تغيير الأيقونة
            .setOngoing(true)
            .build()
    }

    private fun showNotification(content: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification(content))
    }

    private fun runOnUiThread(action: () -> Unit) {
        android.os.Handler(android.os.Looper.getMainLooper()).post(action)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}        if (diff < 0) diff += 24 * 3600

        if (diff <= 1 && adhanSound == true && nextIdx != 1) {
          val intent = Intent(this@NextPrayService, AdhanSound::class.java)
          startService(intent)
        }

        // Countdown and update notification
        val countdown = secondsToHHMMSS(diff)
        val prayName = getString(getPrayerNameResId(nextIdx))
        updateNotification(prayName, nextIdx, countdown)
        handler.postDelayed(this, 1000)
      }
    }
    handler.post(runnable)
  }

  private fun getPrayerNameResId(index: Int): Int {
    return when (index) {
      0 -> R.string.Fajr
      1 -> R.string.Sunrise
      2 -> R.string.Dhuhr
      3 -> R.string.Asr
      4 -> R.string.Maghrib
      5 -> R.string.Isha
      else -> R.string.app_name
    }
  }

  private fun secondsToHHMMSS(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours == 0) {
      String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    } else {
      String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    }
  }
}
