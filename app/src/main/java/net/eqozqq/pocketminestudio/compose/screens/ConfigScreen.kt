package net.eqozqq.pocketminestudio.compose.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.eqozqq.pocketminestudio.R
import net.eqozqq.pocketminestudio.ServerUtils
import net.eqozqq.pocketminestudio.compose.theme.CardShape
import net.eqozqq.pocketminestudio.compose.theme.PillShape
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.PrintWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var serverName by remember { mutableStateOf("Minecraft: PE Server") }
    var description by remember { mutableStateOf("Server made using PocketMine-MP") }
    var motd by remember { mutableStateOf("Welcome @player to server!") }
    var language by remember { mutableStateOf("eng") }

    var serverPort by remember { mutableStateOf("19132") }
    var enableIpv6 by remember { mutableStateOf(true) }
    var serverPortV6 by remember { mutableStateOf("19133") }
    var enableQuery by remember { mutableStateOf(true) }
    var enableRcon by remember { mutableStateOf(false) }
    var rconPassword by remember { mutableStateOf("secret") }

    var gamemode by remember { mutableStateOf("0") }
    var difficulty by remember { mutableStateOf("1") }
    var forceGamemode by remember { mutableStateOf(false) }
    var hardcore by remember { mutableStateOf(false) }
    var pvp by remember { mutableStateOf(true) }
    var spawnAnimals by remember { mutableStateOf(true) }
    var spawnMobs by remember { mutableStateOf(true) }
    var allowFlight by remember { mutableStateOf(false) }

    var levelName by remember { mutableStateOf("world") }
    var levelSeed by remember { mutableStateOf("12345") }
    var levelType by remember { mutableStateOf("DEFAULT") }
    var generatorSettings by remember { mutableStateOf("flat") }
    var maxPlayers by remember { mutableStateOf("20") }
    var spawnProtectionEnabled by remember { mutableStateOf(true) }
    var spawnProtection by remember { mutableStateOf("16") }
    var viewDistance by remember { mutableFloatStateOf(10f) }
    var ramLimit by remember { mutableStateOf("256") }
    var autoSave by remember { mutableStateOf(true) }

    var xboxAuth by remember { mutableStateOf(true) }
    var whitelist by remember { mutableStateOf(false) }
    var serverType by remember { mutableStateOf("normal") }
    var lastUpdate by remember { mutableStateOf(false) }
    var announceAchievements by remember { mutableStateOf(true) }

    var showCustomRamDialog by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }

    fun loadConfig() {
        val file = File(ServerUtils.getDataDirectory(), "server.properties")
        if (!file.exists()) return
        val map = mutableMapOf<String, String>()
        try {
            BufferedReader(FileReader(file)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val l = line?.trim() ?: continue
                    if (l.startsWith("#") || !l.contains("=")) continue
                    val idx = l.indexOf("=")
                    val k = l.substring(0, idx).trim()
                    val v = l.substring(idx + 1).trim()
                    map[k] = v
                }
            }

            map["server-name"]?.let { serverName = it }
            map["description"]?.let { description = it }
            map["motd"]?.let { motd = it }
            map["language"]?.let { language = it }

            map["server-port"]?.let { serverPort = it }
            map["enable-ipv6"]?.let { enableIpv6 = it.equals("on", ignoreCase = true) || it.equals("true", ignoreCase = true) }
            map["server-portv6"]?.let { serverPortV6 = it }
            map["enable-query"]?.let { enableQuery = it.equals("on", ignoreCase = true) || it.equals("true", ignoreCase = true) }
            map["enable-rcon"]?.let { enableRcon = it.equals("on", ignoreCase = true) || it.equals("true", ignoreCase = true) }
            map["rcon.password"]?.let { rconPassword = it }

            map["gamemode"]?.let { gamemode = it }
            map["difficulty"]?.let { difficulty = it }
            map["force-gamemode"]?.let { forceGamemode = it.equals("on", ignoreCase = true) || it.equals("true", ignoreCase = true) }
            map["hardcore"]?.let { hardcore = it.equals("on", ignoreCase = true) || it.equals("true", ignoreCase = true) }
            map["pvp"]?.let { pvp = it.equals("on", ignoreCase = true) || it.equals("true", ignoreCase = true) }
            map["spawn-animals"]?.let { spawnAnimals = it.equals("on", ignoreCase = true) || it.equals("true", ignoreCase = true) }
            map["spawn-mobs"]?.let { spawnMobs = it.equals("on", ignoreCase = true) || it.equals("true", ignoreCase = true) }
            map["allow-flight"]?.let { allowFlight = it.equals("on", ignoreCase = true) || it.equals("true", ignoreCase = true) }

            map["level-name"]?.let { levelName = it }
            map["level-seed"]?.let { levelSeed = it }
            map["level-type"]?.let { levelType = it }
            map["generator-settings"]?.let { generatorSettings = it }
            map["max-players"]?.let { maxPlayers = it }
            map["spawn-protection"]?.let {
                val sp = it.toIntOrNull() ?: 16
                if (sp == -1) {
                    spawnProtectionEnabled = false
                    spawnProtection = "16"
                } else {
                    spawnProtectionEnabled = true
                    spawnProtection = sp.toString()
                }
            }
            map["view-distance"]?.toFloatOrNull()?.let { viewDistance = it }
            map["memory-limit"]?.let {
                var clean = it.replace("M", "").replace("m", "").replace("G", "").replace("g", "").trim()
                if (it.endsWith("G", true) || it.endsWith("g", true)) {
                    val g = clean.toIntOrNull() ?: 1
                    clean = (g * 1024).toString()
                }
                ramLimit = clean
            }
            map["auto-save"]?.let { autoSave = it.equals("on", ignoreCase = true) || it.equals("true", ignoreCase = true) }

            map["xbox-auth"]?.let { xboxAuth = it.equals("on", ignoreCase = true) || it.equals("true", ignoreCase = true) }
            map["white-list"]?.let { whitelist = it.equals("on", ignoreCase = true) || it.equals("true", ignoreCase = true) }
            map["server-type"]?.let { serverType = it }
            map["last-update"]?.let { lastUpdate = it.equals("on", ignoreCase = true) || it.equals("true", ignoreCase = true) }
            map["announce-player-achievements"]?.let { announceAchievements = it.equals("on", ignoreCase = true) || it.equals("true", ignoreCase = true) }
        } catch (_: Exception) {}
    }

    LaunchedEffect(Unit) {
        loadConfig()
    }

    fun saveConfig() {
        coroutineScope.launch(Dispatchers.IO) {
            val file = File(ServerUtils.getDataDirectory(), "server.properties")
            try {
                PrintWriter(file).use { writer ->
                    writer.println("# PocketMine-MP Server Properties")
                    writer.println("language=$language")
                    writer.println("server-name=$serverName")
                    writer.println("description=$description")
                    writer.println("motd=$motd")
                    writer.println("server-port=$serverPort")
                    writer.println("enable-ipv6=${if (enableIpv6) "on" else "off"}")
                    writer.println("server-portv6=$serverPortV6")
                    writer.println("enable-query=${if (enableQuery) "on" else "off"}")
                    writer.println("enable-rcon=${if (enableRcon) "on" else "off"}")
                    writer.println("rcon.password=$rconPassword")
                    writer.println("gamemode=$gamemode")
                    writer.println("difficulty=$difficulty")
                    writer.println("force-gamemode=${if (forceGamemode) "on" else "off"}")
                    writer.println("hardcore=${if (hardcore) "on" else "off"}")
                    writer.println("pvp=${if (pvp) "on" else "off"}")
                    writer.println("spawn-animals=${if (spawnAnimals) "on" else "off"}")
                    writer.println("spawn-mobs=${if (spawnMobs) "on" else "off"}")
                    writer.println("allow-flight=${if (allowFlight) "on" else "off"}")
                    writer.println("level-name=$levelName")
                    writer.println("level-seed=$levelSeed")
                    writer.println("level-type=$levelType")
                    writer.println("generator-settings=$generatorSettings")
                    writer.println("max-players=$maxPlayers")
                    writer.println("spawn-protection=${if (spawnProtectionEnabled) spawnProtection else "-1"}")
                    writer.println("view-distance=${viewDistance.toInt()}")
                    writer.println("memory-limit=${ramLimit}M")
                    writer.println("auto-save=${if (autoSave) "on" else "off"}")
                    writer.println("xbox-auth=${if (xboxAuth) "on" else "off"}")
                    writer.println("white-list=${if (whitelist) "on" else "off"}")
                    writer.println("server-type=$serverType")
                    writer.println("last-update=${if (lastUpdate) "on" else "off"}")
                    writer.println("announce-player-achievements=${if (announceAchievements) "on" else "off"}")
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.config_save) + ": OK", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to save configuration", Toast.LENGTH_SHORT).show()
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
                    title = {
                        Text(
                            text = context.getString(R.string.auto_title_options),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = { loadConfig() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload")
                        }
                        Button(
                            onClick = { saveConfig() },
                            shape = PillShape,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(context.getString(R.string.config_save))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ConfigGroup {
                OutlinedTextField(
                    value = serverName,
                    onValueChange = { serverName = it },
                    label = { Text(context.getString(R.string.config_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = motd,
                    onValueChange = { motd = it },
                    label = { Text(context.getString(R.string.config_motd)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(context.getString(R.string.config_desc)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            ConfigGroup {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = serverPort,
                        onValueChange = { serverPort = it },
                        label = { Text(context.getString(R.string.config_port)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = serverPortV6,
                        onValueChange = { serverPortV6 = it },
                        label = { Text("Port (IPv6)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                ConfigSwitchRow(
                    title = "Enable IPv6",
                    checked = enableIpv6,
                    onCheckedChange = { enableIpv6 = it }
                )
                ConfigSwitchRow(
                    title = context.getString(R.string.config_query),
                    checked = enableQuery,
                    onCheckedChange = { enableQuery = it }
                )
                ConfigSwitchRow(
                    title = context.getString(R.string.config_rcon),
                    checked = enableRcon,
                    onCheckedChange = { enableRcon = it }
                )
                if (enableRcon) {
                    OutlinedTextField(
                        value = rconPassword,
                        onValueChange = { rconPassword = it },
                        label = { Text(context.getString(R.string.config_rcon_password)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            ConfigGroup {
                Text(
                    text = context.getString(R.string.config_gamemode),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val modes = listOf(
                        "0" to context.getString(R.string.gamemode_survival),
                        "1" to context.getString(R.string.gamemode_creative),
                        "2" to context.getString(R.string.gamemode_adventure),
                        "3" to context.getString(R.string.gamemode_spectator)
                    )
                    modes.forEach { (id, label) ->
                        FilterChip(
                            selected = gamemode == id,
                            onClick = { gamemode = id },
                            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                            shape = PillShape
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = context.getString(R.string.config_difficulty),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val diffs = listOf(
                        "0" to context.getString(R.string.difficulty_peaceful),
                        "1" to context.getString(R.string.difficulty_easy),
                        "2" to context.getString(R.string.difficulty_normal),
                        "3" to context.getString(R.string.difficulty_hard)
                    )
                    diffs.forEach { (id, label) ->
                        FilterChip(
                            selected = difficulty == id,
                            onClick = { difficulty = id },
                            label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                            shape = PillShape
                        )
                    }
                }

                ConfigSwitchRow(
                    title = "Force Gamemode",
                    checked = forceGamemode,
                    onCheckedChange = { forceGamemode = it }
                )
                ConfigSwitchRow(
                    title = "Hardcore Mode",
                    checked = hardcore,
                    onCheckedChange = { hardcore = it }
                )
                ConfigSwitchRow(
                    title = context.getString(R.string.config_pvp),
                    checked = pvp,
                    onCheckedChange = { pvp = it }
                )
                ConfigSwitchRow(
                    title = context.getString(R.string.config_fly),
                    checked = allowFlight,
                    onCheckedChange = { allowFlight = it }
                )
            }

            ConfigGroup {
                OutlinedTextField(
                    value = maxPlayers,
                    onValueChange = { maxPlayers = it },
                    label = { Text(context.getString(R.string.config_players)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = context.getString(R.string.config_distance),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${viewDistance.toInt()} chunks",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = viewDistance,
                        onValueChange = { viewDistance = it },
                        valueRange = 3f..16f,
                        steps = 12
                    )
                }

                Column {
                    Text(
                        text = context.getString(R.string.config_ram),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    val rams = listOf("128", "256", "512", "1024")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        rams.forEach { r ->
                            FilterChip(
                                selected = ramLimit == r,
                                onClick = { ramLimit = r },
                                label = { Text("${r}M") },
                                shape = PillShape
                            )
                        }
                        FilterChip(
                            selected = !rams.contains(ramLimit),
                            onClick = { showCustomRamDialog = true },
                            label = { Text(if (!rams.contains(ramLimit)) "${ramLimit}M" else "Custom") },
                            shape = PillShape
                        )
                    }
                    if (!rams.contains(ramLimit)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = ramLimit,
                            onValueChange = { ramLimit = it.filter { c -> c.isDigit() } },
                            label = { Text("RAM (MB)") },
                            singleLine = true,
                            modifier = Modifier.width(100.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                ConfigSwitchRow(
                    title = context.getString(R.string.config_spawnprotect),
                    checked = spawnProtectionEnabled,
                    onCheckedChange = { spawnProtectionEnabled = it }
                )
                if (spawnProtectionEnabled) {
                    OutlinedTextField(
                        value = spawnProtection,
                        onValueChange = { spawnProtection = it },
                        label = { Text("Protection Radius (blocks)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                ConfigSwitchRow(
                    title = context.getString(R.string.config_auto_save),
                    checked = autoSave,
                    onCheckedChange = { autoSave = it }
                )

                ConfigSwitchRow(
                    title = "Xbox Live Auth",
                    checked = xboxAuth,
                    onCheckedChange = { xboxAuth = it }
                )
                ConfigSwitchRow(
                    title = context.getString(R.string.config_whitelist),
                    checked = whitelist,
                    onCheckedChange = { whitelist = it }
                )
            }

            Surface(
                onClick = { showAdvanced = !showAdvanced },
                shape = CardShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SettingsApplications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = context.getString(R.string.config_advanced),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Icon(
                        imageVector = if (showAdvanced) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
            }

            AnimatedVisibility(visible = showAdvanced) {
                ConfigGroup {
                    OutlinedTextField(
                        value = levelName,
                        onValueChange = { levelName = it },
                        label = { Text(context.getString(R.string.config_level_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = levelSeed,
                        onValueChange = { levelSeed = it },
                        label = { Text(context.getString(R.string.config_level_seed)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = levelType,
                        onValueChange = { levelType = it },
                        label = { Text(context.getString(R.string.config_level_type)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = generatorSettings,
                        onValueChange = { generatorSettings = it },
                        label = { Text(context.getString(R.string.conf_generator_setting)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = serverType,
                        onValueChange = { serverType = it },
                        label = { Text("Server Type") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ConfigSwitchRow(
                        title = "Spawn Animals",
                        checked = spawnAnimals,
                        onCheckedChange = { spawnAnimals = it }
                    )
                    ConfigSwitchRow(
                        title = "Spawn Mobs",
                        checked = spawnMobs,
                        onCheckedChange = { spawnMobs = it }
                    )
                    ConfigSwitchRow(
                        title = context.getString(R.string.config_achievements),
                        checked = announceAchievements,
                        onCheckedChange = { announceAchievements = it }
                    )
                    ConfigSwitchRow(
                        title = "Check for Updates",
                        checked = lastUpdate,
                        onCheckedChange = { lastUpdate = it }
                    )
                }
            }

            Spacer(modifier = Modifier.height(140.dp))
        }
    }

    if (showCustomRamDialog) {
        var ramInput by remember { mutableStateOf(ramLimit) }
        AlertDialog(
            onDismissRequest = { showCustomRamDialog = false },
            title = { Text(context.getString(R.string.auto_java_custom)) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    OutlinedTextField(
                        value = ramInput,
                        onValueChange = { ramInput = it.filter { c -> c.isDigit() } },
                        label = { Text("RAM in MB") },
                        singleLine = true,
                        modifier = Modifier.width(110.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = ramInput.trim().toIntOrNull()
                        if (num != null && num > 0) {
                            ramLimit = num.toString()
                        }
                        showCustomRamDialog = false
                    },
                    shape = PillShape
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomRamDialog = false }) {
                    Text(context.getString(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
private fun ConfigGroup(
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

@Composable
private fun ConfigSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
