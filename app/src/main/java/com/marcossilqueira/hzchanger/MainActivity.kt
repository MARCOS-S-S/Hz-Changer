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

// Imports do Shizuku
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider // Pode não ser necessário importar diretamente aqui
//import rikka.shizuku.ShizukuRemoteContext // Para obter o contexto do Shizuku

class MainActivity : AppCompatActivity() {

    // Constante para o código de requisição de permissão
    private val SHIZUKU_PERMISSION_REQUEST_CODE = 1001

    // Views (para fácil acesso) - Encontre-as no onCreate
    private lateinit var statusTextView: TextView
    private lateinit var switchFixedHz: SwitchMaterial
    private lateinit var button30Hz: Button
    private lateinit var button60Hz: Button
    private lateinit var button90Hz: Button
    private lateinit var button120Hz: Button
    private lateinit var currentRefreshRateTextView: TextView
    private var isFixedHz: Boolean = false
    private var selectedHz: Int = 60 // valor padrão inicial

    // Listener para o resultado da permissão Shizuku
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                updateStatus(getString(R.string.permissao_shizuku_concedida))
                // Habilitar funcionalidades que dependem do Shizuku
                enableUiComponents(true)
            } else {
                updateStatus(getString(R.string.permissao_shizuku_negada))
                // Desabilitar funcionalidades ou mostrar aviso
                enableUiComponents(false) // Ou apenas as que dependem de permissão
            }
        }

    // Listener para o estado do Shizuku (se está ativo ou não)
    private val shizukuBinderListener = Shizuku.OnBinderReceivedListener {
        // Chamado quando o Shizuku conecta (ou já estava conectado)
        checkShizukuPermission()
    }

    // Listener para o resultado da requisição de permissão (caso use o método antigo)
    // Usaremos o ActivityResultContracts acima, mas é bom saber que existe este
    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                updateStatus(getString(R.string.permissao_shizuku_concedida_listener))
                enableUiComponents(true)
            } else {
                updateStatus(getString(R.string.permissao_shizuku_negada_listener))
                enableUiComponents(false)
            }
        }
    }
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

    private fun syncSwitchState() {
        try {
            // Lê o estado atual do widget
            val widgetPrefs = getSharedPreferences("com.marcossilqueira.hzchanger.HzChangerWidget", MODE_PRIVATE)
            val widgetIsFixed = widgetPrefs.getBoolean("is_fixed_hz", false)
            val widgetHz = widgetPrefs.getInt("current_hz", 60)
            
            // Atualiza o estado interno
            isFixedHz = widgetIsFixed
            selectedHz = widgetHz
            
            // Atualiza o switch na UI
            runOnUiThread {
                switchFixedHz.isChecked = widgetIsFixed
            }
            
            Log.d("MainActivity", "syncSwitchState: Switch sincronizado - Hz: $widgetHz, Fixo: $widgetIsFixed")
        } catch (e: Exception) {
            Log.e("MainActivity", "syncSwitchState: Erro ao sincronizar switch", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        enableEdgeToEdge() // Se você tiver esta linha
        setContentView(R.layout.activity_main)

        // Encontrar as Views pelo ID
        statusTextView = findViewById(R.id.text_view_status)
        switchFixedHz = findViewById(R.id.switch_fixed_hz)
        button30Hz = findViewById(R.id.button_30hz)
        button60Hz = findViewById(R.id.button_60hz)
        button90Hz = findViewById(R.id.button_90hz)
        button120Hz = findViewById(R.id.button_120hz)
        currentRefreshRateTextView = findViewById(R.id.text_view_current_refresh_rate)
        updateCurrentRefreshRate()

        button30Hz.setOnClickListener {
            selectedHz = 30
            setRefreshRate(selectedHz)
        }
        button60Hz.setOnClickListener {
            selectedHz = 60
            setRefreshRate(selectedHz)
        }
        button90Hz.setOnClickListener {
            selectedHz = 90
            setRefreshRate(selectedHz)
        }
        button120Hz.setOnClickListener {
            selectedHz = 120
            setRefreshRate(selectedHz)
        }

        switchFixedHz.setOnCheckedChangeListener { _, isChecked ->
            isFixedHz = isChecked
            setRefreshRate(selectedHz) // Aplica imediatamente a nova configuração
        }

        // Inicialmente desabilitar botões até checar permissão
        enableUiComponents(false)

        // Adicionar listeners do Shizuku
        Shizuku.addBinderReceivedListener(shizukuBinderListener)
        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)


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

        // Iniciar verificação de permissão
        checkShizukuPermission()

        // Atualizar taxa atual quando o app é aberto
        updateCurrentRefreshRate()
        
        // Sincronizar estado do switch quando o app é aberto
        syncSwitchState()
        
        // Atualizar widget quando o app é aberto
        updateWidget()

        // ----- CONFIGURAR LISTENERS PARA OS BOTÕES (FAREMOS DEPOIS) ----
        // button30Hz.setOnClickListener { ... }
        // button60Hz.setOnClickListener { ... }
        // ...
        // switchFixedHz.setOnCheckedChangeListener { _, isChecked -> ... }
    }
    private fun setRefreshRate(hz: Int) {
        val commands = if (isFixedHz) {
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
                }
                val sufixo = if (isFixedHz) getString(R.string.taxa_fixa_sufixo) else ""
                updateStatus(getString(R.string.taxa_definida_format, hz, sufixo))
            } else {
                updateStatus(getString(R.string.sem_permissao_alterar_taxa))
            }
        } catch (e: Exception) {
            updateStatus(getString(R.string.erro_definir_taxa, e.message ?: "Erro desconhecido"))
        }
        updateCurrentRefreshRate()
        
        // Salvar estado no SharedPreferences para sincronização
        saveAppState(hz)
        
        // Atualizar widget após alterar taxa
        updateWidget()
        
        // Atualizar tile do Quick Settings
        updateTile()
    }

    override fun onPause() {
        super.onPause()
        // Atualizar taxa atual quando o app é pausado
        updateCurrentRefreshRate()
        // Sincronizar estado do switch quando o app é pausado
        syncSwitchState()
        // Atualizar widget quando o app é pausado
        updateWidget()
    }

    override fun onResume() {
        super.onResume()
        // Atualizar taxa atual quando o app é retomado
        updateCurrentRefreshRate()
        // Sincronizar estado do switch quando o app é retomado
        syncSwitchState()
        // Atualizar widget quando o app é retomado
        updateWidget()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Atualizar widget quando o app é fechado
        updateWidget()
        
        // Remover listeners para evitar memory leaks
        Shizuku.removeBinderReceivedListener(shizukuBinderListener)
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
    }

    private fun checkShizukuPermission() {
        try { // <--- Adiciona o try aqui
            // Verificar se o Shizuku está instalado e rodando
            if (Shizuku.isPreV11() || Shizuku.getVersion() < 10) {
                updateStatus(getString(R.string.shizuku_desatualizado))
                checkRootAccess() // Tentar checar Root como alternativa
                return // Sai da função aqui se Shizuku não está ok
            }

            // Shizuku parece estar disponível, verificar permissão
            when (Shizuku.checkSelfPermission()) {
                PackageManager.PERMISSION_GRANTED -> {
                    // Permissão já concedida!
                    updateStatus(getString(R.string.shizuku_pronto))
                    enableUiComponents(true) // Habilitar botões e switch
                }
                PackageManager.PERMISSION_DENIED -> {
                    // Permissão negada
                    if (Shizuku.shouldShowRequestPermissionRationale()) {
                        // O usuário negou antes, talvez mostrar um diálogo explicando por que precisa
                        updateStatus(getString(R.string.permissao_shizuku_necessaria)) // Mensagem mais clara
                        // Poderíamos ter um botão para tentar pedir de novo, mas Shizuku recomenda que o usuário vá ao app Shizuku
                        enableUiComponents(false) // Manter desabilitado
                        // Não vamos pedir permissão automaticamente aqui se já foi negada antes.
                    } else {
                        // Primeira vez pedindo ou usuário marcou "não perguntar novamente" (ou Shizuku não permite pedir de novo)
                        updateStatus(getString(R.string.solicitando_permissao_shizuku))
                        requestShizukuPermission() // Tenta solicitar
                        enableUiComponents(false) // Manter desabilitado
                    }
                }
                else -> {
                    // Outro estado inesperado?
                    updateStatus(getString(R.string.status_permissao_shizuku_desconhecido))
                    enableUiComponents(false)
                    checkRootAccess() // Tentar Root como fallback
                }
            }

        } catch (e: Exception) { // <--- Adiciona o catch aqui
            // Captura QUALQUER erro que ocorrer dentro do try
            Log.e("HzChangerApp", "Erro ao verificar/pedir permissão Shizuku", e) // Loga o erro completo no Logcat
            updateStatus(getString(R.string.erro_verificar_shizuku, e.message ?: "Erro desconhecido")) // Mostra uma mensagem básica na tela
            enableUiComponents(false) // Desabilita UI por segurança
            checkRootAccess() // Tenta verificar Root como fallback mesmo se deu erro no Shizuku
        }
    }

