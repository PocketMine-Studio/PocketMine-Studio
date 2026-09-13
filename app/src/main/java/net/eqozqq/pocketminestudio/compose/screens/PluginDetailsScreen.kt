package net.eqozqq.pocketminestudio.compose.screens

import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.eqozqq.pocketminestudio.R
import net.eqozqq.pocketminestudio.ServerUtils
import net.eqozqq.pocketminestudio.compose.theme.CardShape
import net.eqozqq.pocketminestudio.compose.theme.PillShape
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.JSONValue
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class PluginVersionItem(
    val version: String,
    val apiRange: String,
    val downloadUrl: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginDetailsScreen(
    name: String,
    version: String,
    iconUrl: String,
    descUrl: String,
    dlUrl: String,
    author: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    var descriptionHtml by remember { mutableStateOf("") }
    var isDescLoading by remember { mutableStateOf(true) }

    var versions by remember { mutableStateOf<List<PluginVersionItem>>(emptyList()) }
    var isVersionsLoading by remember { mutableStateOf(true) }

    var isDownloading by remember { mutableStateOf(false) }
    var downloadingVersion by remember { mutableStateOf("") }

    val cleanIconUrl = if (iconUrl == "none") "" else iconUrl
    val cleanDescUrl = if (descUrl == "none") "" else descUrl
    val cleanAuthor = if (author == "none") "Unknown" else author
    val cleanVersion = if (version == "none") "" else version

    LaunchedEffect(name, cleanDescUrl) {
        withContext(Dispatchers.IO) {
            if (cleanDescUrl.isNotBlank()) {
                try {
                    val url = URL(cleanDescUrl)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "GET"
                    val markdown = conn.inputStream.bufferedReader().use { it.readText() }
                    val finalHtml = """
                        <html>
                        <head>
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <style>
                            body {
                                font-family: sans-serif;
                                padding: 8px;
                                margin: 0;
                                word-wrap: break-word;
                                background: transparent;
                                font-size: 15px;
                                line-height: 1.5;
                            }
                            @media (prefers-color-scheme: dark) {
                                body { color: #E6E1E5; }
                                a { color: #D0BCFF; }
                                pre, code { background: #2B2930; color: #E6E1E5; padding: 2px 4px; border-radius: 4px; }
                            }
                            @media (prefers-color-scheme: light) {
                                body { color: #1C1B1F; }
                                a { color: #6750A4; }
                                pre, code { background: #EADDFF; color: #21005D; padding: 2px 4px; border-radius: 4px; }
                            }
                            img { max-width: 100%; height: auto; border-radius: 8px; }
                            table { width: 100%; border-collapse: collapse; margin: 8px 0; }
                            th, td { border: 1px solid #79747E; padding: 6px; }
                        </style>
                        </head>
                        <body>
                        $markdown
                        </body>
                        </html>
                    """.trimIndent()
                    descriptionHtml = finalHtml
                } catch (_: Exception) {
                    descriptionHtml = "<html><body><p>Failed to load description.</p></body></html>"
                }
            } else {
                descriptionHtml = "<html><body><p>No description available.</p></body></html>"
            }
            isDescLoading = false

            try {
                val vUrl = URL("https://poggit.pmmp.io/releases.json?name=$name")
                val vConn = vUrl.openConnection() as HttpURLConnection
                vConn.requestMethod = "GET"
                val jsonString = vConn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONValue.parse(jsonString) as? JSONArray
                if (arr != null) {
                    val list = mutableListOf<PluginVersionItem>()
                    for (i in 0 until arr.size) {
                        val obj = arr[i] as? JSONObject ?: continue
                        val vName = obj["version"] as? String ?: ""
                        val artifactUrl = obj["artifact_url"] as? String ?: ""
                        val itemDl = "$artifactUrl/$name.phar"

                        val apiArr = obj["api"] as? JSONArray
                        var apiStr = "Unknown API"
                        if (apiArr != null && apiArr.size > 0) {
                            val apiObj = apiArr[0] as? JSONObject
                            val from = apiObj?.get("from") as? String ?: ""
                            val to = apiObj?.get("to") as? String ?: ""
                            apiStr = if (to.isNotBlank() && to != from) "API: $from - $to" else "API: $from"
                        }
                        list.add(PluginVersionItem(vName, apiStr, itemDl))
                    }
                    versions = list
                }
            } catch (_: Exception) {}
            isVersionsLoading = false
        }
    }

    fun startDownload(targetVersion: String, targetUrl: String) {
        if (isDownloading) return
        isDownloading = true
        downloadingVersion = targetVersion

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val pluginsDir = File(ServerUtils.getDataDirectory(), "plugins")
                if (!pluginsDir.exists()) pluginsDir.mkdirs()

                val url = URL(targetUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                val input = BufferedInputStream(url.openStream())
                val dest = File(pluginsDir, "${name}_v${targetVersion}.phar")
                val output = FileOutputStream(dest)

                val data = ByteArray(2048)
                var count: Int
                while (input.read(data).also { count = it } != -1) {
                    output.write(data, 0, count)
                }
                output.flush()
                output.close()
                input.close()

                withContext(Dispatchers.Main) {
                    isDownloading = false
                    Toast.makeText(context, "${name} v$targetVersion downloaded to /plugins", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isDownloading = false
                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    title = { Text(name, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://poggit.pmmp.io/p/$name"))
                            context.startActivity(browserIntent)
                        }) {
                            Icon(Icons.Rounded.OpenInBrowser, contentDescription = "Open in browser")
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = CardShape,
                color = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = cleanIconUrl,
                        contentDescription = name,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "by $cleanAuthor",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (cleanVersion.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = PillShape,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "v$cleanVersion",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isDownloading) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Downloading v$downloadingVersion...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(24.dp))
                    .padding(4.dp)
            ) {
                val tabTitles = listOf("Description", if (versions.isNotEmpty()) "Versions (${versions.size})" else "Versions")
                tabTitles.forEachIndexed { index, title ->
                    val selected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            )
                            .clickable { selectedTab = index }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (selectedTab == 0) {
                if (isDescLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                setBackgroundColor(0)
                                settings.javaScriptEnabled = false
                                settings.domStorageEnabled = false
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL("https://poggit.pmmp.io/", descriptionHtml, "text/html", "UTF-8", null)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
            } else {
                if (isVersionsLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (versions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No version history available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(versions, key = { it.version }) { item ->
                            Surface(
                                shape = CardShape,
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "v${item.version}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.apiRange,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Button(
                                        onClick = { startDownload(item.version, item.downloadUrl) },
                                        enabled = !isDownloading,
                                        shape = PillShape
                                    ) {
                                        Icon(
                                            Icons.Rounded.Download,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Download")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

