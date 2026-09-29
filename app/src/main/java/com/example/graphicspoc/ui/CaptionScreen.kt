package com.example.graphicspoc.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.graphicspoc.captions.model.CaptionLine
import com.example.graphicspoc.captions.transcribe.SttProvider
import com.example.graphicspoc.captions.transcribe.SttSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptionScreen(vm: CaptionViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.onVideoPicked(uri)
    }
    val pickVideo = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Hinglish Captions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (state.videoUri != null) TextButton(onClick = pickVideo) { Text("New") }
                },
                actions = { TextButton(onClick = { showSettings = true }) { Text("Settings") } },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            val uri = state.videoUri
            if (uri == null) {
                EmptyState(onPick = pickVideo, needsKey = state.settings.apiKey.isBlank(), onSettings = { showSettings = true })
            } else {
                Column(Modifier.fillMaxSize()) {
                    VideoPreview(
                        uri = uri,
                        aspect = state.videoAspect,
                        lines = state.lines,
                        style = state.style,
                        layout = state.layout,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                    StageBanner(state.stage, onCancel = vm::cancel, onDismiss = vm::dismissStage)
                    EditorTabs(state, vm, onSettings = { showSettings = true })
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            current = state.settings,
            keyFor = vm::apiKeyFor,
            onDismiss = { showSettings = false },
            onSave = {
                vm.saveSettings(it)
                showSettings = false
            },
        )
    }
}

@Composable
private fun EmptyState(onPick: () -> Unit, needsKey: Boolean, onSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Reels ke liye Hinglish captions", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Captions come from what you actually say – nothing is made up.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        listOf(
            "1" to "We pull the audio out of your video",
            "2" to "Whisper transcribes it word-by-word with timings",
            "3" to "Hindi words are written in Hinglish (main, nahi, bahut)",
            "4" to "Pick a style, fix any word, export",
        ).forEach { (n, text) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(n, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(28.dp))
                Text(text, style = MaterialTheme.typography.bodyLarge)
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onPick, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Pick a short video") }
        if (needsKey) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text("Add speech-to-text API key") }
        }
    }
}

@Composable
private fun StageBanner(stage: Stage, onCancel: () -> Unit, onDismiss: () -> Unit) {
    val (text, progress) = when (stage) {
        is Stage.ExtractingAudio -> "Extracting audio…" to stage.progress
        Stage.Transcribing -> "Transcribing speech…" to null
        is Stage.Exporting -> "Exporting video… ${(stage.progress * 100).toInt()}%" to stage.progress
        is Stage.Error -> stage.message to null
        else -> return
    }
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        colors = if (stage is Stage.Error) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (stage is Stage.Error) {
                TextButton(onClick = onDismiss) { Text("OK") }
            } else {
                TextButton(onClick = onCancel) { Text("Cancel") }
            }
        }
        if (stage.isBusy) {
            if (progress != null) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun EditorTabs(state: CaptionUiState, vm: CaptionViewModel, onSettings: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("Captions", "Style", "Export")
    PrimaryTabRow(selectedTabIndex = tab) {
        tabs.forEachIndexed { i, title ->
            Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
        }
    }
    Box(Modifier.fillMaxWidth().height(300.dp).padding(top = 12.dp)) {
        when (tab) {
            0 -> CaptionsTab(state, vm, onSettings)
            1 -> StylePicker(state.style, state.layout, vm::selectStyle, vm::updateLayout)
            else -> ExportTab(state, vm)
        }
    }
}

@Composable
private fun CaptionsTab(state: CaptionUiState, vm: CaptionViewModel, onSettings: () -> Unit) {
    var editing by remember { mutableStateOf<CaptionLine?>(null) }

    if (state.lines.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Captions are made from your video's real audio, then written in Hinglish.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            if (state.settings.apiKey.isBlank()) {
                Button(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Text("Add API key to start") }
            } else {
                Button(
                    onClick = vm::generateCaptions,
                    enabled = !state.stage.isBusy,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("Generate Hinglish captions") }
            }
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${state.lines.size} lines · tap a line to fix it",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = vm::generateCaptions, enabled = !state.stage.isBusy) { Text("Re-transcribe") }
            }
        }
        itemsIndexed(state.lines, key = { i, l -> "$i-${l.startMs}" }) { _, line ->
            Row(
                Modifier.fillMaxWidth().clickable { editing = line }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    formatTime(line.startMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(56.dp),
                )
                Text(line.text, style = MaterialTheme.typography.bodyLarge)
            }
            HorizontalDivider()
        }
    }

    editing?.let { line ->
        var text by remember(line) { mutableStateOf(line.text) }
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Edit caption · ${formatTime(line.startMs)}") },
            text = {
                Column {
                    OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth())
                    val heard = line.words.joinToString(" ") { it.original }
                    if (heard != line.text) {
                        Spacer(Modifier.height(8.dp))
                        Text("Heard as: $heard", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.editLine(line, text)
                    editing = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ExportTab(state: CaptionUiState, vm: CaptionViewModel) {
    val context = LocalContext.current
    val needsStoragePermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.export()
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Burns the ${state.style.name} captions into the video (H.264 MP4, original audio) and saves it to Movies/HinglishCaptions.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (needsStoragePermission) permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else vm.export()
            },
            enabled = state.lines.isNotEmpty() && !state.stage.isBusy,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text(if (state.lines.isEmpty()) "Generate captions first" else "Export video with captions") }

        val stage = state.stage
        if (stage is Stage.Exported) {
            Spacer(Modifier.height(16.dp))
            Text("Saved to your gallery ✓", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "video/mp4"
                    putExtra(Intent.EXTRA_STREAM, stage.uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(send, "Share video"))
            }) { Text("Share to Instagram / YouTube / WhatsApp") }
        }
    }
}

@Composable
private fun SettingsDialog(
    current: SttSettings,
    keyFor: (SttProvider) -> String,
    onDismiss: () -> Unit,
    onSave: (SttSettings) -> Unit,
) {
    var provider by remember { mutableStateOf(current.provider) }
    var key by remember { mutableStateOf(current.apiKey) }
    var autoDetect by remember { mutableStateOf(current.language.isBlank()) }
    var vocab by remember { mutableStateOf(current.vocabularyHint) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Speech-to-text") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SttProvider.entries.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = provider == p, onClick = {
                            provider = p
                            key = keyFor(p)
                        }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = provider == p, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(p.label)
                    }
                }
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("API key") },
                    placeholder = { Text(provider.keyHint) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text("Spoken language", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth().selectable(selected = !autoDetect, onClick = { autoDetect = false }), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !autoDetect, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Hindi / Hinglish (recommended)")
                }
                Row(Modifier.fillMaxWidth().selectable(selected = autoDetect, onClick = { autoDetect = true }), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = autoDetect, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Auto-detect")
                }
                OutlinedTextField(
                    value = vocab,
                    onValueChange = { vocab = it },
                    label = { Text("Spelling hints (optional)") },
                    placeholder = { Text("Names, brands: Zomato, Rahul, CRED") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                Text(
                    "Your key is stored only on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(SttSettings(provider, key.trim(), if (autoDetect) "" else "hi", vocab.trim()))
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    return "%d:%02d.%d".format(totalSec / 60, totalSec % 60, (ms % 1000) / 100)
}
