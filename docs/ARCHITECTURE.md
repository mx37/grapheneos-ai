# Architecture Overview

## Project Structure

```
app/src/main/
├── java/com/satory/graphenosai/
│   ├── AssistantApplication.kt          # App entry point
│   ├── MainActivity.kt                  # Main UI activity
│   ├── accessibility/
│   │   ├── AssistantAccessibilityService.kt    # Volume button listener & hotkey trigger
│   │   └── GlobalShortcutListener.kt           # Global shortcuts handling
│   ├── audio/
│   │   ├── VoskTranscriber.kt                  # On-device speech recognition
│   │   ├── TextToSpeechManager.kt              # Text-to-speech output
│   │   └── GroqTranscriber.kt                  # Cloud-based speech recognition
│   ├── llm/
│   │   ├── OpenRouterClient.kt                 # OpenRouter API integration
│   │   ├── CopilotClient.kt                    # GitHub Copilot API integration
│   │   └── GitHubCopilotAuth.kt               # OAuth2 authentication
│   ├── search/
│   │   ├── BraveSearchClient.kt               # Brave Search API integration
│   │   └── SearchResult.kt                    # Search result models
│   ├── storage/
│   │   ├── KeyManager.kt                      # Encrypted API key storage
│   │   ├── SettingsManager.kt                 # User preferences
│   │   └── ChatSessionManager.kt              # Chat history
│   └── ui/
│       ├── SettingsScreen.kt                  # Settings UI
│       ├── MainScreen.kt                      # Chat UI
│       ├── MarkdownText.kt                    # Markdown renderer
│       └── SearchContextDialog.kt             # Search dialog
└── res/
    ├── values/strings.xml
    ├── drawable/                # App icons
    └── mipmap/                  # Launcher icons
```

## Key Components

### 1. Accessibility Service (`AssistantAccessibilityService`)
- Listens for Volume Up + Volume Down key combination
- Launches the assistant activity when triggered
- Runs in background as system service
- Requires "Accessibility" permission

**Key Methods:**
- `onAccessibilityEvent()` - Intercepts system events and detects hotkey
- `launchAssistant()` - Opens main activity

### 2. Audio Processing

#### Vosk Transcriber (On-Device)
- Completely offline speech recognition
- Uses Vosk library with pre-trained models
- Fast and lightweight
- Minimal data usage

#### Groq Transcriber (Cloud)
- High-accuracy speech-to-text
- Fast inference via Groq API
- Requires internet connection
- Needs API key

#### TextToSpeechManager
- Android system TextToSpeech engine
- Reads responses aloud
- Configurable speech rate and language

### 3. LLM Integration

#### OpenRouter Client
- Unified API for 100+ models
- Supports streaming responses
- Token-based authentication
- Handles chat history

**Streaming Flow:**
```
User Query → Build Request → Stream Server-Sent Events → Parse JSON → Display Content
```

#### GitHub Copilot Client
- Utilizes OAuth2 Device Authorization Grant flow for secure authentication
- Implements device code authorization where users visit a GitHub URL to approve access
- Integrates with GitHub Copilot's language model for chat responses

**Authentication Flow:**
```
App Requests Device Code → User Authorizes at GitHub URL → App Polls for Access Token → Authenticated API Calls
```

### 4. Search Integration

#### Brave Search API
- Privacy-respecting search
- Real-time information retrieval
- Injected into chat context
- Optional feature

### 5. Storage & Security

#### KeyManager
- Uses Android Keystore for encryption
- Stores API keys securely
- AES-256-GCM encryption
- Device-locked keys

#### SettingsManager
- Sharedpreferences for user settings
- Stores:
  - Selected LLM backend
  - Voice recognition method
  - TTS preferences
  - Search settings

#### ChatSessionManager
- Maintains conversation history
- Manages system prompts
- Persists chat sessions
- Limits history for context size

### 6. UI Architecture

#### Jetpack Compose
- Modern declarative UI framework
- Reactive state management
- Material Design 3 components

**Main Screens:**
- `MainScreen` - Chat interface with message history
- `SettingsScreen` - Configuration for all backends
- `SearchContextDialog` - Web search integration UI

## Data Flow

### Text Chat
```
User Input → LLM Client → Streaming Response → Parse → Update UI → Save to History
```

### Voice Input
```
Audio Recording → Transcriber → Speech-to-Text → LLM Client → TTS Output → Chat History
```

### Web Search
```
User Query → Brave Search → Process Results → Inject into Prompt → Send to LLM → Response with Citations
```

## Authentication

### OpenRouter
- Simple API key authentication
- `Authorization: Bearer <api_key>` header
- Stored in encrypted Android Keystore

### GitHub Copilot
- OAuth2 Device Authorization Grant flow
- User visits GitHub URL to authorize
- No client secret needed (public client)
- Tokens cached locally

### Brave Search
- Simple API key authentication
- `Accept-Encoding: gzip` support
- Rate limited (free tier: 2000/month)

## Permissions

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

## Build Configuration

### Minification (ProGuard)
- Enabled in release builds
- Custom keep rules for PDFBox, Compose, Kotlin Coroutines
- Line number preservation for crash reporting

### Dependencies

**Core:**
- Kotlin Coroutines (async/await)
- Jetpack Compose (UI)
- Jetpack Navigation (routing)

**APIs:**
- OkHttp3 (HTTP client)
- org.json (JSON parsing)

**Audio:**
- Vosk (speech recognition)
- Android TextToSpeech (TTS)

**PDF (optional):**
- PDFBox (document parsing)

### Kotlin Target
- JVM 17
- Kotlin 1.9+

## Security

- API keys never logged or exposed
- Encrypted storage with Android Keystore
- No analytics or tracking
- Minimal permissions requested
- No data sent to third parties without explicit API calls