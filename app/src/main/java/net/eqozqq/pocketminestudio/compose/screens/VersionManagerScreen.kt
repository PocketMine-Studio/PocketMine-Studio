package net.eqozqq.pocketminestudio.compose.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import net.eqozqq.pocketminestudio.compose.theme.PillShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.eqozqq.pocketminestudio.R
import net.eqozqq.pocketminestudio.ServerFragment
import net.eqozqq.pocketminestudio.ServerUtils
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.JSONValue
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection

data class CoreRelease(
    val id: String,
    val name: String,
    val tagName: String,
    val body: String,
    val pharUrl: String,
    val shUrl: String,
    val mcVersion: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VersionManagerScreen(
    onNavigateBack: (() -> Unit)? = null,
    onInstallComplete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    var pmReleases by remember { mutableStateOf<List<CoreRelease>>(emptyList()) }
    var altayReleases by remember { mutableStateOf<List<CoreRelease>>(emptyList()) }

    var expandedReleaseId by remember { mutableStateOf<String?>(null) }
    var installTargetRelease by remember { mutableStateOf<CoreRelease?>(null) }
    var isInstalling by remember { mutableStateOf(false) }
    var installStatusMessage by remember { mutableStateOf("") }
    var installProgress by remember { mutableFloatStateOf(0f) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    var fileName = "CustomCore.phar"
                    context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (idx != -1) {
                                fileName = cursor.getString(idx)
                            }
                        }
                    }
                    if (!fileName.endsWith(".phar")) fileName = "CustomCore.phar"

                    val dest = File(ServerUtils.getDataDirectory(), fileName)
                    File(ServerUtils.getDataDirectory(), "PocketMine-MP.phar").delete()
                    File(ServerUtils.getDataDirectory(), "BetterAltay.phar").delete()

                    context.contentResolver.openInputStream(it)?.use { input ->
                        FileOutputStream(dest).use { output ->
                            input.copyTo(output)
                        }
                    }

                    ServerFragment.prefs?.edit()
                        ?.putString("custom_core_path", dest.absolutePath)
                        ?.putString("selected_core", fileName)
                        ?.apply()

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, String.format(context.getString(R.string.msg_custom_phar_imported), fileName), Toast.LENGTH_SHORT).show()
                        onInstallComplete?.invoke()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.msg_failed_to_import_phar), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    fun loadReleasesForTab(tab: Int) {
        coroutineScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { isLoading = true }
            try {
                val apiUrl = if (tab == 0) {
                    "https://api.github.com/repos/pmmp/pocketmine-mp/releases?per_page=100"
                } else {
                    "https://api.github.com/repos/BetterAltayBedrock/BetterAltay/releases?per_page=100"
                }

                val json = fetchUrlContent(apiUrl)
                val array = JSONValue.parse(json) as? JSONArray ?: JSONArray()
                val list = mutableListOf<CoreRelease>()

                for (i in 0 until array.size) {
                    val obj = array[i] as? JSONObject ?: continue
                    val tagName = obj["tag_name"] as? String ?: ""
                    val name = (obj["name"] as? String)?.ifEmpty { tagName } ?: tagName
                    val body = obj["body"] as? String ?: ""
                    val assets = obj["assets"] as? JSONArray

                    var pharUrl = ""
                    var shUrl = ""
                    if (assets != null) {
                        for (j in 0 until assets.size) {
                            val asset = assets[j] as? JSONObject ?: continue
                            val assetName = asset["name"] as? String ?: ""
                            val downloadUrl = asset["browser_download_url"] as? String ?: ""
                            if (tab == 1) {
                                if (assetName.equals("BetterAltay.phar", true) || assetName.endsWith(".phar")) {
                                    pharUrl = downloadUrl
                                } else if (assetName.equals("start.sh", true)) {
                                    shUrl = downloadUrl
                                }
                            } else {
                                if (assetName.equals("PocketMine-MP.phar", true)) {
                                    pharUrl = downloadUrl
                                } else if (assetName.equals("start.sh", true)) {
                                    shUrl = downloadUrl
                                }
                            }
                        }
                    }

                    if (pharUrl.isEmpty()) {
                        pharUrl = if (tab == 1) {
                            "https://github.com/BetterAltayBedrock/BetterAltay/releases/download/$tagName/BetterAltay.phar"
                        } else {
                            "https://github.com/pmmp/pocketmine-mp/releases/download/$tagName/PocketMine-MP.phar"
                        }
                    }

                    if (shUrl.isEmpty()) {
                        shUrl = if (tab == 1) {
                            "https://github.com/pmmp/pocketmine-mp/releases/download/5.0.0/start.sh"
                        } else {
                            "https://github.com/pmmp/pocketmine-mp/releases/download/$tagName/start.sh"
                        }
                    }

                    var mcVersion = ""
                    if (tab == 0 && body.isNotEmpty()) {
                        for (line in body.split("\n")) {
                            if (line.contains("For Minecraft:", true)) {
                                mcVersion = line.replace("**", "").trim()
                                break
                            }
                        }
                    }

                    list.add(
                        CoreRelease(
                            id = "$tab-$tagName",
                            name = name,
                            tagName = tagName,
                            body = body,
                            pharUrl = pharUrl,
                            shUrl = shUrl,
                            mcVersion = mcVersion
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    if (tab == 0) pmReleases = list else altayReleases = list
                    isLoading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    Toast.makeText(context, "Cannot load version list", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(selectedTab) {
        val target = if (selectedTab == 0) pmReleases else altayReleases
        if (target.isEmpty()) {
            loadReleasesForTab(selectedTab)
        }
    }

    fun performInstallation(release: CoreRelease) {
        isInstalling = true
        installStatusMessage = "Starting download..."
        installProgress = 0f

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val vdir = File(ServerUtils.getDataDirectory(), "versions")
                if (!vdir.exists()) vdir.mkdirs()

                val arch = System.getProperty("os.arch").lowercase()
                val isArm = arch.contains("aarch64") || arch.contains("arm")
                val phpUrl = if (isArm) {
                    "https://github.com/pmmp/PHP-Binaries/releases/download/pm5-php-8.2-latest/PHP-8.2-Android-arm64-PM5.tar.gz"
                } else {
                    "https://github.com/pmmp/PHP-Binaries/releases/download/pm5-php-8.2-latest/PHP-8.2-Linux-x86_64-PM5.tar.gz"
                }

                val currentPharName = if (selectedTab == 1) "BetterAltay.phar" else "PocketMine-MP.phar"
                val pharFile = File(vdir, currentPharName)
                val shFile = File(vdir, "start.sh")
                val phpFile = File(vdir, "php.tar.gz")

                withContext(Dispatchers.Main) {
                    installStatusMessage = "Downloading ${release.name} ($currentPharName)..."
                }
                downloadWithProgress(release.pharUrl, pharFile) { p ->
                    installProgress = p * 0.4f
                }

                withContext(Dispatchers.Main) {
                    installStatusMessage = "Downloading start.sh..."
                }
                downloadWithProgress(release.shUrl, shFile) { p ->
                    installProgress = 0.4f + p * 0.1f
                }

                withContext(Dispatchers.Main) {
                    installStatusMessage = "Downloading PHP Binaries..."
                }
                downloadWithProgress(phpUrl, phpFile) { p ->
                    installProgress = 0.5f + p * 0.3f
                }

                withContext(Dispatchers.Main) {
                    installStatusMessage = "Configuring and extracting..."
                    installProgress = 0.85f
                }

                ServerFragment.prefs?.edit()
                    ?.remove("custom_core_path")
                    ?.putString("selected_core", currentPharName)
                    ?.apply()

                File(ServerUtils.getDataDirectory(), "src").deleteRecursively()
                File(ServerUtils.getAppDirectory(), "php").deleteRecursively()
                File(ServerUtils.getDataDirectory(), "PocketMine-MP.phar").delete()
                File(ServerUtils.getDataDirectory(), "BetterAltay.phar").delete()
                File(ServerUtils.getDataDirectory(), "start.sh").delete()

                pharFile.copyTo(File(ServerUtils.getDataDirectory(), currentPharName), overwrite = true)
                shFile.copyTo(File(ServerUtils.getDataDirectory(), "start.sh"), overwrite = true)
                File(ServerUtils.getDataDirectory(), "start.sh").setExecutable(true)

                extractTarGz(phpFile, File(ServerUtils.getAppDirectory(), "php"))

                withContext(Dispatchers.Main) {
                    installProgress = 1f
                    installStatusMessage = "Done!"
                    isInstalling = false
                    Toast.makeText(context, "Installation completed!", Toast.LENGTH_SHORT).show()
                    onInstallComplete?.invoke()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isInstalling = false
                    Toast.makeText(context, "Installation failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val currentList = if (selectedTab == 0) pmReleases else altayReleases
    val filteredList = remember(currentList, searchQuery) {
        if (searchQuery.isEmpty()) currentList
        else currentList.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.tagName.contains(searchQuery, ignoreCase = true)
        }
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
                            text = context.getString(R.string.title_activity_version_manager),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        if (onNavigateBack != null && ServerUtils.checkIfInstalled()) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { loadReleasesForTab(selectedTab) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { importLauncher.launch("*/*") },
                icon = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                text = { Text("Import Custom Core (.phar)") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = PillShape,
                modifier = Modifier.navigationBarsPadding().padding(bottom = 16.dp)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(24.dp))
                    .padding(4.dp)
            ) {
                listOf("PocketMine-MP", "BetterAltay").forEachIndexed { index, title ->
                    val selected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else androidx.compose.ui.graphics.Color.Transparent
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

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(context.getString(R.string.auto_hint_search_versions)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No versions found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.id }) { release ->
                        val isExpanded = expandedReleaseId == release.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .clickable {
                                    expandedReleaseId = if (isExpanded) null else release.id
                                },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = release.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = release.tagName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = { installTargetRelease = release },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text("Install")
                                    }
                                }

                                if (release.mcVersion.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                    ) {
                                        Text(
                                            text = release.mcVersion,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = isExpanded && release.body.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp)
                                    ) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = release.body.take(1500),
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
    }

    if (installTargetRelease != null) {
        val rel = installTargetRelease!!
        AlertDialog(
            onDismissRequest = { installTargetRelease = null },
            title = { Text("Install ${rel.name}") },
            text = { Text(context.getString(R.string.auto_java_are_you_sure_you_want_to_insta)) },
            confirmButton = {
                Button(
                    onClick = {
                        installTargetRelease = null
                        performInstallation(rel)
                    }
                ) {
                    Text("Install")
                }
            },
            dismissButton = {
                TextButton(onClick = { installTargetRelease = null }) {
                    Text(context.getString(R.string.btn_cancel))
                }
            }
        )
    }

    if (isInstalling) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(context.getString(R.string.auto_java_installing)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = installStatusMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    LinearProgressIndicator(
                        progress = { installProgress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "${(installProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            },
            confirmButton = {}
        )
    }
}

private fun fetchUrlContent(urlStr: String): String {
    val url = URL(urlStr)
    val conn = url.openConnection() as HttpURLConnection
    conn.setRequestProperty("User-Agent", "PocketMine-Studio")
    conn.instanceFollowRedirects = true
    val status = conn.responseCode
    if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
        val redirect = conn.getHeaderField("Location")
        if (redirect != null) {
            conn.disconnect()
            return fetchUrlContent(redirect)
        }
    }
    val sb = StringBuilder()
    BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            sb.append(line)
        }
    }
    conn.disconnect()
    return sb.toString()
}

private fun downloadWithProgress(address: String, dest: File, onProgress: (Float) -> Unit) {
    val url = URL(address)
    val conn = url.openConnection()
    conn.connect()
    val length = conn.contentLength
    BufferedInputStream(url.openStream()).use { input ->
        FileOutputStream(dest).use { output ->
            val buf = ByteArray(2048)
            var total = 0L
            var read: Int
            while (input.read(buf).also { read = it } != -1) {
                total += read
                output.write(buf, 0, read)
                if (length > 0) {
                    onProgress(total.toFloat() / length.toFloat())
                }
            }
        }
    }
}

private fun extractTarGz(tarGzFile: File, targetDirectory: File) {
    TarArchiveInputStream(GzipCompressorInputStream(FileInputStream(tarGzFile))).use { tin ->
        var entry: TarArchiveEntry?
        while (tin.nextTarEntry.also { entry = it } != null) {
            val e = entry ?: break
            val newFile = File(targetDirectory, e.name)
            if (e.isDirectory) {
                newFile.mkdirs()
            } else {
                newFile.parentFile?.mkdirs()
                FileOutputStream(newFile).use { fos ->
                    val buf = ByteArray(2048)
                    var len: Int
                    while (tin.read(buf).also { len = it } != -1) {
                        fos.write(buf, 0, len)
                    }
                }
                if (e.name.contains("bin/")) {
                    newFile.setExecutable(true)
                }
            }
        }
    }
}
