# DroidCoder2

> **AI-Powered Coding Assistant for Android** — Native Kotlin + Material You 3

DroidCoder2 é um assistente de codificação com IA que roda nativamente no Android. Ele pode acessar, editar e excluir arquivos do seu projeto, além de enviar código para o GitHub — tudo através de uma interface moderna Material You 3.

---

## ✨ Funcionalidades

- **💬 Chat com IA**: Converse com modelos de IA (OpenAI, Gemini, etc.) usando a API compatível com OpenAI
- **📁 Gerenciamento de Arquivos**: Leia, edite, crie e exclua arquivos diretamente pelo app
- **🔀 Git Integrado**: Clone repositórios, faça commits e push para o GitHub (via JGit)
- **🛠️ Ferramentas de IA**: A IA pode usar ferramentas como `list_files`, `read_file`, `write_file`, `delete_file`, `git_commit`, `git_push`
- **📂 Seleção de Pasta**: Escolha qualquer pasta do dispositivo via SAF (Storage Access Framework)
- **🎨 Material You 3**: Design moderno com cores dinâmicas (Android 12+)
- **⚡ Processamento Nativo**: Operações pesadas (hash, diff, busca) delegadas para C++ via JNI
- **📱 Auto-fetch de Modelos**: Busca automaticamente os modelos disponíveis do provedor de IA

---

## 🏗️ Arquitetura

```
app/src/main/java/com/deividsrk/droidcoder/
├── agent/
│   ├── AgentApi.kt          # Cliente HTTP OpenAI-compatible
│   ├── AgentCore.kt         # Loop de execução do agente de IA
│   ├── Models.kt            # Data classes (mensagens, config, API)
│   └── SystemPrompt.kt      # Prompt do sistema que define o comportamento
├── file/
│   ├── FileManager.kt       # Operações de sistema de arquivos
│   └── NativeBridge.kt      # Ponte JNI para código nativo C++
├── git/
│   └── GitManager.kt        # Operações Git via JGit
├── tool/
│   ├── ToolRegistry.kt      # Definições das ferramentas (OpenAI function calling)
│   └── ToolExecutor.kt      # Executor das chamadas de ferramentas
├── ui/
│   ├── MainActivity.kt      # Entry point
│   ├── MainScreen.kt        # Navegação principal (Chat | Arquivos | Config)
│   ├── MainViewModel.kt     # ViewModel central
│   ├── chat/ChatScreen.kt   # Tela de chat com IA
│   ├── explorer/FileExplorerScreen.kt  # Explorador de arquivos + editor
│   ├── settings/SettingsScreen.kt      # Configurações
│   └── theme/               # Tema Material You 3
├── jni/
│   ├── CMakeLists.txt       # Build config para código nativo
│   └── native-lib.cpp       # Operações nativas (SHA-256, diff, busca)
└── MainActivity.kt
```

---

## 🚀 Como Compilar

### Pré-requisitos

- Android Studio Hedgehog (2024.1+) ou mais recente
- JDK 21+
- Android SDK 35
- NDK 27.0.12077973 (para o módulo nativo)
- CMake 3.22.1+

### Passos

```bash
# Clone o repositório
git clone https://github.com/deivid22srk/DroidCoder2.git
cd DroidCoder2

# Dê permissão de execução ao gradlew
chmod +x gradlew

# Compile o APK de debug
./gradlew assembleDebug

# O APK estará em:
# app/build/outputs/apk/debug/app-debug.apk
```

### GitHub Actions

O projeto inclui `.github/workflows/build.yml` que compila automaticamente o APK em pushes para `main`/`master` e faz upload dos artefatos.

---

## 🤖 Ferramentas Disponíveis para a IA

| Ferramenta | Descrição |
|------------|-----------|
| `list_files` | Lista todos os arquivos do projeto |
| `read_file` | Lê o conteúdo de um arquivo |
| `write_file` | Cria ou sobrescreve um arquivo |
| `delete_file` | Exclui um arquivo |
| `git_status` | Mostra o status do repositório Git |
| `git_commit` | Faz commit das alterações |
| `git_push` | Envia commits para o GitHub |
| `git_clone` | Clona um repositório remoto |
| `finish` | Finaliza a tarefa atual |

---

## 📝 Licença

MIT License — veja o arquivo [LICENSE](LICENSE) para detalhes.

---

Feito com 💜 por [@deivid22srk](https://github.com/deivid22srk)
