package com.nidanursigirta.smartcampussafety

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.widget.Toast

class NotificationSettingsActivity : AppCompatActivity() {

    private lateinit var switchHealth: Switch
    private lateinit var switchSecurity: Switch
    private lateinit var switchEnvironment: Switch
    private lateinit var switchTechnical: Switch
    private lateinit var switchLost: Switch
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_settings)

        // Switch bağlama
        switchHealth = findViewById(R.id.switchHealth)
        switchSecurity = findViewById(R.id.switchSecurity)
        switchEnvironment = findViewById(R.id.switchEnvironment)
        switchTechnical = findViewById(R.id.switchTechnical_fault)
        switchLost = findViewById(R.id.switchLost_found)
        btnSave = findViewById(R.id.btnSave)


        val btnBack: ImageView = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        // Eskiden kaydedilen ayarları yükle
        loadSettings()

        // Kaydet butonu
        btnSave.setOnClickListener {
            saveSettings()
        }
    }

    // ------------------------------------------
    //   SharedPreferences ile Ayarları Kaydetme
    // ------------------------------------------
    private fun saveSettings() {
        val sp = getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE)
        val editor = sp.edit()

        editor.putBoolean("health", switchHealth.isChecked)
        editor.putBoolean("security", switchSecurity.isChecked)
        editor.putBoolean("environment", switchEnvironment.isChecked)
        editor.putBoolean("technical", switchTechnical.isChecked)
        editor.putBoolean("lost", switchLost.isChecked)

        editor.apply()

        // Başarıyla kaydedildi mesajı göster
        Toast.makeText(this, "Başarıyla kaydedildi", Toast.LENGTH_SHORT).show()
    }

    // ------------------------------------------
    //   Kaydedilen Ayarları Ekrana Yükleme
    // ------------------------------------------
    private fun loadSettings() {
        val sp = getSharedPreferences("NotificationPrefs", Context.MODE_PRIVATE)

        switchHealth.isChecked = sp.getBoolean("health", true)
        switchSecurity.isChecked = sp.getBoolean("security", true)
        switchEnvironment.isChecked = sp.getBoolean("environment", true)
        switchTechnical.isChecked = sp.getBoolean("technical", true)
        switchLost.isChecked = sp.getBoolean("lost", true)
    }


}