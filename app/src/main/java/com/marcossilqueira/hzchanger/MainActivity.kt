package com.marcossilqueira.hzchanger // Seu pacote

// Imports necessários (adicione os que faltarem)
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

    // Listener para o resultado da permissão Shizuku
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                updateStatus("Permissão Shizuku concedida!")
                // Habilitar funcionalidades que dependem do Shizuku
                enableUiComponents(true)
            } else {
                updateStatus("Permissão Shizuku negada.")
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
                updateStatus("Permissão Shizuku concedida! (Listener)")
                enableUiComponents(true)
            } else {
                updateStatus("Permissão Shizuku negada. (Listener)")
                enableUiComponents(false)
            }
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

        // ----- CONFIGURAR LISTENERS PARA OS BOTÕES (FAREMOS DEPOIS) ----
        // button30Hz.setOnClickListener { ... }
        // button60Hz.setOnClickListener { ... }
        // ...
        // switchFixedHz.setOnCheckedChangeListener { _, isChecked -> ... }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remover listeners para evitar memory leaks
        Shizuku.removeBinderReceivedListener(shizukuBinderListener)
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
    }

    private fun checkShizukuPermission() {
        try { // <--- Adiciona o try aqui
            // Verificar se o Shizuku está instalado e rodando
            if (Shizuku.isPreV11() || Shizuku.getVersion() < 10) {
                updateStatus("Shizuku desatualizado ou não encontrado.")
                checkRootAccess() // Tentar checar Root como alternativa
                return // Sai da função aqui se Shizuku não está ok
            }

            // Shizuku parece estar disponível, verificar permissão
            when (Shizuku.checkSelfPermission()) {
                PackageManager.PERMISSION_GRANTED -> {
                    // Permissão já concedida!
                    updateStatus("Shizuku pronto!")
                    enableUiComponents(true) // Habilitar botões e switch
                }
                PackageManager.PERMISSION_DENIED -> {
                    // Permissão negada
                    if (Shizuku.shouldShowRequestPermissionRationale()) {
                        // O usuário negou antes, talvez mostrar um diálogo explicando por que precisa
                        updateStatus("Permissão Shizuku necessária. Conceda no app Shizuku ou reinstale.") // Mensagem mais clara
                        // Poderíamos ter um botão para tentar pedir de novo, mas Shizuku recomenda que o usuário vá ao app Shizuku
                        enableUiComponents(false) // Manter desabilitado
                        // Não vamos pedir permissão automaticamente aqui se já foi negada antes.
                    } else {
                        // Primeira vez pedindo ou usuário marcou "não perguntar novamente" (ou Shizuku não permite pedir de novo)
                        updateStatus("Solicitando permissão Shizuku...")
                        requestShizukuPermission() // Tenta solicitar
                        enableUiComponents(false) // Manter desabilitado
                    }
                }
                else -> {
                    // Outro estado inesperado?
                    updateStatus("Status de permissão Shizuku desconhecido.")
                    enableUiComponents(false)
                    checkRootAccess() // Tentar Root como fallback
                }
            }

        } catch (e: Exception) { // <--- Adiciona o catch aqui
            // Captura QUALQUER erro que ocorrer dentro do try
            Log.e("HzChangerApp", "Erro ao verificar/pedir permissão Shizuku", e) // Loga o erro completo no Logcat
            updateStatus("Erro ao verificar Shizuku: ${e.message}") // Mostra uma mensagem básica na tela
            enableUiComponents(false) // Desabilita UI por segurança
            checkRootAccess() // Tenta verificar Root como fallback mesmo se deu erro no Shizuku
        }
    }

// A função requestShizukuPermission e o resto da classe continuam iguais...

    // Verifique se a função checkRootAccess ainda está como placeholder:
    private fun checkRootAccess() {
        Log.d("HzChangerApp", "Verificando acesso root...")
        if (isDeviceRooted()) {
            updateStatus("Acesso root detectado!")
            enableUiComponents(true)
        } else {
            updateStatus("Acesso root NÃO disponível neste dispositivo.")
            enableUiComponents(false)
        }
    }

    private fun requestShizukuPermission() {
        if (Shizuku.isPreV11() || Shizuku.getVersion() < 10) {
            updateStatus("Shizuku não disponível para solicitar permissão.")
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
}