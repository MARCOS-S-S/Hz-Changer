package com.marcossilqueira.hzchanger

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.annotation.RequiresApi
import rikka.shizuku.Shizuku

@RequiresApi(Build.VERSION_CODES.N)
class HzChangerTileService : TileService() {

    companion object {
        private const val TAG = "HzChangerTileService"
        private const val PREFS_NAME = "HzChangerTileService"
        private const val PREF_CURRENT_STATE = "current_state"
        private const val PREF_CURRENT_HZ = "current_hz"
        const val ACTION_UPDATE_TILE = "com.marcossilqueira.hzchanger.UPDATE_TILE"
    }

    private val handler = Handler(Looper.getMainLooper())
    private val broadcastReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.Q)
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "BroadcastReceiver: Recebido broadcast de atualização do widget")
            // Força sincronização e atualização do tile
            syncWithWidget()
            updateTileImmediately()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartListening() {
        super.onStartListening()
        Log.d(TAG, "onStartListening: Tile começou a escutar")
        
        // Registrar BroadcastReceiver para detectar atualizações do widget
        val filter = IntentFilter(ACTION_UPDATE_TILE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(broadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(broadcastReceiver, filter)
        }
        
        // Sincroniza com o estado do widget se disponível
        syncWithWidget()
        
        updateTile()
    }

    override fun onStopListening() {
        super.onStopListening()
        Log.d(TAG, "onStopListening: Tile parou de escutar")
        
        // Desregistrar BroadcastReceiver
        try {
            unregisterReceiver(broadcastReceiver)
            Log.d(TAG, "onStopListening: BroadcastReceiver desregistrado")
        } catch (e: Exception) {
            Log.e(TAG, "onStopListening: Erro ao desregistrar BroadcastReceiver", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Limpa todas as mensagens pendentes do Handler
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "onDestroy: Handler limpo")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onClick() {
        super.onClick()
        Log.d(TAG, "onClick: Tile clicado")
        
        // Verifica se tem permissão para alterar taxa de atualização
        if (!hasPermissionToChangeRefreshRate()) {
            Log.w(TAG, "onClick: Sem permissão para alterar taxa de atualização")
            updateTileImmediately()
            return
        }
        
        // Cicla para próxima taxa de atualização
        val newState = cycleRefreshRate()
        Log.d(TAG, "onClick: Mudando para estado $newState")
        
        // Aplica a nova taxa de atualização
        applyRefreshRateChange(newState)
        
        // Força a atualização imediata do tile
        updateTileImmediately()
        
        // Garante que a atualização seja executada mesmo se o tile parar de escutar
        handler.postDelayed({
            try {
                updateTileImmediately()
                Log.d(TAG, "onClick: Atualização de segurança executada")
            } catch (e: Exception) {
                Log.e(TAG, "onClick: Erro na atualização de segurança", e)
            }
        }, 100) // 100ms de delay para garantir que a UI seja atualizada
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun updateTile() {
        val tile = qsTile ?: return
        updateTileContent(tile)
        tile.updateTile()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun updateTileImmediately() {
        val tile = qsTile ?: return
        updateTileContent(tile)
        tile.updateTile()
        
        // Log para debug
        val currentHzState = getCurrentHzState()
        Log.d(TAG, "updateTileImmediately: Estado atualizado para $currentHzState")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun updateTileContent(tile: Tile) {
        val currentRefreshRate = getCurrentRefreshRate()
        val currentHzState = getCurrentHzState()
        
        // Configurar o tile com os novos recursos do Android 16
        tile.label = getString(R.string.qs_tile_label)
        tile.contentDescription = getString(R.string.qs_tile_content_description)
        
        // Estado do tile sempre ativo (ligado) - mostra a bola indicadora
        tile.state = Tile.STATE_ACTIVE
        
        // Subtítulo com a taxa de atualização atual (aparece no lado esquerdo quando expandido)
        tile.subtitle = getHzStateString(currentHzState)
        
        // Ícone do tile
        tile.icon = Icon.createWithResource(this, R.drawable.ic_launcher_foreground)
    }

    private fun cycleRefreshRate(): Int {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val currentState = prefs.getInt(PREF_CURRENT_HZ, 0) // 0 = 60Hz
        val nextState = (currentState + 1) % 6 // Ciclo entre 0-5
        prefs.edit().putInt(PREF_CURRENT_HZ, nextState).apply()
        return nextState
    }

    private fun getCurrentHzState(): Int {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return prefs.getInt(PREF_CURRENT_HZ, 0)
    }

    private fun getHzStateString(state: Int): String {
        return when (state) {
            0 -> getString(R.string.qs_tile_60hz)
            1 -> getString(R.string.qs_tile_60_90hz)
            2 -> getString(R.string.qs_tile_60_120hz)
            3 -> getString(R.string.qs_tile_90hz)
            4 -> getString(R.string.qs_tile_90_120hz)
            5 -> getString(R.string.qs_tile_120hz)
            else -> getString(R.string.qs_tile_60hz)
        }
    }

    private fun getCurrentRefreshRate(): Float {
        return try {
            val displayManager = getSystemService(DISPLAY_SERVICE) as android.hardware.display.DisplayManager
            val display = displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY)
            display?.refreshRate ?: 60f
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter taxa de atualização atual", e)
            60f
        }
    }

    private fun hasPermissionToChangeRefreshRate(): Boolean {
        // Verifica se tem acesso root
        if (isDeviceRooted()) {
            Log.d(TAG, "hasPermissionToChangeRefreshRate: Acesso root disponível")
            return true
        }
        
        // Verifica se tem permissão Shizuku
        if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "hasPermissionToChangeRefreshRate: Permissão Shizuku disponível")
            return true
        }
        
        Log.w(TAG, "hasPermissionToChangeRefreshRate: Nenhuma permissão disponível")
        return false
    }

    private fun isDeviceRooted(): Boolean {
        // Verifica se o binário 'su' existe em locais comuns
        val paths = arrayOf(
            "/system/bin/", "/system/xbin/", "/sbin/", "/system/sd/xbin/",
            "/system/bin/failsafe/", "/data/local/xbin/", "/data/local/bin/", "/data/local/"
        )
        for (path in paths) {
            val file = java.io.File(path + "su")
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

    private fun applyRefreshRateChange(state: Int) {
        val (peakHz, minHz, isFixed) = getRefreshRateSettings(state)
        
        Log.d(TAG, "applyRefreshRateChange: Aplicando $peakHz Hz (min: $minHz, fixo: $isFixed)")
        
        // Salva o estado no SharedPreferences do widget para sincronização
        saveWidgetState(peakHz, isFixed)
        
        // Atualiza o widget
        updateWidget()
        
        // Inicia o serviço para aplicar a mudança
        val intent = Intent(this, HzChangerService::class.java).apply {
            putExtra("hz", peakHz)
            putExtra("min_hz", minHz)
            putExtra("is_fixed", isFixed)
            putExtra("is_from_tile", true)
        }
        
        try {
            startForegroundService(intent)
            Log.d(TAG, "applyRefreshRateChange: Serviço iniciado com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "applyRefreshRateChange: Erro ao iniciar serviço", e)
        }
    }

    private fun getRefreshRateSettings(state: Int): Triple<Int, Int, Boolean> {
        return when (state) {
            0 -> Triple(60, 60, true)   // 60 Hz fixo
            1 -> Triple(90, 60, false)   // 60-90 Hz variável
            2 -> Triple(120, 60, false)  // 60-120 Hz variável
            3 -> Triple(90, 90, true)    // 90 Hz fixo
            4 -> Triple(120, 90, false)  // 90-120 Hz variável
            5 -> Triple(120, 120, true)  // 120 Hz fixo
            else -> Triple(60, 60, true)  // Padrão: 60 Hz fixo
        }
    }

    private fun saveWidgetState(hz: Int, isFixed: Boolean) {
        val prefs = getSharedPreferences("com.marcossilqueira.hzchanger.HzChangerWidget", MODE_PRIVATE)
        prefs.edit()
            .putInt("current_hz", hz)
            .putBoolean("is_fixed_hz", isFixed)
            .apply()
        Log.d(TAG, "saveWidgetState: Estado salvo - Hz: $hz, Fixo: $isFixed")
    }

    private fun updateWidget() {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val widgetProvider = ComponentName(this, HzChangerWidget::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(widgetProvider)
            
            if (widgetIds.isNotEmpty()) {
                // Força atualização do widget
                val intent = Intent(this, HzChangerWidget::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                }
                sendBroadcast(intent)
                Log.d(TAG, "updateWidget: Widget atualizado com sucesso")
            } else {
                Log.d(TAG, "updateWidget: Nenhum widget encontrado")
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateWidget: Erro ao atualizar widget", e)
        }
    }

    private fun syncWithWidget() {
        try {
            // Lê o estado atual do widget
            val widgetPrefs = getSharedPreferences("com.marcossilqueira.hzchanger.HzChangerWidget", MODE_PRIVATE)
            val widgetHz = widgetPrefs.getInt("current_hz", -1)
            val widgetIsFixed = widgetPrefs.getBoolean("is_fixed_hz", false)
            
            if (widgetHz != -1) {
                // Converte Hz do widget para estado do tile
                val tileState = getTileStateFromWidgetHz(widgetHz, widgetIsFixed)
                
                // Atualiza o estado do tile
                val tilePrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                tilePrefs.edit().putInt(PREF_CURRENT_HZ, tileState).apply()
                
                Log.d(TAG, "syncWithWidget: Sincronizado com widget - Hz: $widgetHz, Fixo: $widgetIsFixed, Estado: $tileState")
            }
        } catch (e: Exception) {
            Log.e(TAG, "syncWithWidget: Erro ao sincronizar com widget", e)
        }
    }

    private fun getTileStateFromWidgetHz(hz: Int, isFixed: Boolean): Int {
        return when {
            hz == 60 && isFixed -> 0  // 60 Hz fixo
            hz == 90 && !isFixed -> 1  // 60-90 Hz variável
            hz == 120 && !isFixed -> 2  // 60-120 Hz variável
            hz == 90 && isFixed -> 3   // 90 Hz fixo
            hz == 120 && !isFixed -> 4  // 90-120 Hz variável
            hz == 120 && isFixed -> 5   // 120 Hz fixo
            else -> 0  // Padrão: 60 Hz fixo
        }
    }
}
