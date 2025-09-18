package com.marcossilqueira.hzchanger // Seu pacote

// Imports necessários (adicione os que faltarem)
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.switchmaterial.SwitchMaterial // Adicione se não estiver


class MainActivity : AppCompatActivity() {


    // Views (para fácil acesso) - Encontre-as no onCreate
    private lateinit var statusTextView: TextView
    private lateinit var buttonMax60Hz: Button
    private lateinit var buttonMax90Hz: Button
    private lateinit var buttonMax120Hz: Button
    private lateinit var buttonMin60Hz: Button
    private lateinit var buttonMin90Hz: Button
    private lateinit var buttonMin120Hz: Button
    private lateinit var currentRefreshRateTextView: TextView
    private lateinit var buttonInfo: android.widget.ImageButton
    private var selectedMaxHz: Int = 60 // taxa máxima selecionada
    private var selectedMinHz: Int = 60 // taxa mínima selecionada

    private fun updateCurrentRefreshRate() {
        try {
            val display = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            }
            val refreshRate = display?.refreshRate ?: 0f
            currentRefreshRateTextView.text = getString(R.string.taxa_atual_format, refreshRate.toInt())
            Log.d("MainActivity", "updateCurrentRefreshRate: Taxa atual atualizada para ${refreshRate.toInt()} Hz")
        } catch (e: Exception) {
            Log.e("MainActivity", "updateCurrentRefreshRate: Erro ao atualizar taxa atual", e)
        }
    }

    private fun syncButtonStates() {
        try {
            // Lê o estado atual do widget
            val widgetPrefs = getSharedPreferences("com.marcossilqueira.hzchanger.HzChangerWidget", MODE_PRIVATE)
            val widgetIsFixed = widgetPrefs.getBoolean("is_fixed_hz", false)
            val widgetHz = widgetPrefs.getInt("current_hz", 60)
            
            // Atualiza o estado interno
            if (widgetIsFixed) {
                selectedMaxHz = widgetHz
                selectedMinHz = widgetHz
            } else {
                // Para taxa variável, assume configurações padrão baseadas no Hz
                when (widgetHz) {
                    90 -> { selectedMaxHz = 90; selectedMinHz = 60 }
                    120 -> { selectedMaxHz = 120; selectedMinHz = 60 }
                    else -> { selectedMaxHz = 60; selectedMinHz = 60 }
                }
            }
            
            // Atualiza os botões na UI
            updateButtonStates()
            
            Log.d("MainActivity", "syncButtonStates: Botões sincronizados - Max: $selectedMaxHz, Min: $selectedMinHz")
        } catch (e: Exception) {
            Log.e("MainActivity", "syncButtonStates: Erro ao sincronizar botões", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        enableEdgeToEdge() // Se você tiver esta linha
        setContentView(R.layout.activity_main)

        // Encontrar as Views pelo ID
        statusTextView = findViewById(R.id.text_view_status)
        buttonMax60Hz = findViewById(R.id.button_max_60hz)
        buttonMax90Hz = findViewById(R.id.button_max_90hz)
        buttonMax120Hz = findViewById(R.id.button_max_120hz)
        buttonMin60Hz = findViewById(R.id.button_min_60hz)
        buttonMin90Hz = findViewById(R.id.button_min_90hz)
        buttonMin120Hz = findViewById(R.id.button_min_120hz)
        currentRefreshRateTextView = findViewById(R.id.text_view_current_refresh_rate)
        buttonInfo = findViewById(R.id.button_info)
        updateCurrentRefreshRate()

        // Configurar listeners para taxa máxima
        buttonMax60Hz.setOnClickListener { selectMaxRate(60) }
        buttonMax90Hz.setOnClickListener { selectMaxRate(90) }
        buttonMax120Hz.setOnClickListener { selectMaxRate(120) }

        // Configurar listeners para taxa mínima
        buttonMin60Hz.setOnClickListener { selectMinRate(60) }
        buttonMin90Hz.setOnClickListener { selectMinRate(90) }
        buttonMin120Hz.setOnClickListener { selectMinRate(120) }

        // Configurar listener para botão de informação
        buttonInfo.setOnClickListener { showInfoDialog() }

        // Inicialmente desabilitar botões até checar permissão
        enableUiComponents(false)


        // --- Ajuste para compatibilidade com EdgeToEdge ---
        // Se você tem `enableEdgeToEdge()`, o código original do ViewCompat pode
        // precisar ser ajustado ou você pode precisar lidar com padding manualmente
        // dentro do listener ou removê-lo se não for essencial agora.
        // Por simplicidade, vamos comentar o ViewCompat original por enquanto.
        /*
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
         */

        // Iniciar verificação de permissão root
        checkRootAccess()

        // Atualizar taxa atual quando o app é aberto
        updateCurrentRefreshRate()
        
        // Sincronizar estado dos botões quando o app é aberto
        syncButtonStates()
        
        // Atualizar widget quando o app é aberto
        updateWidget()

        // ----- CONFIGURAR LISTENERS PARA OS BOTÕES (FAREMOS DEPOIS) ----
        // button30Hz.setOnClickListener { ... }
        // button60Hz.setOnClickListener { ... }
        // ...
        // switchFixedHz.setOnCheckedChangeListener { _, isChecked -> ... }
    }

    override fun onPause() {
        super.onPause()
        // Atualizar taxa atual quando o app é pausado
        updateCurrentRefreshRate()
        // Sincronizar estado dos botões quando o app é pausado
        syncButtonStates()
        // Atualizar widget quando o app é pausado
        updateWidget()
    }

    override fun onResume() {
        super.onResume()
        // Atualizar taxa atual quando o app é retomado
        updateCurrentRefreshRate()
        // Sincronizar estado dos botões quando o app é retomado
        syncButtonStates()
        // Atualizar widget quando o app é retomado
        updateWidget()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Atualizar widget quando o app é fechado
        updateWidget()
    }

    private fun checkRootAccess() {
        Log.d("HzChangerApp", "Verificando acesso root...")
        if (isDeviceRooted()) {
            updateStatus(getString(R.string.acesso_root_detectado))
            enableUiComponents(true)
        } else {
            updateStatus(getString(R.string.acesso_root_nao_disponivel))
            enableUiComponents(false)
        }
    }

    // Função para atualizar o TextView de status
    private fun updateStatus(message: String) {
        runOnUiThread { // Garante que a UI seja atualizada na thread principal
            statusTextView.text = message
        }
        // Log.d("HzChangerStatus", message) // Opcional: Log para debug
    }

    // Função para habilitar/desabilitar os controles da UI
    private fun enableUiComponents(enabled: Boolean) {
        runOnUiThread {
            buttonMax60Hz.isEnabled = enabled
            buttonMax90Hz.isEnabled = enabled
            buttonMax120Hz.isEnabled = enabled
            buttonMin60Hz.isEnabled = enabled
            buttonMin90Hz.isEnabled = enabled
            buttonMin120Hz.isEnabled = enabled
        }
    }
    private fun isDeviceRooted(): Boolean {
        // 1. Verifica se o binário 'su' existe em locais comuns
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
        // 2. Tenta executar o comando 'su'
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo root"))
            val result = process.inputStream.bufferedReader().readText()
            process.destroy()
            result.contains("root")
        } catch (e: Exception) {
            false
        }
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
                Log.d("MainActivity", "updateWidget: Widget atualizado com sucesso")
            } else {
                Log.d("MainActivity", "updateWidget: Nenhum widget encontrado")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "updateWidget: Erro ao atualizar widget", e)
        }
    }


    private fun updateTile() {
        try {
            // Força atualização do tile do Quick Settings
            val intent = Intent(HzChangerTileService.ACTION_UPDATE_TILE)
            sendBroadcast(intent)
            Log.d("MainActivity", "updateTile: Tile do Quick Settings atualizado")
        } catch (e: Exception) {
            Log.e("MainActivity", "updateTile: Erro ao atualizar tile", e)
        }
    }

    private fun selectMaxRate(hz: Int) {
        selectedMaxHz = hz
        updateButtonStates()
        applyRefreshRate()
        Log.d("MainActivity", "selectMaxRate: Taxa máxima selecionada: $hz Hz")
    }

    private fun selectMinRate(hz: Int) {
        selectedMinHz = hz
        updateButtonStates()
        applyRefreshRate()
        Log.d("MainActivity", "selectMinRate: Taxa mínima selecionada: $hz Hz")
    }

    private fun updateButtonStates() {
        runOnUiThread {
            // Atualizar botões de taxa máxima
            buttonMax60Hz.isSelected = (selectedMaxHz == 60)
            buttonMax90Hz.isSelected = (selectedMaxHz == 90)
            buttonMax120Hz.isSelected = (selectedMaxHz == 120)

            // Atualizar botões de taxa mínima
            buttonMin60Hz.isSelected = (selectedMinHz == 60)
            buttonMin90Hz.isSelected = (selectedMinHz == 90)
            buttonMin120Hz.isSelected = (selectedMinHz == 120)
        }
    }

    private fun applyRefreshRate() {
        val isFixed = (selectedMaxHz == selectedMinHz)
        val peakHz = selectedMaxHz
        val minHz = selectedMinHz

        Log.d("MainActivity", "applyRefreshRate: Aplicando taxa - Max: $peakHz Hz, Min: $minHz Hz, Fixo: $isFixed")

        val commands = if (isFixed) {
            // Fixo: min e peak iguais
            arrayOf(
                "settings put system peak_refresh_rate $peakHz.0",
                "settings put system min_refresh_rate $peakHz.0"
            )
        } else {
            // Variável: min e peak diferentes
            arrayOf(
                "settings put system peak_refresh_rate $peakHz.0",
                "settings put system min_refresh_rate $minHz.0"
            )
        }

        try {
            if (isDeviceRooted()) {
                for (cmd in commands) {
                    val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
                    process.waitFor()
                }
                val sufixo = if (isFixed) getString(R.string.taxa_fixa_sufixo) else ""
                val message = if (isFixed) {
                    getString(R.string.taxa_definida_format, peakHz, sufixo)
                } else {
                    getString(R.string.taxa_configurada_format, minHz, peakHz)
                }
                updateStatus(message)
            } else {
                updateStatus(getString(R.string.sem_permissao_alterar_taxa))
            }
        } catch (e: Exception) {
            updateStatus(getString(R.string.erro_definir_taxa, e.message ?: "Erro desconhecido"))
        }

        updateCurrentRefreshRate()

        // Salvar estado no SharedPreferences para sincronização
        saveAppState(peakHz, minHz, isFixed)

        // Atualizar widget após alterar taxa
        updateWidget()

        // Atualizar tile do Quick Settings
        updateTile()
    }

    private fun saveAppState(peakHz: Int, minHz: Int, isFixed: Boolean) {
        try {
            // Salva no SharedPreferences do widget
            val widgetPrefs = getSharedPreferences("com.marcossilqueira.hzchanger.HzChangerWidget", MODE_PRIVATE)
            widgetPrefs.edit()
                .putInt("current_hz", peakHz)
                .putBoolean("is_fixed_hz", isFixed)
                .apply()

            // Salva no SharedPreferences do tile
            val tilePrefs = getSharedPreferences("HzChangerTileService", MODE_PRIVATE)
            val tileState = getTileStateFromAppHz(peakHz, minHz, isFixed)
            tilePrefs.edit()
                .putInt("current_hz", tileState)
                .apply()

            Log.d("MainActivity", "saveAppState: Estado salvo - Peak: $peakHz Hz, Min: $minHz Hz, Fixo: $isFixed, Estado Tile: $tileState")
        } catch (e: Exception) {
            Log.e("MainActivity", "saveAppState: Erro ao salvar estado", e)
        }
    }

    private fun getTileStateFromAppHz(peakHz: Int, minHz: Int, isFixed: Boolean): Int {
        return when {
            peakHz == 60 && isFixed -> 0  // 60 Hz fixo
            peakHz == 90 && !isFixed && minHz == 60 -> 1  // 60-90 Hz variável
            peakHz == 120 && !isFixed && minHz == 60 -> 2  // 60-120 Hz variável
            peakHz == 90 && isFixed -> 3   // 90 Hz fixo
            peakHz == 120 && !isFixed && minHz == 90 -> 4  // 90-120 Hz variável
            peakHz == 120 && isFixed -> 5   // 120 Hz fixo
            else -> 0  // Padrão: 60 Hz fixo
        }
    }

    private fun showInfoDialog() {
        // Inflar o layout customizado
        val dialogView = layoutInflater.inflate(R.layout.custom_info_dialog, null)
        
        // Criar o diálogo
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        // Configurar o botão OK
        val okButton = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.button_ok)
        okButton.setOnClickListener {
            dialog.dismiss()
        }
        
        // Mostrar o diálogo
        dialog.show()
    }
}