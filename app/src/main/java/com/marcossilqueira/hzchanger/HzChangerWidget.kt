package com.marcossilqueira.hzchanger

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.util.Log

class HzChangerWidget : AppWidgetProvider() {

    companion object {
        private const val TAG = "HzChangerWidget"
        private const val PREFS_NAME = "com.marcossilqueira.hzchanger.HzChangerWidget"
        private const val PREF_IS_FIXED = "is_fixed_hz"
        private const val PREF_CURRENT_HZ = "current_hz"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d(TAG, "onUpdate: Atualizando widgets")

        // Carrega as preferências
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isFixed = prefs.getBoolean(PREF_IS_FIXED, false)

        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, isFixed)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val action = intent.action

        Log.d(TAG, "onReceive: Ação recebida: $action")

        when (action) {
            "ACTION_SET_60HZ" -> {
                prefs.edit().putInt(PREF_CURRENT_HZ, 60).apply()
                sendCommandToService(context, 60)
            }
            "ACTION_SET_90HZ" -> {
                prefs.edit().putInt(PREF_CURRENT_HZ, 90).apply()
                sendCommandToService(context, 90)
            }
            "ACTION_SET_120HZ" -> {
                prefs.edit().putInt(PREF_CURRENT_HZ, 120).apply()
                sendCommandToService(context, 120)
            }
            "ACTION_TOGGLE_FIX" -> {
                val isFixed = !prefs.getBoolean(PREF_IS_FIXED, false)
                prefs.edit().putBoolean(PREF_IS_FIXED, isFixed).apply()

                // Atualiza o widget para refletir a mudança
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val thisAppWidget = ComponentName(context.packageName, HzChangerWidget::class.java.name)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget)

                for (appWidgetId in appWidgetIds) {
                    updateWidget(context, appWidgetManager, appWidgetId, isFixed)
                }

                // Aplica a configuração atual
                val currentHz = prefs.getInt(PREF_CURRENT_HZ, 60)
                sendToggleFixCommand(context, isFixed, currentHz)
            }
        }
    }

    private fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, isFixed: Boolean) {
        val views = RemoteViews(context.packageName, R.layout.widget_hz_changer)

        // Configura os PendingIntents para os botões
        views.setOnClickPendingIntent(R.id.widget_button_60hz, getPendingIntent(context, "ACTION_SET_60HZ"))
        views.setOnClickPendingIntent(R.id.widget_button_90hz, getPendingIntent(context, "ACTION_SET_90HZ"))
        views.setOnClickPendingIntent(R.id.widget_button_120hz, getPendingIntent(context, "ACTION_SET_120HZ"))
        views.setOnClickPendingIntent(R.id.widget_button_fix, getPendingIntent(context, "ACTION_TOGGLE_FIX"))

        // Atualiza o texto do botão de fixar
        views.setTextViewText(R.id.widget_button_fix, if (isFixed) "Desfixar taxa" else "Fixar taxa")

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
        val intent = Intent(context, HzChangerService::class.java).apply {
            putExtra("hz", hz)
            putExtra("is_from_widget", true)
        }
        context.startForegroundService(intent)
    }

    private fun sendToggleFixCommand(context: Context, isFixed: Boolean, hz: Int) {
        val intent = Intent(context, HzChangerService::class.java).apply {
            putExtra("toggle_fix", true)
            putExtra("is_fixed", isFixed)
            putExtra("hz", hz)
            putExtra("is_from_widget", true)
        }
        context.startForegroundService(intent)
    }
}