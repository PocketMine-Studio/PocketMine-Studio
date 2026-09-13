package net.eqozqq.pocketminestudio.compose.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.eqozqq.pocketminestudio.R
import net.eqozqq.pocketminestudio.ServerUtils
import net.eqozqq.pocketminestudio.compose.model.PluginItem
import net.eqozqq.pocketminestudio.compose.repository.PluginRepository
import net.eqozqq.pocketminestudio.compose.theme.CardShape
import net.eqozqq.pocketminestudio.compose.theme.PillShape
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginsScreen(
    onNavigateToDetails: (name: String, version: String, iconUrl: String, descUrl: String, dlUrl: String, author: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var minApiFilter by remember { mutableStateOf("") }
    var maxApiFilter by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }

    val allPlugins = PluginRepository.plugins
    val categories = PluginRepository.categories
    val isLoading = PluginRepository.isLoading

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val contentResolver = context.contentResolver
                    var fileName = "plugin.phar"
                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIdx != -1) fileName = cursor.getString(nameIdx)
                        }
                    }
                    val pluginsDir = File(ServerUtils.getDataDirectory(), "plugins")
                    if (!pluginsDir.exists()) pluginsDir.mkdirs()
                    val dest = File(pluginsDir, fileName)
                    contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(dest).use { output ->
                            input.copyTo(output)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, String.format(context.getString(R.string.msg_plugin_imported), fileName), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.msg_failed_to_import_plugin), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!PluginRepository.isLoaded) {
            PluginRepository.loadPlugins()
        }
    }

    val filteredPlugins = remember(allPlugins, searchQuery, selectedCategory, minApiFilter, maxApiFilter) {
        allPlugins.filter { p ->
            val matchQuery = searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.tagline.contains(searchQuery, ignoreCase = true) ||
                    p.author.contains(searchQuery, ignoreCase = true)
            val matchCategory = selectedCategory == "All" || p.categories.contains(selectedCategory)
            val matchMinApi = minApiFilter.isBlank() || p.minApi.compareTo(minApiFilter) >= 0
            val matchMaxApi = maxApiFilter.isBlank() || (p.maxApi.isNotBlank() && p.maxApi.compareTo(maxApiFilter) <= 0)
            matchQuery && matchCategory && matchMinApi && matchMaxApi
        }
    }

    var pageSize by remember { mutableIntStateOf(30) }
    LaunchedEffect(searchQuery, selectedCategory, minApiFilter, maxApiFilter) {
        pageSize = 30
    }

    val displayedPlugins = remember(filteredPlugins, pageSize) {
        filteredPlugins.take(pageSize)
    }

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= displayedPlugins.size - 6 && pageSize < filteredPlugins.size
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            pageSize = (pageSize + 30).coerceAtMost(filteredPlugins.size)
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
                        if (isSearchActive) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search plugins...") },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(context.getString(R.string.title_activity_plugins), fontWeight = FontWeight.Bold)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) searchQuery = ""
                        }) {
                            Icon(
                                if (isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                                contentDescription = "Search"
                            )
                        }
                        IconButton(onClick = { showFilterDialog = true }) {
                            Icon(Icons.Rounded.FilterList, contentDescription = "Filter API")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { importLauncher.launch("*/*") },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Import Plugin") },
                shape = PillShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 90.dp)
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
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == "All",
                    onClick = { selectedCategory = "All" },
                    label = { Text("All") },
                    shape = PillShape
                )
                categories.filter { it != "All" }.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        shape = PillShape
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredPlugins.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No plugins found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        count = displayedPlugins.size,
                        key = { idx -> displayedPlugins[idx].name }
                    ) { idx ->
                        val plugin = displayedPlugins[idx]
                        Surface(
                            shape = CardShape,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(CardShape)
                                .clickable {
                                    onNavigateToDetails(
                                        plugin.name,
                                        plugin.version,
                                        plugin.iconUrl,
                                        plugin.descUrl,
                                        plugin.downloadUrl,
                                        plugin.author
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = plugin.iconUrl,
                                    contentDescription = plugin.name,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = plugin.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (plugin.minApi.isNotBlank()) {
                                            Surface(
                                                shape = PillShape,
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                                            ) {
                                                val apiText = if (plugin.maxApi.isNotBlank() && plugin.maxApi != plugin.minApi) {
                                                    "API ${plugin.minApi} - ${plugin.maxApi}"
                                                } else {
                                                    "API ${plugin.minApi}"
                                                }
                                                Text(
                                                    text = apiText,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    if (plugin.tagline.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = plugin.tagline,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "by ${plugin.author}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showFilterDialog) {
            var tempMin by remember { mutableStateOf(if (minApiFilter.isNotBlank()) minApiFilter else "4.0.0") }
            var tempMax by remember { mutableStateOf(if (maxApiFilter.isNotBlank()) maxApiFilter else "5.0.0") }

            AlertDialog(
                onDismissRequest = { showFilterDialog = false },
                title = { Text("Filter by API Version") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = tempMin,
                            onValueChange = { tempMin = it },
                            label = { Text("Minimum API (e.g. 4.0.0)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = tempMax,
                            onValueChange = { tempMax = it },
                            label = { Text("Maximum API (e.g. 5.0.0)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            minApiFilter = tempMin.trim()
                            maxApiFilter = tempMax.trim()
                            showFilterDialog = false
                        },
                        shape = PillShape
                    ) {
                        Text("Apply")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            minApiFilter = ""
                            maxApiFilter = ""
                            showFilterDialog = false
                        }
                    ) {
                        Text("Reset")
                    }
                }
            )
        }
    }
}
