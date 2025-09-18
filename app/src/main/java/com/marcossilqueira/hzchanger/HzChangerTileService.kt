package com.marcossilqueira.hzchanger

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class HzChangerTileService : TileService() {

    companion object {
        private const val TAG = "HzChangerTileService"
        private const val PREFS_NAME = "HzChangerTileService"
        private const val PREF_CURRENT_STATE = "current_state"
        private const val PREF_CURRENT_HZ = "current_hz"
    }

    override fun onStartListening() {
        super.onStartListening()
        Log.d(TAG, "onStartListening: Tile começou a escutar")
        updateTile()
    }

    override fun onStopListening() {
        super.onStopListening()
        Log.d(TAG, "onStopListening: Tile parou de escutar")
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onClick() {
        super.onClick()
        Log.d(TAG, "onClick: Tile clicado")
        
        // Por enquanto, apenas alterna a taxa de atualização
        // A lógica completa será implementada depois
        cycleRefreshRate()
        updateTile()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun updateTile() {
        val tile = qsTile ?: return
        
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
        
        tile.updateTile()
    }

    private fun cycleRefreshRate() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val currentState = prefs.getInt(PREF_CURRENT_HZ, 0) // 0 = 60Hz
        val nextState = (currentState + 1) % 6 // Ciclo entre 0-5
        prefs.edit().putInt(PREF_CURRENT_HZ, nextState).apply()
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
}
