package com.marcossilqueira.hzchanger

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import rikka.shizuku.Shizuku
import java.io.File

class HzChangerService : Service() {

    companion object {
        private const val TAG = "HzChangerService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "hz_changer_channel"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, getString(R.string.log_service_started))

        // Cria uma notificação para o serviço em primeiro plano
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(getString(R.string.service_notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        // Processa a intenção
        if (intent != null) {
            val hz = intent.getIntExtra("hz", 60)
            val isFromWidget = intent.getBooleanExtra("is_from_widget", false)

            if (intent.getBooleanExtra("toggle_fix", false)) {
                val isFixed = intent.getBooleanExtra("is_fixed", false)
                setRefreshRate(hz, isFixed)
            } else {
                // Carrega o estado atual de "fixar"
                val prefs = getSharedPreferences(
                    "com.marcossilqueira.hzchanger.HzChangerWidget",
                    Context.MODE_PRIVATE
                )
                val isFixed = prefs.getBoolean("is_fixed_hz", false)
                setRefreshRate(hz, isFixed)
            }
        }

        // Para o serviço após executar o comando
        stopSelf(startId)

        return START_NOT_STICKY
    }

    private fun setRefreshRate(hz: Int, isFixed: Boolean) {
        Log.d(TAG, getString(R.string.log_setting_refresh_rate, hz, isFixed))

        val commands = if (isFixed) {
            // Fixar: define min e peak para o mesmo valor
            arrayOf(
                "settings put system peak_refresh_rate $hz.0",
                "settings put system min_refresh_rate $hz.0"
            )
        } else {
            // Não fixar: define apenas peak, min volta para 60
            arrayOf(
                "settings put system peak_refresh_rate $hz.0",
                "settings put system min_refresh_rate 60.0"
            )
        }

        try {
            if (isDeviceRooted()) {
                for (cmd in commands) {
                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
                    process.waitFor()
                    Log.d(TAG, getString(R.string.log_command_executed_root, cmd))
                }
            } else if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                // Implementação para Shizuku (se disponível)
                Log.d(TAG, getString(R.string.log_shizuku_available_not_implemented))
                // Aqui você implementaria a lógica para Shizuku
            } else {
                Log.e(TAG, getString(R.string.log_neither_root_nor_shizuku))
            }
        } catch (e: Exception) {
            Log.e(TAG, getString(R.string.log_error_setting_rate, e.message ?: "Erro desconhecido"), e)
        }
    }

    private fun isDeviceRooted(): Boolean {
        // Verifica se o binário 'su' existe em locais comuns
        val paths = arrayOf(
            "/system/bin/", "/system/xbin/", "/sbin/", "/system/sd/xbin/",
            "/system/bin/failsafe/", "/data/local/xbin/", "/data/local/bin/", "/data/local/"
        )
        for (path in paths) {
            val file = File(path + "su")
            if (file.exists()) {
                return true
            }
        }

        // Tenta executar o comando 'su'
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo root"))
            val result = process.inputStream.bufferedReader().readText()
            process.destroy()
            result.contains("root")
        } catch (e: Exception) {
            false
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.service_channel_name)
            val descriptionText = getString(R.string.service_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}