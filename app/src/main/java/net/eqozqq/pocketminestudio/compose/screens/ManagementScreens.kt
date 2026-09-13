package net.eqozqq.pocketminestudio.compose.screens

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.eqozqq.pocketminestudio.R
import net.eqozqq.pocketminestudio.SchedulerActivity.ScheduledEvent
import net.eqozqq.pocketminestudio.ServerFragment
import net.eqozqq.pocketminestudio.ServerUtils
import net.eqozqq.pocketminestudio.compose.theme.PillShape
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePlayersScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var currentTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var playersList by remember { mutableStateOf<List<String>>(emptyList()) }
    var whitelistList by remember { mutableStateOf<List<String>>(emptyList()) }
    var showAddWhitelistDialog by remember { mutableStateOf(false) }

    var selectedPlayerForAction by remember { mutableStateOf<String?>(null) }
    var actionDialogType by remember { mutableStateOf<String?>(null) }
    var actionReasonInput by remember { mutableStateOf("") }

    fun loadPlayers() {
        val players = mutableListOf<String>()
        try {
            val playersDir = File(ServerUtils.getAppDirectory(), "players")
            if (playersDir.exists() && playersDir.isDirectory) {
                playersDir.listFiles()?.forEach { f ->
                    val name = f.nameWithoutExtension
                    if (name.isNotEmpty() && !players.contains(name)) {
                        players.add(name)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        ServerFragment.players?.forEach { p ->
            val clean = p.trim().replace(Regex("§[0-9a-fk-or]"), "")
            if (clean.isNotEmpty() && !players.contains(clean)) {
                players.add(clean)
            }
        }
        playersList = players.sorted()
    }

    fun loadWhitelist() {
        val list = mutableListOf<String>()
        try {
            val file = File(ServerUtils.getDataDirectory(), "white-list.txt")
            if (file.exists()) {
                BufferedReader(FileReader(file)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val trimmed = line?.trim() ?: continue
                        if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                            list.add(trimmed)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        whitelistList = list.sorted()
    }

    LaunchedEffect(currentTab) {
        if (currentTab == 0) loadPlayers() else loadWhitelist()
    }

    fun isOnline(player: String): Boolean {
        return ServerFragment.players?.any {
            it.trim().replace(Regex("§[0-9a-fk-or]"), "").equals(player, ignoreCase = true)
        } ?: false
    }

    Scaffold(
        floatingActionButtonPosition = FabPosition.Center,
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
                    title = {
                        Text(
                            text = if (currentTab == 0) context.getString(R.string.server_players)
                            else context.getString(R.string.title_activity_whitelist),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { if (currentTab == 0) loadPlayers() else loadWhitelist() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (currentTab == 1) {
                ExtendedFloatingActionButton(
                    onClick = { showAddWhitelistDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add to Whitelist") },
                    shape = PillShape,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.navigationBarsPadding().padding(bottom = 16.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val tabs = listOf(
                context.getString(R.string.server_players),
                context.getString(R.string.title_activity_whitelist)
            )
            Surface(
                shape = PillShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabs.forEachIndexed { index, title ->
                        val selected = currentTab == index
                        Surface(
                            onClick = { currentTab = index },
                            shape = PillShape,
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            val displayList = if (currentTab == 0) {
                playersList.filter { searchQuery.isEmpty() || it.contains(searchQuery, ignoreCase = true) }
            } else {
                whitelistList.filter { searchQuery.isEmpty() || it.contains(searchQuery, ignoreCase = true) }
            }

            if (displayList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (currentTab == 0) "No players found" else "Whitelist is empty",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayList, key = { it }) { player ->
                        val online = if (currentTab == 0) isOnline(player) else false
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectedPlayerForAction = player },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                if (currentTab == 0) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (online) Color(0xFF00E676) else Color.Gray)
                                    )
                                }
                                Text(
                                    text = player,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedPlayerForAction != null) {
        val player = selectedPlayerForAction!!
        ModalBottomSheet(
            onDismissRequest = { selectedPlayerForAction = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Player: $player",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider()

                if (currentTab == 0) {
                    ListItem(
                        headlineContent = { Text("Kick Player") },
                        leadingContent = { Icon(Icons.Default.ExitToApp, contentDescription = null) },
                        modifier = Modifier.clickable {
                            selectedPlayerForAction = null
                            actionDialogType = "kick"
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Ban / Pardon") },
                        leadingContent = { Icon(Icons.Default.Block, contentDescription = null) },
                        modifier = Modifier.clickable {
                            selectedPlayerForAction = null
                            actionDialogType = "ban"
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Give OP") },
                        leadingContent = { Icon(Icons.Default.Star, contentDescription = null) },
                        modifier = Modifier.clickable {
                            selectedPlayerForAction = null
                            ServerUtils.executeCMD("op \"$player\"")
                            Toast.makeText(context, "Sent op $player", Toast.LENGTH_SHORT).show()
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Take OP") },
                        leadingContent = { Icon(Icons.Default.StarOutline, contentDescription = null) },
                        modifier = Modifier.clickable {
                            selectedPlayerForAction = null
                            ServerUtils.executeCMD("deop \"$player\"")
                            Toast.makeText(context, "Sent deop $player", Toast.LENGTH_SHORT).show()
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Add to Whitelist") },
                        leadingContent = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                        modifier = Modifier.clickable {
                            selectedPlayerForAction = null
                            ServerUtils.executeCMD("whitelist add \"$player\"")
                            Toast.makeText(context, "Sent whitelist add $player", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    ListItem(
                        headlineContent = { Text("Remove from Whitelist") },
                        leadingContent = { Icon(Icons.Default.Delete, contentDescription = null) },
                        modifier = Modifier.clickable {
                            selectedPlayerForAction = null
                            ServerUtils.executeCMD("whitelist remove \"$player\"")
                            val file = File(ServerUtils.getDataDirectory(), "white-list.txt")
                            if (file.exists()) {
                                val lines = file.readLines().filter { it.trim() != player }
                                file.writeText(lines.joinToString("\n"))
                            }
                            loadWhitelist()
                        }
                    )
                }
            }
        }
    }

    if (actionDialogType != null) {
        val type = actionDialogType!!
        val player = selectedPlayerForAction ?: ""
        AlertDialog(
            onDismissRequest = { actionDialogType = null },
            title = { Text(if (type == "kick") "Kick $player" else "Ban / Pardon $player") },
            text = {
                OutlinedTextField(
                    value = actionReasonInput,
                    onValueChange = { actionReasonInput = it },
                    label = { Text("Reason (leave empty to pardon)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = actionReasonInput.trim()
                        if (type == "kick") {
                            ServerUtils.executeCMD("kick \"$player\" $reason")
                        } else {
                            if (reason.isEmpty()) {
                                ServerUtils.executeCMD("pardon \"$player\"")
                            } else {
                                ServerUtils.executeCMD("ban \"$player\" $reason")
                            }
                        }
                        actionReasonInput = ""
                        actionDialogType = null
                    }
                ) {
                    Text("Execute")
                }
            },
            dismissButton = {
                TextButton(onClick = { actionDialogType = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddWhitelistDialog) {
        var newPlayer by remember { mutableStateOf("Player") }
        AlertDialog(
            onDismissRequest = { showAddWhitelistDialog = false },
            title = { Text("Add to Whitelist") },
            text = {
                OutlinedTextField(
                    value = newPlayer,
                    onValueChange = { newPlayer = it },
                    label = { Text("Player Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = newPlayer.trim()
                        if (p.isNotEmpty()) {
                            ServerUtils.executeCMD("whitelist add \"$p\"")
                            val file = File(ServerUtils.getDataDirectory(), "white-list.txt")
                            try {
                                FileWriter(file, true).use { fw ->
                                    fw.write("\n$p\n")
                                }
                            } catch (e: Exception) {}
                            loadWhitelist()
                        }
                        showAddWhitelistDialog = false
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWhitelistDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageGridScreen(
    title: String,
    path: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentDir by remember { mutableStateOf(File(path)) }
    var items by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedItemForAction by remember { mutableStateOf<File?>(null) }

    fun loadDir() {
        if (!currentDir.exists()) currentDir.mkdirs()
        val list = currentDir.listFiles()?.toList() ?: emptyList()
        items = list.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
    }

    LaunchedEffect(currentDir) {
        loadDir()
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val dest = File(currentDir, "imported_${System.currentTimeMillis()}.phar")
                    context.contentResolver.openInputStream(it)?.use { input ->
                        FileOutputStream(dest).use { output ->
                            input.copyTo(output)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Imported successfully", Toast.LENGTH_SHORT).show()
                        loadDir()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to import", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Scaffold(
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
                    title = { Text(if (currentDir.absolutePath == path) title else currentDir.name, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (currentDir.absolutePath != path && currentDir.parentFile != null) {
                                currentDir = currentDir.parentFile!!
                            } else {
                                onNavigateBack()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { importLauncher.launch("*/*") }) {
                            Icon(Icons.Default.FileUpload, contentDescription = "Import")
                        }
                        IconButton(onClick = { loadDir() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No items in this directory",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items, key = { it.absolutePath }) { file ->
                    val isDisabled = file.name.endsWith(".disabled")
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.9f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                if (file.isDirectory) {
                                    currentDir = file
                                } else {
                                    selectedItemForAction = file
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDisabled) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (file.isDirectory) Icons.Default.Folder
                                else if (file.name.endsWith(".phar")) Icons.Default.Extension
                                else Icons.Default.InsertDriveFile,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = if (isDisabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = file.name.removeSuffix(".disabled"),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedItemForAction != null) {
        val target = selectedItemForAction!!
        val isDisabled = target.name.endsWith(".disabled")
        AlertDialog(
            onDismissRequest = { selectedItemForAction = null },
            title = { Text(target.name) },
            text = { Text("Choose action for this file:") },
            confirmButton = {
                Button(
                    onClick = {
                        val newFile = if (isDisabled) {
                            File(target.parentFile, target.name.removeSuffix(".disabled"))
                        } else {
                            File(target.parentFile, target.name + ".disabled")
                        }
                        target.renameTo(newFile)
                        selectedItemForAction = null
                        loadDir()
                    }
                ) {
                    Text(if (isDisabled) "Enable" else "Disable")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        target.deleteRecursively()
                        selectedItemForAction = null
                        loadDir()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulerScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("EventScheduler", Context.MODE_PRIVATE) }
    var events by remember { mutableStateOf<List<ScheduledEvent>>(emptyList()) }
    var editingEvent by remember { mutableStateOf<ScheduledEvent?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }

    fun loadEvents() {
        val list = mutableListOf<ScheduledEvent>()
        val jsonStr = prefs.getString("events", "[]") ?: "[]"
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val interval = obj.optLong("intervalMs", 0L).let { if (it <= 0L) 60000L else it }
                list.add(
                    ScheduledEvent(
                        obj.optString("id", UUID.randomUUID().toString()),
                        obj.optString("name", "Event"),
                        obj.optString("action", "restart"),
                        obj.optString("actionData", ""),
                        interval,
                        obj.optBoolean("retryOnFail", false),
                        obj.optBoolean("isEnabled", true),
                        obj.optLong("lastRunTime", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        events = list
    }

    fun saveEvents(updated: List<ScheduledEvent>) {
        events = updated
        val arr = JSONArray()
        for (e in updated) {
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("name", e.name)
            obj.put("action", e.action)
            obj.put("actionData", e.actionData)
            obj.put("intervalMs", e.intervalMs)
            obj.put("retryOnFail", e.retryOnFail)
            obj.put("isEnabled", e.isEnabled)
            obj.put("lastRunTime", e.lastRunTime)
            arr.put(obj)
        }
        prefs.edit().putString("events", arr.toString()).apply()
    }

    LaunchedEffect(Unit) {
        loadEvents()
    }

    Scaffold(
        floatingActionButtonPosition = FabPosition.Center,
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
                    title = { Text("Event Scheduler", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingEvent = ScheduledEvent(UUID.randomUUID().toString(), "Auto Restart", "restart", "say Server restarting...", 3600000L, false, true, System.currentTimeMillis())
                    isCreatingNew = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Event") },
                shape = PillShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.navigationBarsPadding().padding(bottom = 16.dp)
            )
        }
    ) { paddingValues ->
        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No scheduled tasks",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                editingEvent = event
                                isCreatingNew = false
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = event.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                val intervalText = when {
                                    event.intervalMs % 3600000L == 0L -> "${event.intervalMs / 3600000L} hours"
                                    event.intervalMs % 60000L == 0L -> "${event.intervalMs / 60000L} mins"
                                    else -> "${event.intervalMs / 1000L} secs"
                                }
                                Text(
                                    text = "Action: ${event.action} | Every $intervalText",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = event.isEnabled,
                                onCheckedChange = { chk ->
                                    val updated = events.map {
                                        if (it.id == event.id) {
                                            ScheduledEvent(it.id, it.name, it.action, it.actionData, it.intervalMs, it.retryOnFail, chk, it.lastRunTime)
                                        } else it
                                    }
                                    saveEvents(updated)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (editingEvent != null) {
        val ev = editingEvent!!
        var nameInput by remember(ev) { mutableStateOf(ev.name) }
        var actionInput by remember(ev) { mutableStateOf(ev.action) }
        var cmdInput by remember(ev) { mutableStateOf(ev.actionData ?: "") }
        var retryOnFail by remember(ev) { mutableStateOf(ev.retryOnFail) }
        val initialIntervalStr = remember(ev) {
            val ms = ev.intervalMs
            when {
                ms % 3600000L == 0L -> (ms / 3600000L).toString()
                ms % 60000L == 0L -> (ms / 60000L).toString()
                else -> (ms / 1000L).toString()
            }
        }
        var intervalVal by remember(ev) { mutableStateOf(initialIntervalStr) }
        val initialUnitPos = remember(ev) {
            val ms = ev.intervalMs
            when {
                ms % 3600000L == 0L -> 2
                ms % 60000L == 0L -> 1
                else -> 0
            }
        }
        var unitPos by remember(ev) { mutableIntStateOf(initialUnitPos) }

        val actionOptions = listOf("Restart Server", "Stop Server", "Send Command")
        val actionKeys = listOf("restart", "stop", "command")
        val unitOptions = listOf("Seconds", "Minutes", "Hours")

        var actionExpanded by remember { mutableStateOf(false) }
        var unitExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editingEvent = null },
            title = {
                Text(
                    text = if (isCreatingNew) "Create Event" else "Edit Event",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text(context.getString(R.string.auto_hint_event_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenuBox(
                        expanded = actionExpanded,
                        onExpandedChange = { actionExpanded = it }
                    ) {
                        val currentActionIdx = when (actionInput) {
                            "stop" -> 1
                            "command" -> 2
                            else -> 0
                        }
                        OutlinedTextField(
                            value = actionOptions[currentActionIdx],
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(context.getString(R.string.auto_hint_action)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = actionExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = actionExpanded,
                            onDismissRequest = { actionExpanded = false }
                        ) {
                            actionOptions.forEachIndexed { index, option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        actionInput = actionKeys[index]
                                        actionExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = actionInput == "command") {
                        OutlinedTextField(
                            value = cmdInput,
                            onValueChange = { cmdInput = it },
                            label = { Text(context.getString(R.string.auto_hint_command_to_send_with)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = intervalVal,
                            onValueChange = { intervalVal = it },
                            label = { Text(context.getString(R.string.auto_hint_interval)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenuBox(
                            expanded = unitExpanded,
                            onExpandedChange = { unitExpanded = it },
                            modifier = Modifier.weight(1.2f)
                        ) {
                            OutlinedTextField(
                                value = unitOptions[unitPos],
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(context.getString(R.string.auto_hint_unit)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = unitExpanded,
                                onDismissRequest = { unitExpanded = false }
                            ) {
                                unitOptions.forEachIndexed { index, option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            unitPos = index
                                            unitExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { retryOnFail = !retryOnFail }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = context.getString(R.string.auto_text_retry_if_server_is_o),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Switch(
                            checked = retryOnFail,
                            onCheckedChange = { retryOnFail = it }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = intervalVal.toLongOrNull() ?: 60L
                        val multiplier = when (unitPos) {
                            2 -> 3600000L
                            1 -> 60000L
                            else -> 1000L
                        }
                        val finalMs = parsed * multiplier
                        val updatedEvent = ScheduledEvent(ev.id, nameInput.ifEmpty { "Event" }, actionInput, cmdInput, finalMs, retryOnFail, ev.isEnabled, System.currentTimeMillis())
                        val newEvents = if (isCreatingNew) {
                            events + updatedEvent
                        } else {
                            events.map { if (it.id == ev.id) updatedEvent else it }
                        }
                        saveEvents(newEvents)
                        editingEvent = null
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isCreatingNew) {
                        TextButton(
                            onClick = {
                                saveEvents(events.filter { it.id != ev.id })
                                editingEvent = null
                            }
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { editingEvent = null }) {
                        Text("Cancel")
                    }
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}
