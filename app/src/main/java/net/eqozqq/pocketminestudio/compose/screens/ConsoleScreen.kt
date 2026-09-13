package net.eqozqq.pocketminestudio.compose.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import net.eqozqq.pocketminestudio.MacroManager
import net.eqozqq.pocketminestudio.ServerUtils
import net.eqozqq.pocketminestudio.compose.state.ServerState
import net.eqozqq.pocketminestudio.compose.theme.PillShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var commandText by remember { mutableStateOf("") }
    var showMacroDialog by remember { mutableStateOf(false) }
    var macros by remember { mutableStateOf(MacroManager.getMacros(context)) }
    var autoScroll by remember { mutableStateOf(true) }

    val logs = ServerState.consoleLogs.toList()

    LaunchedEffect(logs.size) {
        if (autoScroll && logs.isNotEmpty()) {
            try {
                listState.scrollToItem((logs.size - 1).coerceAtLeast(0))
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = 3.dp,
                tonalElevation = 2.dp
            ) {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    title = { Text("Console", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { autoScroll = !autoScroll }) {
                            Icon(
                                imageVector = if (autoScroll) Icons.Rounded.VerticalAlignBottom else Icons.Rounded.Pause,
                                contentDescription = "Auto-scroll",
                                tint = if (autoScroll) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val fullLog = logs.joinToString("\n")
                                cm.setPrimaryClip(ClipData.newPlainText("Console Log", fullLog))
                                Toast.makeText(context, "Console logs copied", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy")
                        }
                        IconButton(onClick = { ServerState.clearLogs() }) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear")
                        }
                    }
                )
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        macros.forEach { macro ->
                            SuggestionChip(
                                onClick = {
                                    val space = if (commandText.isEmpty() || commandText.endsWith(" ")) "" else " "
                                    commandText = "$commandText$space$macro "
                                },
                                label = { Text(macro, style = MaterialTheme.typography.labelSmall) },
                                shape = PillShape
                            )
                        }
                        IconButton(
                            onClick = { showMacroDialog = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Manage Macros", modifier = Modifier.size(18.dp))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commandText,
                            onValueChange = { commandText = it },
                            placeholder = { Text("Enter command...", style = MaterialTheme.typography.bodyMedium) },
                            modifier = Modifier.weight(1f),
                            shape = PillShape,
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        FilledIconButton(
                            onClick = {
                                if (commandText.isNotBlank()) {
                                    ServerUtils.executeCMD(commandText.trim())
                                    commandText = ""
                                }
                            },
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Console is empty",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    items(
                        count = logs.size,
                        key = { index -> index.toString() + "_" + (logs.getOrNull(index)?.hashCode() ?: 0) }
                    ) { index ->
                        val rawLine = logs.getOrNull(index) ?: return@items
                        val cleanLine = rawLine.replace("\u001B\\[[;\\d]*m".toRegex(), "")
                            .replace("\u001B\\][^\u0007]*\u0007".toRegex(), "")

                        val textColor = when {
                            cleanLine.contains("[ERROR]") || cleanLine.contains("CRITICAL") -> MaterialTheme.colorScheme.error
                            cleanLine.contains("[WARNING]") -> Color(0xFFFFA000)
                            cleanLine.contains("[NOTICE]") || cleanLine.contains("[INFO]") -> Color(0xFF2E7D32)
                            cleanLine.contains("[DEBUG]") -> Color(0xFF1976D2)
                            else -> MaterialTheme.colorScheme.onBackground
                        }

                        Text(
                            text = cleanLine,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.5.sp,
                            color = textColor,
                            lineHeight = 17.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("Log line", cleanLine))
                                    Toast.makeText(context, "Line copied", Toast.LENGTH_SHORT).show()
                                }
                        )
                    }
                }
            }

            val isAtBottom by remember {
                derivedStateOf {
                    val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    lastVisible >= (logs.size - 2).coerceAtLeast(0)
                }
            }

            AnimatedVisibility(
                visible = !isAtBottom && logs.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 12.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                listState.animateScrollToItem((logs.size - 1).coerceAtLeast(0))
                            } catch (_: Exception) {}
                        }
                    },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "Scroll to bottom", modifier = Modifier.size(18.dp))
                }
            }
        }

        if (showMacroDialog) {
            var newMacroText by remember { mutableStateOf("help") }
            AlertDialog(
                onDismissRequest = { showMacroDialog = false },
                title = { Text("Manage Macros") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newMacroText,
                                onValueChange = { newMacroText = it },
                                placeholder = { Text("e.g. status, list, op") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newMacroText.isNotBlank()) {
                                        val list = MacroManager.getMacros(context).toMutableList()
                                        list.add(newMacroText.trim())
                                        MacroManager.saveMacros(context, list)
                                        macros = list
                                        newMacroText = ""
                                    }
                                },
                                shape = PillShape
                            ) {
                                Text("Add")
                            }
                        }

                        LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                            items(macros.size) { i ->
                                val m = macros[i]
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(m, style = MaterialTheme.typography.bodyMedium)
                                    IconButton(
                                        onClick = {
                                            val list = MacroManager.getMacros(context).toMutableList()
                                            list.remove(m)
                                            MacroManager.saveMacros(context, list)
                                            macros = list
                                        }
                                    ) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMacroDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}
