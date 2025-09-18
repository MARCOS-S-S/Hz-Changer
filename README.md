# Hz Changer

Um aplicativo Android que permite alterar a taxa de atualização (refresh rate) da tela do seu dispositivo, oferecendo opções de controle entre 60Hz, 90Hz e 120Hz.

## 📱 Características

- **Interface Moderna**: Botões com estilo outline que se preenchem quando selecionados
- **Controle Granular**: Defina taxa mínima e máxima independentemente
- **Widget Rápido**: Controle direto da tela inicial
- **Quick Settings Tile**: Acesso rápido pelas configurações rápidas
- **Multilíngue**: Suporte a Português e Inglês
- **Material Design**: Interface seguindo as diretrizes do Material Design 3

## ⚙️ Requisitos

- **Android 8.0+** (API 26+)
- **Permissões Root** (obrigatório)
- **Dispositivo com suporte a múltiplas taxas de atualização**

## 🚀 Instalação

1. Baixe o APK da [página de releases](../../releases)
2. Habilite "Fontes desconhecidas" nas configurações do Android
3. Instale o APK
4. Certifique-se de que seu dispositivo está com root ativo

## 📖 Como Usar

### Interface Principal

1. **Taxa Mínima**: Selecione a menor taxa de atualização desejada (60Hz, 90Hz, 120Hz)
2. **Taxa Máxima**: Selecione a maior taxa de atualização desejada (60Hz, 90Hz, 120Hz)
3. **Aplicação Automática**: As configurações são aplicadas automaticamente ao selecionar

### Regras Importantes

- ⚠️ **A taxa mínima deve ser menor ou igual à taxa máxima**
- ⚠️ **Taxas fixas**: Quando mínima = máxima, a taxa fica fixa
- ⚠️ **Taxas variáveis**: Quando mínima < máxima, a taxa varia dinamicamente

### Widget

- Adicione o widget "Hz Changer" à tela inicial
- Toque para alternar entre diferentes configurações
- Visualização clara da taxa atual

### Quick Settings

- Adicione o tile "Hz Changer" nas configurações rápidas
- Controle rápido sem abrir o app
- Indicador visual do estado atual

## 🔧 Configurações Disponíveis

| Taxa Mínima | Taxa Máxima | Resultado |
|-------------|-------------|-----------|
| 60Hz | 60Hz | 60Hz fixo |
| 60Hz | 90Hz | 60-90Hz variável |
| 60Hz | 120Hz | 60-120Hz variável |
| 90Hz | 90Hz | 90Hz fixo |
| 90Hz | 120Hz | 90-120Hz variável |
| 120Hz | 120Hz | 120Hz fixo |

## 🛠️ Desenvolvimento

### Estrutura do Projeto

```
app/
├── src/main/
│   ├── java/com/marcossilqueira/hzchanger/
│   │   ├── MainActivity.kt          # Interface principal
│   │   ├── HzChangerWidget.kt       # Widget da tela inicial
│   │   ├── HzChangerTileService.kt  # Tile do Quick Settings
│   │   └── HzChangerService.kt      # Serviço de alteração de taxa
│   ├── res/
│   │   ├── layout/                  # Layouts da interface
│   │   ├── drawable/                # Ícones e recursos visuais
│   │   ├── values/                  # Strings e temas
│   │   └── xml/                     # Configurações de widget e tile
│   └── AndroidManifest.xml
```

### Tecnologias Utilizadas

- **Kotlin** - Linguagem principal
- **Material Design 3** - Componentes de interface
- **ConstraintLayout** - Layout responsivo
- **SharedPreferences** - Armazenamento de configurações
- **App Widget Provider** - Widget da tela inicial
- **Tile Service** - Quick Settings tile

### Comandos Root Utilizados

```bash
# Definir taxa máxima
settings put system peak_refresh_rate [TAXA].0

# Definir taxa mínima  
settings put system min_refresh_rate [TAXA].0
```

## 🔍 Solução de Problemas

### App não funciona
- ✅ Verifique se o dispositivo está com root ativo
- ✅ Certifique-se de que o app tem permissões root
- ✅ Reinicie o dispositivo se necessário

### Botões desabilitados
- ❌ Indica que não há permissões root
- 🔧 Use um gerenciador de root (Magisk, SuperSU, etc.)
- 🔧 Conceda permissões root ao app

### Taxa não muda
- ⚠️ Verifique se o dispositivo suporta a taxa desejada
- ⚠️ Alguns dispositivos têm limitações de hardware
- ⚠️ Teste com diferentes combinações de taxa

## 📝 Notas da Versão

### v1.0 - Interface Aprimorada
- Redesign completo dos botões com estilo outline
- Botão de informação com diálogo customizado
- Remoção da dependência do Shizuku
- Foco exclusivo em permissões root
- Melhorias visuais e de usabilidade

## ⚠️ Avisos Importantes

- **Root Necessário**: Este app requer permissões root para funcionar
- **Riscos**: Alterar configurações do sistema pode causar instabilidade
- **Backup**: Sempre faça backup antes de usar apps com root
- **Compatibilidade**: Nem todos os dispositivos suportam todas as taxas
- **Shizuku**: Suporte ao Shizuku foi removido nesta versão

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

## 🤝 Contribuições

Contribuições são bem-vindas! Por favor:

1. Faça um fork do projeto
2. Crie uma branch para sua feature (`git checkout -b feature/AmazingFeature`)
3. Commit suas mudanças (`git commit -m 'Add some AmazingFeature'`)
4. Push para a branch (`git push origin feature/AmazingFeature`)
5. Abra um Pull Request

## 📞 Suporte

Se você encontrar problemas ou tiver sugestões:

- 📧 Abra uma [issue](../../issues) no GitHub
- 💬 Descreva o problema detalhadamente
- 📱 Inclua informações do dispositivo e versão do Android
- 🔍 Verifique se o problema já foi reportado

## 🙏 Agradecimentos

- Comunidade Android por feedback e sugestões
- Desenvolvedores de ferramentas root (Magisk, SuperSU)
- Contribuidores do Material Design

---

**⚠️ Disclaimer**: Este app requer root e modifica configurações do sistema. Use por sua conta e risco. Os desenvolvedores não se responsabilizam por danos ao dispositivo.
