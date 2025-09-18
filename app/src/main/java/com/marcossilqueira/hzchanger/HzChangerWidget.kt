package com.marcossilqueira.hzchanger

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.service.quicksettings.Tile
import android.widget.RemoteViews
import android.util.Log

class HzChangerWidget : AppWidgetProvider() {

    companion object {
        private const val TAG = "HzChangerWidget"
        private const val PREFS_NAME = "com.marcossilqueira.hzchanger.HzChangerWidget"
        private const val PREF_IS_FIXED = "is_fixed_hz"
        private const val PREF_CURRENT_HZ = "current_hz"

        // Novas ações para o widget compacto
        private const val ACTION_CYCLE_HZ = "ACTION_CYCLE_HZ"
        private const val ACTION_TOGGLE_FIX = "ACTION_TOGGLE_FIX"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d(TAG, context.getString(R.string.log_widget_updating))

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isFixed = prefs.getBoolean(PREF_IS_FIXED, false)
        val currentHz = prefs.getInt(PREF_CURRENT_HZ, 60) // Carrega o Hz atual

        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, isFixed, currentHz)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val action = intent.action

        Log.d(TAG, context.getString(R.string.log_action_received, action))

        // Lógica de ações unificada
        when (action) {
            ACTION_CYCLE_HZ -> {
                var currentHz = prefs.getInt(PREF_CURRENT_HZ, 60)

                // Lógica de circulação
                currentHz = when (currentHz) {
                    60 -> 90
                    90 -> 120
                    else -> 60 // Volta para 60 se for 120 ou outro valor
                }

                prefs.edit().putInt(PREF_CURRENT_HZ, currentHz).apply()
                sendCommandToService(context, currentHz)
            }
            ACTION_TOGGLE_FIX -> {
                val isFixed = !prefs.getBoolean(PREF_IS_FIXED, false)
                prefs.edit().putBoolean(PREF_IS_FIXED, isFixed).apply()

                // Aplica a configuração atual com o novo estado de "fixo"
                val currentHz = prefs.getInt(PREF_CURRENT_HZ, 60)
                sendToggleFixCommand(context, isFixed, currentHz)
            }
        }

        // Após qualquer ação, atualize a aparência de todos os widgets
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisAppWidget = ComponentName(context.packageName, HzChangerWidget::class.java.name)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)
        val isFixed = prefs.getBoolean(PREF_IS_FIXED, false)
        val currentHz = prefs.getInt(PREF_CURRENT_HZ, 60)

        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, isFixed, currentHz)
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, isFixed: Boolean, currentHz: Int) {
        // Aponta para o novo layout compacto
        val views = RemoteViews(context.packageName, R.layout.widget_hz_changer_compact)

        // Configura os PendingIntents para os novos botões
        views.setOnClickPendingIntent(R.id.widget_button_cycle_hz, getPendingIntent(context, ACTION_CYCLE_HZ))
        views.setOnClickPendingIntent(R.id.widget_button_fix, getPendingIntent(context, ACTION_TOGGLE_FIX))

        // Atualiza o texto da frequência atual
        views.setTextViewText(R.id.widget_text_current_hz, context.getString(R.string.widget_hz_format, currentHz))

        // Atualiza o texto do botão de fixar
        views.setTextViewText(R.id.widget_button_fix, if (isFixed) context.getString(R.string.desfixar) else context.getString(R.string.fixar))

        // Atualiza o widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getPendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, HzChangerWidget::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun sendCommandToService(context: Context, hz: Int) {
        // Salva o estado no SharedPreferences do tile para sincronização
        saveTileState(context, hz)
        
        // Atualiza o tile do Quick Settings
        updateTile(context)
        
        val intent = Intent(context, HzChangerService::class.java).apply {
            putExtra("hz", hz)
            putExtra("is_from_widget", true)
        }
        context.startForegroundService(intent)
    }

    private fun sendToggleFixCommand(context: Context, isFixed: Boolean, hz: Int) {
        // Salva o estado no SharedPreferences do tile para sincronização
        saveTileState(context, hz, isFixed)
        
        // Atualiza o tile do Quick Settings
        updateTile(context)
        
        val intent = Intent(context, HzChangerService::class.java).apply {
            putExtra("toggle_fix", true)
            putExtra("is_fixed", isFixed)
            putExtra("hz", hz)
            putExtra("is_from_widget", true)
        }
        context.startForegroundService(intent)
    }

    private fun saveTileState(context: Context, hz: Int, isFixed: Boolean? = null) {
        val prefs = context.getSharedPreferences("HzChangerTileService", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        // Converte Hz para estado do tile
        val tileState = getTileStateFromHz(hz, isFixed ?: false)
        editor.putInt("current_hz", tileState)
        
        Log.d(TAG, "saveTileState: Estado do tile salvo - Hz: $hz, Estado: $tileState")
        editor.apply()
    }

    private fun getTileStateFromHz(hz: Int, isFixed: Boolean): Int {
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

    private fun updateTile(context: Context) {
        try {
            // Força atualização do tile do Quick Settings
            val intent = Intent(HzChangerTileService.ACTION_UPDATE_TILE)
            context.sendBroadcast(intent)
            Log.d(TAG, "updateTile: Tile do Quick Settings atualizado")
        } catch (e: Exception) {
            Log.e(TAG, "updateTile: Erro ao atualizar tile", e)
        }
    }
}