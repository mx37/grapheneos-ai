package com.satory.graphenosai.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.satory.graphenosai.AssistantApplication
import com.satory.graphenosai.audio.VoskTranscriber
import com.satory.graphenosai.llm.GitHubCopilotAuth
import com.satory.graphenosai.llm.LocalModelManager
import com.satory.graphenosai.service.AssistantService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    assistantService: AssistantService? = null,
    onNavigateToLanguages: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as AssistantApplication
    val settingsManager = remember { SettingsManager(context) }
    val voskTranscriber = remember { VoskTranscriber(context) }
    val scope = rememberCoroutineScope()
    
    var selectedModel by remember { mutableStateOf(settingsManager.selectedModel) }
    var customModelId by remember { mutableStateOf(settingsManager.customModelId) }
    var systemPrompt by remember { mutableStateOf(settingsManager.systemPrompt) }
    var voiceInputMethod by remember { mutableStateOf(settingsManager.voiceInputMethod) }
    var ttsEnabled by remember { mutableStateOf(settingsManager.ttsEnabled) }
    var autoSendVoice by remember { mutableStateOf(settingsManager.autoSendVoice) }
    var autoStartVoice by remember { mutableStateOf(settingsManager.autoStartVoice) }
    var apiProvider by remember { mutableStateOf(settingsManager.apiProvider) }
    var multilingualEnabled by remember { mutableStateOf(settingsManager.multilingualEnabled) }
    var secondaryLanguage by remember { mutableStateOf(settingsManager.secondaryVoiceLanguage) }
    
    var showModelDialog by remember { mutableStateOf(false) }
    var showCustomModelDialog by remember { mutableStateOf(false) }
    var showLocalModelDialog by remember { mutableStateOf(false) }
    var showPromptDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showCopilotTokenDialog by remember { mutableStateOf(false) }
    var showBraveKeyDialog by remember { mutableStateOf(false) }
    var showExaKeyDialog by remember { mutableStateOf(false) }
    var showSearchEngineDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showSecondaryLanguageDialog by remember { mutableStateOf(false) }
    var hasApiKey by remember { mutableStateOf(app.secureKeyManager.hasOpenRouterApiKey()) }
    var hasCopilotToken by remember { mutableStateOf(app.secureKeyManager.hasCopilotToken()) }
    var hasBraveApiKey by remember { mutableStateOf(app.secureKeyManager.hasBraveApiKey()) }
    var hasExaApiKey by remember { mutableStateOf(app.secureKeyManager.hasExaApiKey()) }
    var searchEngine by remember { mutableStateOf(settingsManager.searchEngine) }
    
    // Voice language state
    var selectedLanguage by remember { mutableStateOf(settingsManager.voiceLanguage) }
    var downloadedLanguages by remember { mutableStateOf(voskTranscriber.getDownloadedLanguages()) }
    
    // Vosk model state  
    var isVoskModelDownloaded by remember { mutableStateOf(!voskTranscriber.needsModelDownload(selectedLanguage)) }
    var isDownloading by remember { mutableStateOf(false) }
    var isExtracting by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }
    var downloadError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // API Section
            SettingsSection(title = "API Configuration") {
                // Provider Selection - now with 3 options
                var showProviderDialog by remember { mutableStateOf(false) }
                
                val providerName = when (apiProvider) {
                    SettingsManager.PROVIDER_COPILOT -> "GitHub Copilot"
                    SettingsManager.PROVIDER_LOCAL -> "Local AI (Offline)"
                    else -> "OpenRouter"
                }
                
                SettingsItem(
                    icon = when (apiProvider) {
                        SettingsManager.PROVIDER_LOCAL -> Icons.Default.OfflineBolt
                        else -> Icons.Default.Cloud
                    },
                    title = "AI Provider",
                    subtitle = providerName,
                    onClick = { showProviderDialog = true }
                )
                
                // Provider selection dialog
                if (showProviderDialog) {
                    AlertDialog(
                        onDismissRequest = { showProviderDialog = false },
                        title = { Text("Select AI Provider") },
                        text = {
                            Column {
                                listOf(
                                    Triple(SettingsManager.PROVIDER_OPENROUTER, "OpenRouter", "Cloud AI via API key"),
                                    Triple(SettingsManager.PROVIDER_COPILOT, "GitHub Copilot", "Cloud AI via GitHub"),
                                    Triple(SettingsManager.PROVIDER_LOCAL, "Local AI (Offline)", "Runs on device, no internet needed")
                                ).forEach { (provider, name, description) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                apiProvider = provider
                                                settingsManager.apiProvider = provider
                                                assistantService?.reloadSettings()
                                                showProviderDialog = false
                                            }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = apiProvider == provider,
                                            onClick = {
                                                apiProvider = provider
                                                settingsManager.apiProvider = provider
                                                assistantService?.reloadSettings()
                                                showProviderDialog = false
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(name, fontWeight = FontWeight.Medium)
                                            Text(
                                                description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showProviderDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
                
                // OpenRouter API Key
                if (apiProvider == SettingsManager.PROVIDER_OPENROUTER) {
                    SettingsItem(
                        icon = Icons.Default.Key,
                        title = "OpenRouter API Key",
                        subtitle = if (hasApiKey) "Configured ✓" else "Not configured",
                        onClick = { showApiKeyDialog = true }
                    )
                }
                
                // GitHub Copilot Token
                if (apiProvider == SettingsManager.PROVIDER_COPILOT) {
                    SettingsItem(
                        icon = Icons.Default.Key,
                        title = "GitHub Copilot Token",
                        subtitle = if (hasCopilotToken) "Configured ✓" else "Not configured",
                        onClick = { showCopilotTokenDialog = true }
                    )
                }
            }
            
            // Local AI Models Section (only shown when Local provider selected)
            if (apiProvider == SettingsManager.PROVIDER_LOCAL) {
                LocalModelsSection(
                    assistantService = assistantService,
                    settingsManager = settingsManager
                )
            }
            
            // Web Search Section - hidden for Local AI (offline)
            if (apiProvider != SettingsManager.PROVIDER_LOCAL) {
            SettingsSection(title = "Web Search") {
                // Search engine selector
                val searchEngineName = when (searchEngine) {
                    SettingsManager.SEARCH_BRAVE -> "Brave Search"
                    SettingsManager.SEARCH_EXA -> "Exa AI"
                    else -> "Brave Search"
                }
                SettingsItem(
                    icon = Icons.Default.Search,
                    title = "Search Engine",
                    subtitle = searchEngineName,
                    onClick = { showSearchEngineDialog = true }
                )
                
                // Show API key field based on selected search engine
                if (searchEngine == SettingsManager.SEARCH_BRAVE) {
                    SettingsItem(
                        icon = Icons.Default.Key,
                        title = "Brave Search API Key",
                        subtitle = if (hasBraveApiKey) "Configured ✓" else "Required • Free at brave.com/search/api",
                        onClick = { showBraveKeyDialog = true }
                    )
                } else {
                    SettingsItem(
                        icon = Icons.Default.Key,
                        title = "Exa AI API Key",
                        subtitle = if (hasExaApiKey) "Configured ✓" else "Required • Get at dashboard.exa.ai",
                        onClick = { showExaKeyDialog = true }
                    )
                }
                
                Text(
                    text = if (searchEngine == SettingsManager.SEARCH_BRAVE)
                        "Brave Search: Privacy-focused, 2000 free queries/month"
                    else
                        "Exa AI: Semantic search with AI-powered results",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            } // End of Web Search conditional
            
            // Model Section - different for local vs cloud providers
            if (apiProvider != SettingsManager.PROVIDER_LOCAL) {
            SettingsSection(title = "AI Model") {
                val modelList = if (apiProvider == SettingsManager.PROVIDER_COPILOT) 
                    SettingsManager.COPILOT_MODELS else SettingsManager.AVAILABLE_MODELS
                val modelName = modelList
                    .find { it.id == selectedModel }?.name ?: selectedModel
                SettingsItem(
                    icon = Icons.Default.SmartToy,
                    title = "Model",
                    subtitle = if (customModelId.isNotBlank()) "Custom: $customModelId" else modelName,
                    onClick = { showModelDialog = true }
                )
                
                // Custom model ID input
                SettingsItem(
                    icon = Icons.Default.Edit,
                    title = "Custom Model ID",
                    subtitle = if (customModelId.isNotBlank()) customModelId else "Not set (use list selection)",
                    onClick = { showCustomModelDialog = true }
                )
                
                SettingsItem(
                    icon = Icons.Default.Description,
                    title = "System Prompt",
                    subtitle = systemPrompt.take(50) + if (systemPrompt.length > 50) "..." else "",
                    onClick = { showPromptDialog = true }
                )
            }
            } // End of Model Section conditional
            
            // Local Model Selection (shown when using offline AI)
            if (apiProvider == SettingsManager.PROVIDER_LOCAL) {
                SettingsSection(title = "Downloaded Models") {
                    SettingsItem(
                        icon = Icons.Default.SmartToy,
                        title = "Local Model",
                        subtitle = com.satory.graphenosai.llm.LocalModelManager.AVAILABLE_MODELS
                            .find { it.id == settingsManager.localModelId }?.name ?: "Select a model",
                        onClick = { showLocalModelDialog = true }
                    )
                }
            }
            
            // System Prompt for Local AI (shown separately since model section is hidden)
            if (apiProvider == SettingsManager.PROVIDER_LOCAL) {
                SettingsSection(title = "AI Configuration") {
                    SettingsItem(
                        icon = Icons.Default.Description,
                        title = "System Prompt",
                        subtitle = systemPrompt.take(50) + if (systemPrompt.length > 50) "..." else "",
                        onClick = { showPromptDialog = true }
                    )
                    
                    Text(
                        text = "⚡ Local AI works completely offline. No data is sent to the internet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            
            // Voice Section
            SettingsSection(title = "Voice Input") {
                var showVoiceMethodDialog by remember { mutableStateOf(false) }
                var whisperProvider by remember { mutableStateOf(settingsManager.whisperProvider) }
                var showWhisperProviderDialog by remember { mutableStateOf(false) }
                var hasGroqKey by remember { mutableStateOf(app.secureKeyManager.hasGroqApiKey()) }
                var showGroqKeyDialog by remember { mutableStateOf(false) }
                
                // Voice method selector
                val voiceMethodName = when (voiceInputMethod) {
                    SettingsManager.VOICE_INPUT_VOSK -> "Vosk (Offline)"
                    SettingsManager.VOICE_INPUT_WHISPER -> "Whisper (Cloud)"
                    else -> "System"
                }
                SettingsItem(
                    icon = Icons.Default.RecordVoiceOver,
                    title = "Voice Recognition Method",
                    subtitle = voiceMethodName,
                    onClick = { showVoiceMethodDialog = true }
                )
                
                // Whisper provider settings
                if (voiceInputMethod == SettingsManager.VOICE_INPUT_WHISPER) {
                    val providerName = when (whisperProvider) {
                        SettingsManager.WHISPER_OPENAI -> "OpenAI (requires API key)"
                        else -> "Groq (free, fast)"
                    }
                    SettingsItem(
                        icon = Icons.Default.Cloud,
                        title = "Whisper Provider",
                        subtitle = providerName,
                        onClick = { showWhisperProviderDialog = true }
                    )
                    
                    // Groq API key (only shown for Groq provider)
                    if (whisperProvider == SettingsManager.WHISPER_GROQ) {
                        SettingsItem(
                            icon = Icons.Default.Key,
                            title = "Groq API Key",
                            subtitle = if (hasGroqKey) "Configured ✓" else "Free at console.groq.com",
                            onClick = { showGroqKeyDialog = true }
                        )
                    }
                }
                
                // Voice method dialog
                if (showVoiceMethodDialog) {
                    AlertDialog(
                        onDismissRequest = { showVoiceMethodDialog = false },
                        title = { Text("Voice Recognition Method") },
                        text = {
                            Column {
                                listOf(
                                    Triple(SettingsManager.VOICE_INPUT_SYSTEM, "System", "Uses Android's built-in speech recognition"),
                                    Triple(SettingsManager.VOICE_INPUT_VOSK, "Vosk (Offline)", "Fully private, works without internet"),
                                    Triple(SettingsManager.VOICE_INPUT_WHISPER, "Whisper (Cloud)", "Best accuracy, requires internet")
                                ).forEach { (method, name, description) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                voiceInputMethod = method
                                                settingsManager.voiceInputMethod = method
                                                showVoiceMethodDialog = false
                                            }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = voiceInputMethod == method,
                                            onClick = {
                                                voiceInputMethod = method
                                                settingsManager.voiceInputMethod = method
                                                showVoiceMethodDialog = false
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(name, fontWeight = FontWeight.Medium)
                                            Text(
                                                description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showVoiceMethodDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
                
                // Whisper provider dialog
                if (showWhisperProviderDialog) {
                    AlertDialog(
                        onDismissRequest = { showWhisperProviderDialog = false },
                        title = { Text("Whisper Provider") },
                        text = {
                            Column {
                                listOf(
                                    Triple(SettingsManager.WHISPER_GROQ, "Groq", "Free tier, very fast"),
                                    Triple(SettingsManager.WHISPER_OPENAI, "OpenAI", "Original, uses OpenRouter key")
                                ).forEach { (provider, name, description) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                whisperProvider = provider
                                                settingsManager.whisperProvider = provider
                                                showWhisperProviderDialog = false
                                            }
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = whisperProvider == provider,
                                            onClick = {
                                                whisperProvider = provider
                                                settingsManager.whisperProvider = provider
                                                showWhisperProviderDialog = false
                                            }
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(name, fontWeight = FontWeight.Medium)
                                            Text(
                                                description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showWhisperProviderDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
                
                // Groq API key dialog
                if (showGroqKeyDialog) {
                    var groqKey by remember { mutableStateOf("") }
                    var showKey by remember { mutableStateOf(false) }
                    
                    AlertDialog(
                        onDismissRequest = { showGroqKeyDialog = false },
                        title = { Text("Groq API Key") },
                        text = {
                            Column {
                                Text(
                                    "Get a free API key at console.groq.com",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = groqKey,
                                    onValueChange = { groqKey = it },
                                    label = { Text("API Key") },
                                    singleLine = true,
                                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showKey = !showKey }) {
                                            Icon(
                                                if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                "Toggle visibility"
                                            )
                                        }
                                    }
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (groqKey.isNotBlank()) {
                                        app.secureKeyManager.setGroqApiKey(groqKey)
                                        hasGroqKey = true
                                    }
                                    showGroqKeyDialog = false
                                }
                            ) {
                                Text("Save")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showGroqKeyDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
                
                // Always show language management (for any voice method)
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = "Voice Languages",
                    subtitle = "${downloadedLanguages.size} downloaded • Manage Vosk models",
                    onClick = { 
                        if (onNavigateToLanguages != null) {
                            onNavigateToLanguages()
                        } else {
                            showLanguageDialog = true
                        }
                    }
                )
                
                // Language selection for Vosk
                if (voiceInputMethod == SettingsManager.VOICE_INPUT_VOSK) {
                    val currentLang = VoskTranscriber.getLanguageByCode(selectedLanguage)
                    val isLangDownloaded = downloadedLanguages.contains(selectedLanguage)
                    SettingsItem(
                        icon = Icons.Default.Language,
                        title = "Recognition Language",
                        subtitle = "${currentLang.displayName}" + 
                            if (isLangDownloaded) " ✓" else " (download required)",
                        onClick = { showLanguageDialog = true }
                    )
                
                // Vosk Model Download Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLangDownloaded) 
                                MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isLangDownloaded) Icons.Default.CheckCircle 
                                                  else Icons.Default.Download,
                                    contentDescription = null,
                                    tint = if (isLangDownloaded) 
                                        MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${currentLang.displayName} Model",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (isLangDownloaded) "Downloaded ✓ (~${currentLang.sizeBytes / 1_000_000}MB)"
                                               else "Required for offline voice (~${currentLang.sizeBytes / 1_000_000}MB)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                if (!isLangDownloaded && !isDownloading && !isExtracting) {
                                    Button(
                                        onClick = {
                                            isDownloading = true
                                            isExtracting = false
                                            downloadError = null
                                            scope.launch {
                                                voskTranscriber.downloadModel(selectedLanguage).collect { state ->
                                                    when (state) {
                                                        is VoskTranscriber.DownloadState.Downloading -> {
                                                            isDownloading = true
                                                            isExtracting = false
                                                            downloadProgress = state.progress
                                                        }
                                                        is VoskTranscriber.DownloadState.Extracting -> {
                                                            isDownloading = false
                                                            isExtracting = true
                                                            downloadProgress = state.progress
                                                        }
                                                        is VoskTranscriber.DownloadState.Complete -> {
                                                            isDownloading = false
                                                            isExtracting = false
                                                            isVoskModelDownloaded = true
                                                            downloadedLanguages = voskTranscriber.getDownloadedLanguages()
                                                            // Reload service settings to initialize Vosk in AssistantService
                                                            assistantService?.reloadSettings()
                                                        }
                                                        is VoskTranscriber.DownloadState.Error -> {
                                                            isDownloading = false
                                                            isExtracting = false
                                                            downloadError = state.message
                                                        }
                                                        else -> {}
                                                    }
                                                }
                                            }
                                        }
                                    ) {
                                        Text("Download")
                                    }
                                }
                            }
                            
                            if (isDownloading || isExtracting) {
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { downloadProgress / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = if (isExtracting) 
                                        "Extracting... $downloadProgress%" 
                                    else 
                                        "Downloading... $downloadProgress%",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                if (isExtracting) {
                                    Text(
                                        text = "This may take a while for large models...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                            
                            downloadError?.let { error ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    
                    // Multilingual mode toggle
                    SettingsItemWithSwitch(
                        icon = Icons.Default.Translate,
                        title = "Multilingual Mode",
                        subtitle = "Recognize mixed language speech (e.g., Russian + English)",
                        checked = multilingualEnabled,
                        onCheckedChange = {
                            multilingualEnabled = it
                            settingsManager.multilingualEnabled = it
                            assistantService?.reloadSettings()
                        }
                    )
                    
                    // Secondary language selection
                    if (multilingualEnabled) {
                        val secondaryLang = VoskTranscriber.getLanguageByCode(secondaryLanguage)
                        val isSecondaryDownloaded = downloadedLanguages.contains(secondaryLanguage)
                        SettingsItem(
                            icon = Icons.Default.Language,
                            title = "Secondary Language",
                            subtitle = "${secondaryLang.displayName}" + 
                                if (isSecondaryDownloaded) " ✓" else " (download required)",
                            onClick = { showSecondaryLanguageDialog = true }
                        )
                    }
                } // end if voiceInputMethod == VOSK
                
                SettingsItemWithSwitch(
                    icon = Icons.AutoMirrored.Default.Send,
                    title = "Auto-send after speech",
                    subtitle = "Automatically send query when speech ends",
                    checked = autoSendVoice,
                    onCheckedChange = {
                        autoSendVoice = it
                        settingsManager.autoSendVoice = it
                    }
                )
                
                SettingsItemWithSwitch(
                    icon = Icons.Default.Mic,
                    title = "Auto-start voice input",
                    subtitle = "Start listening when assistant opens",
                    checked = autoStartVoice,
                    onCheckedChange = {
                        autoStartVoice = it
                        settingsManager.autoStartVoice = it
                    }
                )
            }
            
            // Output Section
            SettingsSection(title = "Output") {
                val ttsAvailable = remember { 
                    com.satory.graphenosai.tts.TTSManager.isTTSAvailable(context) 
                }
                
                SettingsItemWithSwitch(
                    icon = Icons.Default.VolumeUp,
                    title = "Text-to-Speech",
                    subtitle = if (ttsAvailable) "Read responses aloud" else "Not available on this device",
                    checked = ttsEnabled && ttsAvailable,
                    onCheckedChange = {
                        if (ttsAvailable) {
                            ttsEnabled = it
                            settingsManager.ttsEnabled = it
                        }
                    },
                    enabled = ttsAvailable
                )
                
                if (!ttsAvailable) {
                    Text(
                        text = "Text-to-speech is not available on this device. Install a TTS engine from the Play Store to enable this feature.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            
            // Reset Section
            SettingsSection(title = "Advanced") {
                SettingsItem(
                    icon = Icons.Default.Refresh,
                    title = "Reset to Defaults",
                    subtitle = "Reset all settings to default values",
                    onClick = {
                        settingsManager.resetToDefaults()
                        selectedModel = SettingsManager.DEFAULT_MODEL
                        systemPrompt = SettingsManager.DEFAULT_SYSTEM_PROMPT
                        voiceInputMethod = SettingsManager.VOICE_INPUT_SYSTEM
                        ttsEnabled = true
                        autoSendVoice = true
                        autoStartVoice = false
                    }
                )
            }
            
            // About Section
            SettingsSection(title = "About") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "AI Assistant for Android",
                    subtitle = "v1.1.1",
                    onClick = {}
                )
                
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Developer",
                    subtitle = "Max",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://github.com/mx37")
                        }
                        context.startActivity(intent)
                    }
                )
                
                SettingsItem(
                    icon = Icons.Default.Link,
                    title = "Repository",
                    subtitle = "View on GitHub",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://github.com/mx37/grapheneos-ai")
                        }
                        context.startActivity(intent)
                    }
                )
                
                Text(
                    text = "Privacy-first AI assistant with OpenRouter and GitHub Copilot support. All conversations are processed securely with on-device encryption.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                
                Text(
                    text = "This is an independent project and is not affiliated with, endorsed by, or associated with GrapheneOS.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Model Selection Dialog
    if (showModelDialog) {
        val modelList = if (apiProvider == SettingsManager.PROVIDER_COPILOT) 
            SettingsManager.COPILOT_MODELS else SettingsManager.AVAILABLE_MODELS
        ModelSelectionDialog(
            currentModel = selectedModel,
            models = modelList,
            onModelSelected = { model ->
                selectedModel = model
                settingsManager.selectedModel = model
                assistantService?.reloadSettings()
                showModelDialog = false
            },
            onDismiss = { showModelDialog = false }
        )
    }
    
    // Custom Model Dialog
    if (showCustomModelDialog) {
        CustomModelDialog(
            currentCustomModel = customModelId,
            onModelSaved = { modelId ->
                customModelId = modelId
                settingsManager.customModelId = modelId
                assistantService?.reloadSettings()
                showCustomModelDialog = false
            },
            onDismiss = { showCustomModelDialog = false }
        )
    }
    
    // Local Model Selection Dialog
    if (showLocalModelDialog) {
        LocalModelSelectionDialog(
            currentModel = settingsManager.localModelId,
            onModelSelected = { modelId ->
                settingsManager.localModelId = modelId
                assistantService?.reloadSettings()
                showLocalModelDialog = false
            },
            onDismiss = { showLocalModelDialog = false }
        )
    }
    
    // System Prompt Dialog
    if (showPromptDialog) {
        SystemPromptDialog(
            currentPrompt = systemPrompt,
            onPromptSaved = { prompt ->
                systemPrompt = prompt
                settingsManager.systemPrompt = prompt
                assistantService?.reloadSettings()
                showPromptDialog = false
            },
            onDismiss = { showPromptDialog = false }
        )
    }
    
    // API Key Dialog
    if (showApiKeyDialog) {
        ApiKeyDialog(
            hasExistingKey = hasApiKey,
            onApiKeySaved = { key ->
                app.secureKeyManager.setOpenRouterApiKey(key)
                hasApiKey = true
                showApiKeyDialog = false
            },
            onDismiss = { showApiKeyDialog = false }
        )
    }
    
    // Copilot OAuth Dialog
    if (showCopilotTokenDialog) {
        GitHubOAuthDialog(
            onTokenReceived = { token ->
                app.secureKeyManager.setCopilotToken(token)
                hasCopilotToken = true
                showCopilotTokenDialog = false
            },
            onDismiss = { showCopilotTokenDialog = false }
        )
    }
    
    // Brave Search API Key Dialog
    if (showBraveKeyDialog) {
        BraveApiKeyDialog(
            hasExistingKey = hasBraveApiKey,
            onApiKeySaved = { key ->
                app.secureKeyManager.setBraveApiKey(key)
                hasBraveApiKey = true
                showBraveKeyDialog = false
            },
            onDismiss = { showBraveKeyDialog = false }
        )
    }
    
    // Exa AI API Key Dialog
    if (showExaKeyDialog) {
        ExaApiKeyDialog(
            hasExistingKey = hasExaApiKey,
            onApiKeySaved = { key ->
                app.secureKeyManager.setExaApiKey(key)
                hasExaApiKey = true
                showExaKeyDialog = false
            },
            onDismiss = { showExaKeyDialog = false }
        )
    }
    
    // Search Engine Selection Dialog
    if (showSearchEngineDialog) {
        SearchEngineDialog(
            currentEngine = searchEngine,
            onEngineSelected = { engine ->
                searchEngine = engine
                settingsManager.searchEngine = engine
                showSearchEngineDialog = false
            },
            onDismiss = { showSearchEngineDialog = false }
        )
    }
    
    // Language Selection Dialog
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = selectedLanguage,
            downloadedLanguages = downloadedLanguages,
            onLanguageSelected = { langCode ->
                selectedLanguage = langCode
                settingsManager.voiceLanguage = langCode
                isVoskModelDownloaded = downloadedLanguages.contains(langCode)
                assistantService?.reloadSettings()
                showLanguageDialog = false
            },
            onDismiss = { showLanguageDialog = false }
        )
    }
    
    // Secondary Language Selection Dialog
    if (showSecondaryLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = secondaryLanguage,
            downloadedLanguages = downloadedLanguages,
            onLanguageSelected = { langCode ->
                secondaryLanguage = langCode
                settingsManager.secondaryVoiceLanguage = langCode
                assistantService?.reloadSettings()
                showSecondaryLanguageDialog = false
            },
            onDismiss = { showSecondaryLanguageDialog = false }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun SettingsItemWithSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    ListItem(
        headlineContent = { 
            Text(
                title,
                color = if (enabled) MaterialTheme.colorScheme.onSurface 
                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            ) 
        },
        supportingContent = { 
            Text(
                subtitle, 
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant 
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            ) 
        },
        leadingContent = { 
            Icon(
                icon, 
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant 
                      else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            ) 
        },
        trailingContent = {
            Switch(
                checked = checked, 
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectionDialog(
    currentModel: String,
    models: List<SettingsManager.ModelInfo> = SettingsManager.AVAILABLE_MODELS,
    onModelSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f)
        ) {
            Column {
                TopAppBar(
                    title = { Text("Select Model") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                )
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Free models section (only for OpenRouter)
                    val freeModels = models.filter { it.id.contains(":free") }
                    if (freeModels.isNotEmpty()) {
                        item {
                            Text(
                                "Free Models",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        items(freeModels) { model ->
                            ModelItem(
                                model = model,
                                isSelected = model.id == currentModel,
                                onClick = { onModelSelected(model.id) }
                            )
                        }
                    }
                    
                    // Premium/All models section
                    val premiumModels = models.filter { !it.id.contains(":free") }
                    if (premiumModels.isNotEmpty()) {
                        item {
                            Text(
                                if (freeModels.isNotEmpty()) "Premium Models" else "Available Models",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        items(premiumModels) { model ->
                            ModelItem(
                                model = model,
                                isSelected = model.id == currentModel,
                                onClick = { onModelSelected(model.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModelItem(
    model: SettingsManager.ModelInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { 
            Text(
                model.name,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ) 
        },
        supportingContent = { Text(model.description) },
        leadingContent = {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun SystemPromptDialog(
    currentPrompt: String,
    onPromptSaved: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var prompt by remember { mutableStateOf(currentPrompt) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.7f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    "System Prompt",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "This prompt defines how the AI assistant behaves.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    label = { Text("System Prompt") },
                    placeholder = { Text("Enter instructions for the AI...") }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { prompt = SettingsManager.DEFAULT_SYSTEM_PROMPT },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset")
                    }
                    
                    Button(
                        onClick = { onPromptSaved(prompt) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun ApiKeyDialog(
    hasExistingKey: Boolean,
    onApiKeySaved: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKey by remember { mutableStateOf("") }
    var isVisible by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("OpenRouter API Key") },
        text = {
            Column {
                if (hasExistingKey) {
                    Text(
                        "An API key is already configured. Enter a new key to replace it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Text(
                    "Get your API key from openrouter.ai",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API Key") },
                    singleLine = true,
                    visualTransformation = if (isVisible) VisualTransformation.None 
                                          else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isVisible = !isVisible }) {
                            Icon(
                                if (isVisible) Icons.Default.VisibilityOff 
                                else Icons.Default.Visibility,
                                contentDescription = "Toggle visibility"
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onApiKeySaved(apiKey) },
                enabled = apiKey.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BraveApiKeyDialog(
    hasExistingKey: Boolean,
    onApiKeySaved: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKey by remember { mutableStateOf("") }
    var isVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Brave Search API Key") },
        text = {
            Column {
                if (hasExistingKey) {
                    Text(
                        "An API key is already configured. Enter a new key to replace it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Text(
                    "Brave Search API provides privacy-focused web search.\n\n" +
                    "• Free tier: 2,000 queries/month\n" +
                    "• Fast and accurate results\n" +
                    "• No tracking, no ads",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://brave.com/search/api/"))
                        context.startActivity(intent)
                    }
                ) {
                    Icon(Icons.AutoMirrored.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Get API Key at brave.com/search/api")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API Key") },
                    singleLine = true,
                    visualTransformation = if (isVisible) VisualTransformation.None 
                                          else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isVisible = !isVisible }) {
                            Icon(
                                if (isVisible) Icons.Default.VisibilityOff 
                                else Icons.Default.Visibility,
                                contentDescription = "Toggle visibility"
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onApiKeySaved(apiKey) },
                enabled = apiKey.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun GitHubOAuthDialog(
    onTokenReceived: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = context.getSystemService(ClipboardManager::class.java)
    
    var authState by remember { mutableStateOf<GitHubOAuthState>(GitHubOAuthState.Initial) }
    var userCode by remember { mutableStateOf("") }
    var verificationUri by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Start OAuth flow when dialog opens
    LaunchedEffect(Unit) {
        authState = GitHubOAuthState.Starting
        
        val auth = GitHubCopilotAuth()
        val result = auth.authenticate { state ->
            when (state) {
                is GitHubCopilotAuth.AuthState.Idle -> {
                    authState = GitHubOAuthState.Starting
                }
                is GitHubCopilotAuth.AuthState.WaitingForUser -> {
                    userCode = state.userCode
                    verificationUri = state.verificationUri
                    authState = GitHubOAuthState.WaitingForUser
                    // Don't auto-open browser - let user copy the code first
                }
                is GitHubCopilotAuth.AuthState.Polling -> {
                    authState = GitHubOAuthState.Polling
                }
                is GitHubCopilotAuth.AuthState.Success -> {
                    authState = GitHubOAuthState.Success
                    onTokenReceived(state.token)
                }
                is GitHubCopilotAuth.AuthState.Error -> {
                    errorMessage = state.message
                    authState = GitHubOAuthState.Error
                }
            }
        }
        
        if (result.isFailure && authState != GitHubOAuthState.Success && authState != GitHubOAuthState.Error) {
            errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
            authState = GitHubOAuthState.Error
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "GitHub Copilot Login",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                when (authState) {
                    GitHubOAuthState.Initial, GitHubOAuthState.Starting -> {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Connecting to GitHub...",
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    GitHubOAuthState.WaitingForUser -> {
                        Text(
                            "1. Copy this code:",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Big code display - centered
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(20.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = userCode,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 6.dp.value.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager?.setPrimaryClip(
                                            ClipData.newPlainText("user_code", userCode)
                                        )
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copy code",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Copy Code")
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Text(
                            "2. Open GitHub and enter the code:",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(verificationUri))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open GitHub")
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            verificationUri,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    GitHubOAuthState.Polling -> {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Waiting for authorization...",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Complete the login in your browser",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    GitHubOAuthState.Success -> {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Successfully logged in!",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    GitHubOAuthState.Error -> {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Login failed",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        errorMessage?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (authState != GitHubOAuthState.Success) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}private sealed class GitHubOAuthState {
    object Initial : GitHubOAuthState()
    object Starting : GitHubOAuthState()
    object WaitingForUser : GitHubOAuthState()
    object Polling : GitHubOAuthState()
    object Success : GitHubOAuthState()
    object Error : GitHubOAuthState()
}

@Composable
fun CustomModelDialog(
    currentCustomModel: String,
    onModelSaved: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var modelId by remember { mutableStateOf(currentCustomModel) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Model ID") },
        text = {
            Column {
                Text(
                    "Enter the model ID from OpenRouter (e.g., openai/gpt-4-turbo)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = modelId,
                    onValueChange = { modelId = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Model ID") },
                    placeholder = { Text("provider/model-name") },
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Leave empty to use selected model from list",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onModelSaved(modelId.trim()) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LanguageSelectionDialog(
    currentLanguage: String,
    downloadedLanguages: List<String>,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Select Voice Language",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    items(VoskTranscriber.AVAILABLE_LANGUAGES) { language ->
                        val isDownloaded = downloadedLanguages.contains(language.code)
                        val isSelected = currentLanguage == language.code
                        
                        ListItem(
                            modifier = Modifier
                                .clickable { onLanguageSelected(language.code) }
                                .padding(vertical = 2.dp),
                            headlineContent = { 
                                Text(
                                    text = language.displayName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            supportingContent = {
                                Text(
                                    text = if (isDownloaded) "Downloaded ✓" else "~${language.sizeBytes / 1_000_000}MB download",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            leadingContent = {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onLanguageSelected(language.code) }
                                )
                            },
                            trailingContent = {
                                if (isDownloaded) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Downloaded",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun ExaApiKeyDialog(
    hasExistingKey: Boolean,
    onApiKeySaved: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKey by remember { mutableStateOf("") }
    var isVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exa AI API Key") },
        text = {
            Column {
                if (hasExistingKey) {
                    Text(
                        "An API key is already configured. Enter a new key to replace it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                Text(
                    "Exa AI provides semantic search with AI-powered results.\n\n" +
                    "• Neural & keyword search\n" +
                    "• High-quality content extraction\n" +
                    "• Great for research queries",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://dashboard.exa.ai/api-keys"))
                        context.startActivity(intent)
                    }
                ) {
                    Icon(Icons.AutoMirrored.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Get API Key at dashboard.exa.ai")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("API Key") },
                    singleLine = true,
                    visualTransformation = if (isVisible) VisualTransformation.None 
                                          else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isVisible = !isVisible }) {
                            Icon(
                                if (isVisible) Icons.Default.VisibilityOff 
                                else Icons.Default.Visibility,
                                contentDescription = "Toggle visibility"
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onApiKeySaved(apiKey) },
                enabled = apiKey.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SearchEngineDialog(
    currentEngine: String,
    onEngineSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Search Engine") },
        text = {
            Column {
                // Brave Search option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEngineSelected(SettingsManager.SEARCH_BRAVE) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentEngine == SettingsManager.SEARCH_BRAVE,
                        onClick = { onEngineSelected(SettingsManager.SEARCH_BRAVE) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Brave Search", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Privacy-focused, 2000 free queries/month",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                HorizontalDivider()
                
                // Exa AI option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEngineSelected(SettingsManager.SEARCH_EXA) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentEngine == SettingsManager.SEARCH_EXA,
                        onClick = { onEngineSelected(SettingsManager.SEARCH_EXA) }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Exa AI", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Semantic search with AI-powered results",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Section for managing local AI models
 */
@Composable
fun LocalModelsSection(
    assistantService: com.satory.graphenosai.service.AssistantService?,
    settingsManager: SettingsManager
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localModelManager = remember { com.satory.graphenosai.llm.LocalModelManager(context) }
    
    var downloadedModels by remember { mutableStateOf(localModelManager.getDownloadedModels()) }
    var selectedLocalModel by remember { mutableStateOf(settingsManager.localModelId) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0) }
    var downloadingModelId by remember { mutableStateOf<String?>(null) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var showModelListDialog by remember { mutableStateOf(false) }
    
    // Refresh downloaded models
    fun refreshModels() {
        downloadedModels = localModelManager.getDownloadedModels()
    }
    
    SettingsSection(title = "Local AI Models") {
        // Current model info
        val currentModelInfo = com.satory.graphenosai.llm.LocalModelManager.AVAILABLE_MODELS
            .find { it.id == selectedLocalModel }
        val isCurrentModelDownloaded = localModelManager.isModelDownloaded(selectedLocalModel)
        
        SettingsItem(
            icon = Icons.Default.SmartToy,
            title = "Active Model",
            subtitle = if (isCurrentModelDownloaded) {
                "${currentModelInfo?.name ?: selectedLocalModel} ✓"
            } else {
                "${currentModelInfo?.name ?: selectedLocalModel} (Not downloaded)"
            },
            onClick = { showModelListDialog = true }
        )
        
        // Download status / progress
        if (isDownloading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val downloadingModel = com.satory.graphenosai.llm.LocalModelManager.AVAILABLE_MODELS
                    .find { it.id == downloadingModelId }
                Text(
                    "Downloading ${downloadingModel?.name ?: "model"}...",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { downloadProgress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "$downloadProgress%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Download error
        if (downloadError != null) {
            Text(
                text = "❌ $downloadError",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        
        // Downloaded models count
        val totalStorage = localModelManager.getTotalStorageUsed()
        val storageText = when {
            totalStorage >= 1_000_000_000 -> String.format("%.1f GB", totalStorage / 1_000_000_000.0)
            totalStorage >= 1_000_000 -> String.format("%.1f MB", totalStorage / 1_000_000.0)
            else -> "0 MB"
        }
        
        Text(
            text = "${downloadedModels.size} model(s) downloaded • $storageText used",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // Info text
        Text(
            text = "Models run entirely on your device. Recommended: 8GB+ RAM for best performance.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
    
    // Model list dialog
    if (showModelListDialog) {
        AlertDialog(
            onDismissRequest = { showModelListDialog = false },
            title = { Text("Local AI Models") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(com.satory.graphenosai.llm.LocalModelManager.AVAILABLE_MODELS) { model ->
                        val isDownloaded = localModelManager.isModelDownloaded(model.id)
                        val isSelected = selectedLocalModel == model.id
                        val isThisDownloading = downloadingModelId == model.id && isDownloading
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable(enabled = !isThisDownloading) {
                                    if (isDownloaded) {
                                        // Select this model
                                        selectedLocalModel = model.id
                                        settingsManager.localModelId = model.id
                                        assistantService?.loadLocalModel(model.id)
                                        showModelListDialog = false
                                    } else {
                                        // Start download
                                        isDownloading = true
                                        downloadingModelId = model.id
                                        downloadError = null
                                        downloadProgress = 0
                                        
                                        scope.launch {
                                            localModelManager.downloadModel(model.id).collect { progress ->
                                                when (progress) {
                                                    is com.satory.graphenosai.llm.DownloadProgress.Downloading -> {
                                                        downloadProgress = progress.percent
                                                    }
                                                    is com.satory.graphenosai.llm.DownloadProgress.Completed -> {
                                                        isDownloading = false
                                                        downloadingModelId = null
                                                        refreshModels()
                                                        // Auto-select downloaded model
                                                        selectedLocalModel = model.id
                                                        settingsManager.localModelId = model.id
                                                        assistantService?.loadLocalModel(model.id)
                                                    }
                                                    is com.satory.graphenosai.llm.DownloadProgress.Error -> {
                                                        isDownloading = false
                                                        downloadingModelId = null
                                                        downloadError = progress.message
                                                    }
                                                    else -> {}
                                                }
                                            }
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected && isDownloaded) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            model.name,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (model.recommended) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "⭐",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    
                                    if (isDownloaded) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Download,
                                                contentDescription = "Downloaded",
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    } else if (isThisDownloading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            model.formattedSize(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    model.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                if (!isDownloaded && !isThisDownloading) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Tap to download",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelListDialog = false }) {
                    Text("Close")
                }
            },
            dismissButton = {
                // Delete downloaded models button
                if (downloadedModels.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                downloadedModels.forEach { model ->
                                    localModelManager.deleteModel(model.id)
                                }
                                refreshModels()
                            }
                        }
                    ) {
                        Text("Delete All", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }
}

/**
 * Dialog for selecting local models from downloaded collection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalModelSelectionDialog(
    currentModel: String,
    onModelSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f)
        ) {
            Column {
                TopAppBar(
                    title = { Text("Select Local Model") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                )
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(LocalModelManager.AVAILABLE_MODELS) { model ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onModelSelected(model.id)
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = model.id == currentModel,
                                onClick = { onModelSelected(model.id) }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    model.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    model.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