// A função requestShizukuPermission e o resto da classe continuam iguais...

    // Verifique se a função checkRootAccess ainda está como placeholder:
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

    private fun requestShizukuPermission() {
        if (Shizuku.isPreV11() || Shizuku.getVersion() < 10) {
            updateStatus(getString(R.string.shizuku_nao_disponivel_solicitar))
            return
        }

        // Pedir permissão usando o método da API Shizuku
        // Usando o ActivityResultLauncher (método moderno):
        // Nota: A API Shizuku pode ter sua própria forma de pedir permissão,
        // vamos usar a recomendada pela documentação deles se houver.
        // Por agora, vamos usar o método padrão deles que usa o listener:
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
        }

        // Alternativa usando ActivityResultContracts (geralmente para permissões Android padrão)
        // if (ContextCompat.checkSelfPermission(this, ShizukuApiConstants.PERMISSION) != PackageManager.PERMISSION_GRANTED) {
        //    requestPermissionLauncher.launch(ShizukuApiConstants.PERMISSION)
        //}
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
            switchFixedHz.isEnabled = enabled
            button30Hz.isEnabled = enabled
            button60Hz.isEnabled = enabled
            button90Hz.isEnabled = enabled
            button120Hz.isEnabled = enabled
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

    private fun saveAppState(hz: Int) {
        try {
            // Salva no SharedPreferences do widget
            val widgetPrefs = getSharedPreferences("com.marcossilqueira.hzchanger.HzChangerWidget", MODE_PRIVATE)
            widgetPrefs.edit()
                .putInt("current_hz", hz)
                .putBoolean("is_fixed_hz", isFixedHz)
                .apply()
            
            // Salva no SharedPreferences do tile
            val tilePrefs = getSharedPreferences("HzChangerTileService", MODE_PRIVATE)
            val tileState = getTileStateFromAppHz(hz, isFixedHz)
            tilePrefs.edit()
                .putInt("current_hz", tileState)
                .apply()
            
            Log.d("MainActivity", "saveAppState: Estado salvo - Hz: $hz, Fixo: $isFixedHz, Estado Tile: $tileState")
        } catch (e: Exception) {
            Log.e("MainActivity", "saveAppState: Erro ao salvar estado", e)
        }
    }

    private fun getTileStateFromAppHz(hz: Int, isFixed: Boolean): Int {
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
}