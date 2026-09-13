package net.eqozqq.pocketminestudio.compose.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import net.eqozqq.pocketminestudio.About
import net.eqozqq.pocketminestudio.R
import net.eqozqq.pocketminestudio.ServerFragment
import net.eqozqq.pocketminestudio.ServerService
import net.eqozqq.pocketminestudio.ServerUtils
import net.eqozqq.pocketminestudio.compose.state.ServerState
import net.eqozqq.pocketminestudio.compose.theme.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ServerScreen(
    onNavigateToConsole: () -> Unit,
    onNavigateToPlayers: () -> Unit,
    onNavigateToWhitelist: () -> Unit,
    onNavigateToWorlds: () -> Unit,
    onNavigateToPlugins: () -> Unit,
    onNavigateToVersions: () -> Unit,
    onNavigateToScheduler: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    var showMenu by remember { mutableStateOf(false) }
    var showAppSettingsDialog by remember { mutableStateOf(false) }
    var serverDisplayName by remember { mutableStateOf("PocketMine-MP Server") }

    val isRunning = ServerState.isStarted

    val startButtonColor by animateColorAsState(
        targetValue = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        label = "btnColor"
    )

    LaunchedEffect(Unit) {
        ServerState.localIp = ServerFragment.getIPAddress(true)
        ServerState.isStarted = ServerUtils.isRunning()

        withContext(Dispatchers.IO) {
            val propFile = File(ServerUtils.getDataDirectory(), "server.properties")
            if (propFile.exists()) {
                try {
                    propFile.forEachLine { line ->
                        val l = line.trim()
                        if (l.startsWith("server-name=")) {
                            val name = l.substring(12).trim()
                            if (name.isNotEmpty()) {
                                serverDisplayName = name
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            val providers = listOf(
                "https://api.ipify.org",
                "https://icanhazip.com",
                "https://ifconfig.me/ip",
                "https://checkip.amazonaws.com"
            )
            for (urlStr in providers) {
                try {
                    val conn = URL(urlStr).openConnection() as HttpURLConnection
                    conn.connectTimeout = 3500
                    conn.readTimeout = 3500
                    conn.setRequestProperty("User-Agent", "curl/7.68.0")
                    if (conn.responseCode == 200) {
                        val ip = conn.inputStream.bufferedReader().readText().trim()
                        if (ip.isNotEmpty() && ip.matches(Regex("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"))) {
                            withContext(Dispatchers.Main) {
                                ServerFragment.publicIpString = ip
                                ServerState.publicIp = ip
                            }
                            break
                        }
                    }
                } catch (_: Exception) {}
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
                    navigationIcon = {
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_more_vert),
                                    contentDescription = "Menu"
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(context.getString(R.string.auto_java_app_settings)) },
                                    onClick = {
                                        showMenu = false
                                        showAppSettingsDialog = true
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_settings_24px),
                                            contentDescription = null
                                        )
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Kill Server",
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        ServerUtils.stopServer()
                                        ServerState.isStarted = false
                                        ServerFragment.isStarted = false
                                        context.stopService(Intent(context, ServerService::class.java))
                                    },
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_dangerous_24px),
                                            contentDescription = null,
                                        )
                                    }
                                )
                            }
                        }
                    },
                    title = {
                        Text(
                            text = "PocketMine-Studio",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    },
                    actions = {
                        IconButton(onClick = onNavigateToConsole) {
                            Icon(
                                painter = painterResource(R.drawable.ic_terminal_2_24px),
                                contentDescription = "Console"
                            )
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CardShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (isRunning) {
                                ServerState.isStarted = false
                                ServerFragment.isStarted = false
                                context.stopService(Intent(context, ServerService::class.java))
                                ServerUtils.executeCMD("stop")
                            } else {
                                ServerState.isStarted = true
                                ServerFragment.isStarted = true
                                ServerUtils.runServer()
                                if (prefs.getBoolean("open_console_on_start", true)) {
                                    onNavigateToConsole()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = PillShape,
                        colors = ButtonDefaults.buttonColors(containerColor = startButtonColor)
                    ) {
                        Icon(
                            painter = painterResource(if (isRunning) R.drawable.ic_stop_circle_24px else R.drawable.ic_play_circle_24px),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRunning) "Stop Server" else "Start Server",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            if (isRunning) {
                                ServerUtils.executeCMD("stop")
                                ServerUtils.runServer()
                                Toast.makeText(context, context.getString(R.string.auto_text_restart), Toast.LENGTH_SHORT).show()
                            } else {
                                ServerState.isStarted = true
                                ServerFragment.isStarted = true
                                ServerUtils.runServer()
                            }
                            if (prefs.getBoolean("open_console_on_start", true)) {
                                onNavigateToConsole()
                            }
                        },
                        modifier = Modifier.height(54.dp),
                        shape = PillShape
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_replay_24px),
                            contentDescription = "Restart",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Surface(
                shape = CardShape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = PillShape,
                            color = if (isRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = if (isRunning) "TPS: ${ServerState.tps}" else "Offline",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isRunning) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .combinedClickable(
                                onClick = {},
                                onLongClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    clipboard?.setPrimaryClip(ClipData.newPlainText("Local IP", ServerState.localIp))
                                    Toast.makeText(context, context.getString(R.string.auto_java_ip_copied), Toast.LENGTH_SHORT).show()
                                }
                            )
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Local IP", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = ServerState.localIp, fontWeight = FontWeight.Medium)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .combinedClickable(
                                onClick = {
                                    ServerState.isPublicIpBlurred = !ServerState.isPublicIpBlurred
                                },
                                onLongClick = {
                                    val ipVal = if (ServerState.publicIp != "Loading..." && ServerState.publicIp != "Unknown") ServerState.publicIp else (ServerFragment.publicIpString ?: "")
                                    if (ipVal.isNotEmpty() && ipVal != "Loading...") {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        clipboard?.setPrimaryClip(ClipData.newPlainText("Public IP", ipVal))
                                        Toast.makeText(context, context.getString(R.string.auto_java_public_ip_copied), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Public IP", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val ipVal = if (ServerState.publicIp != "Loading..." && ServerState.publicIp != "Unknown") ServerState.publicIp else (ServerFragment.publicIpString ?: "Loading...")
                            Text(
                                text = if (ServerState.isPublicIpBlurred) "••••••••••••" else ipVal,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = if (ServerState.isPublicIpBlurred) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Online", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val onlineDisplay = if (isRunning) {
                            val p = ServerState.onlinePlayers
                            if (p.contains("unknown", ignoreCase = true) || p.isBlank()) "0/0" else p
                        } else {
                            "0"
                        }
                        Text(
                            text = onlineDisplay,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    ServerMiniChart(
                        title = "RAM Usage",
                        currentValue = if (isRunning) ServerState.ram else "0 MB",
                        values = ServerState.ramHistory,
                        lineColor = MaterialTheme.colorScheme.error
                    )

                    ServerMiniChart(
                        title = "Network Upload",
                        currentValue = if (isRunning) "${ServerState.upload} kB/s" else "0 kB/s",
                        values = ServerState.uploadHistory,
                        lineColor = MaterialTheme.colorScheme.primary
                    )

                    ServerMiniChart(
                        title = "Network Download",
                        currentValue = if (isRunning) "${ServerState.download} kB/s" else "0 kB/s",
                        values = ServerState.downloadHistory,
                        lineColor = GreenSuccess
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = context.getString(R.string.auto_text_management),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ManagementTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.People,
                        title = context.getString(R.string.server_players),
                        onClick = onNavigateToPlayers
                    )
                    ManagementTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Shield,
                        title = context.getString(R.string.title_activity_whitelist),
                        onClick = onNavigateToWhitelist
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ManagementTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Public,
                        title = "Worlds",
                        onClick = onNavigateToWorlds
                    )
                    ManagementTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Extension,
                        title = context.getString(R.string.title_activity_plugins),
                        onClick = onNavigateToPlugins
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ManagementTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Update,
                        title = context.getString(R.string.title_activity_version_manager),
                        onClick = onNavigateToVersions
                    )
                    ManagementTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Schedule,
                        title = context.getString(R.string.auto_text_event_scheduler),
                        onClick = onNavigateToScheduler
                    )
                }
            }

            Spacer(modifier = Modifier.height(140.dp))
        }

        if (showAppSettingsDialog) {
            var openConsoleOnStart by remember { mutableStateOf(prefs.getBoolean("open_console_on_start", true)) }

            AlertDialog(
                onDismissRequest = { showAppSettingsDialog = false },
                title = { Text(context.getString(R.string.auto_java_app_settings)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { openConsoleOnStart = !openConsoleOnStart },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = openConsoleOnStart,
                                onCheckedChange = { openConsoleOnStart = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(context.getString(R.string.auto_text_open_console_on_star))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        prefs.edit()
                            .putBoolean("open_console_on_start", openConsoleOnStart)
                            .apply()
                        showAppSettingsDialog = false
                    }) {
                        Text(context.getString(R.string.config_save))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAppSettingsDialog = false }) {
                        Text(context.getString(R.string.btn_cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun ManagementTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ServerMiniChart(
    title: String,
    currentValue: String,
    values: List<Float>,
    lineColor: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = PillShape,
                    color = lineColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = currentValue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = lineColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                val data = if (values.isEmpty()) listOf(0f, 0f) else values
                val maxVal = (data.maxOrNull() ?: 1f).coerceAtLeast(1f)
                val w = size.width
                val h = size.height
                val paddingVertical = 6f
                val effectiveHeight = h - paddingVertical * 2

                val gridLines = 3
                for (i in 0..gridLines) {
                    val y = paddingVertical + (effectiveHeight / gridLines) * i
                    drawLine(
                        color = lineColor.copy(alpha = 0.08f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val strokePath = Path()
                val fillPath = Path()
                val stepX = if (data.size > 1) w / (data.size - 1) else w

                val points = data.mapIndexed { index, v ->
                    val x = index * stepX
                    val normalized = (v / maxVal).coerceIn(0f, 1f)
                    val y = h - paddingVertical - (normalized * effectiveHeight)
                    Offset(x, y)
                }

                strokePath.moveTo(points[0].x, points[0].y)
                fillPath.moveTo(points[0].x, h)
                fillPath.lineTo(points[0].x, points[0].y)

                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val cx = (p0.x + p1.x) / 2f
                    strokePath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    fillPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }

                fillPath.lineTo(points.last().x, h)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.28f),
                            lineColor.copy(alpha = 0.02f)
                        ),
                        startY = 0f,
                        endY = h
                    )
                )

                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                val lastPoint = points.last()
                drawCircle(
                    color = lineColor.copy(alpha = 0.3f),
                    radius = 5.dp.toPx(),
                    center = lastPoint
                )
                drawCircle(
                    color = lineColor,
                    radius = 3.dp.toPx(),
                    center = lastPoint
                )
                drawCircle(
                    color = Color.White,
                    radius = 1.5.dp.toPx(),
                    center = lastPoint
                )
            }
        }
    }
}
