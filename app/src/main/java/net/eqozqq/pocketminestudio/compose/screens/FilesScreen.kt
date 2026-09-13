package net.eqozqq.pocketminestudio.compose.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.eqozqq.pocketminestudio.CodeEditorActivity
import net.eqozqq.pocketminestudio.R
import net.eqozqq.pocketminestudio.ServerUtils
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.utils.IOUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URLConnection
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.DecimalFormat
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FilesScreen(
    onNavigateToEditor: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val rootDir = remember { File(ServerUtils.getDataDirectory()) }
    var currentDir by remember { mutableStateOf(rootDir) }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    val selectedFiles = remember { mutableStateListOf<File>() }
    val clipboardFiles = remember { mutableStateListOf<File>() }

    var activeSheetFile by remember { mutableStateOf<File?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var isCreatingFolder by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameTargetFile by remember { mutableStateOf<File?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var deleteTargetFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var showArchiveDialog by remember { mutableStateOf(false) }
    var archiveTargetFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var showFabMenu by remember { mutableStateOf(false) }

    fun refreshDir() {
        if (!currentDir.exists()) {
            currentDir.mkdirs()
        }
        val list = currentDir.listFiles()?.toList() ?: emptyList()
        files = list.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
    }

    LaunchedEffect(currentDir) {
        refreshDir()
    }

    val uploadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    var fileName = "uploaded_file"
                    context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (idx != -1) {
                                fileName = cursor.getString(idx)
                            }
                        }
                    }
                    val dest = File(currentDir, fileName)
                    context.contentResolver.openInputStream(it)?.use { input ->
                        FileOutputStream(dest).use { output ->
                            input.copyTo(output)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.msg_file_created), Toast.LENGTH_SHORT).show()
                        refreshDir()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.msg_file_creation_failed), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    BackHandler(enabled = selectedFiles.isNotEmpty() || currentDir != rootDir) {
        if (selectedFiles.isNotEmpty()) {
            selectedFiles.clear()
        } else if (currentDir != rootDir && currentDir.parentFile != null) {
            currentDir = currentDir.parentFile!!
        }
    }

    Scaffold(
        floatingActionButtonPosition = FabPosition.End,
        topBar = {
            Surface(
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = 3.dp,
                tonalElevation = 2.dp
            ) {
                CenterAlignedTopAppBar(
                    title = {
                        if (selectedFiles.isNotEmpty()) {
                            Text(
                                text = "${selectedFiles.size} selected",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (currentDir == rootDir) context.getString(R.string.auto_text_files) else currentDir.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val relativePath = currentDir.absolutePath.removePrefix(rootDir.absolutePath).ifEmpty { "/" }
                                Text(
                                    text = relativePath,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        if (selectedFiles.isNotEmpty()) {
                            IconButton(onClick = { selectedFiles.clear() }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                            }
                        } else if (currentDir != rootDir) {
                            IconButton(onClick = {
                                currentDir.parentFile?.let { currentDir = it }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    actions = {
                        if (selectedFiles.isNotEmpty()) {
                            IconButton(onClick = {
                                clipboardFiles.clear()
                                clipboardFiles.addAll(selectedFiles)
                                selectedFiles.clear()
                                Toast.makeText(context, context.getString(R.string.msg_copied_items, clipboardFiles.size), Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            }
                            IconButton(onClick = {
                                archiveTargetFiles = selectedFiles.toList()
                                showArchiveDialog = true
                            }) {
                                Icon(Icons.Default.Archive, contentDescription = "Archive")
                            }
                            IconButton(onClick = {
                                deleteTargetFiles = selectedFiles.toList()
                                showDeleteConfirmDialog = true
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        } else {
                            IconButton(onClick = { refreshDir() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        },
        floatingActionButton = {
            Column(
                modifier = Modifier.navigationBarsPadding().padding(bottom = 90.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedVisibility(
                    visible = showFabMenu,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 })
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                isCreatingFolder = false
                                showCreateDialog = true
                            },
                            icon = { Icon(Icons.Default.NoteAdd, contentDescription = null) },
                            text = { Text(context.getString(R.string.action_create_file)) },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        ExtendedFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                isCreatingFolder = true
                                showCreateDialog = true
                            },
                            icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                            text = { Text(context.getString(R.string.action_create_folder)) },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        ExtendedFloatingActionButton(
                            onClick = {
                                showFabMenu = false
                                uploadLauncher.launch("*/*")
                            },
                            icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                            text = { Text(context.getString(R.string.action_upload_file)) },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(
                        imageVector = if (showFabMenu) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Add"
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            BreadcrumbsBar(
                rootDir = rootDir,
                currentDir = currentDir,
                onNavigate = { currentDir = it }
            )

            if (clipboardFiles.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = context.getString(R.string.action_paste_items, clipboardFiles.size),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledTonalButton(
                                onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        try {
                                            for (cFile in clipboardFiles) {
                                                val dest = File(currentDir, cFile.name)
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    Files.copy(cFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
                                                }
                                            }
                                            withContext(Dispatchers.Main) {
                                                clipboardFiles.clear()
                                                refreshDir()
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, context.getString(R.string.msg_failed_to_paste), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Paste")
                            }
                            IconButton(
                                onClick = { clipboardFiles.clear() },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Clipboard", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            if (files.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Empty Directory",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(files, key = { it.absolutePath }) { file ->
                        val isSelected = selectedFiles.contains(file)
                        FileItemRow(
                            file = file,
                            isSelected = isSelected,
                            onItemClick = {
                                if (selectedFiles.isNotEmpty()) {
                                    if (isSelected) selectedFiles.remove(file) else selectedFiles.add(file)
                                } else {
                                    if (file.isDirectory) {
                                        currentDir = file
                                    } else {
                                        openFile(context, file, onNavigateToEditor)
                                    }
                                }
                            },
                            onItemLongClick = {
                                activeSheetFile = file
                            }
                        )
                    }
                }
            }
        }
    }

    if (activeSheetFile != null) {
        val target = activeSheetFile!!
        ModalBottomSheet(
            onDismissRequest = { activeSheetFile = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (target.isDirectory) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.secondaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (target.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = if (target.isDirectory) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = target.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (target.isDirectory) "Directory" else formatFileSize(target.length()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                SheetActionRow(
                    icon = Icons.Default.ContentCopy,
                    label = context.getString(R.string.action_copy),
                    onClick = {
                        activeSheetFile = null
                        clipboardFiles.clear()
                        clipboardFiles.add(target)
                        Toast.makeText(context, context.getString(R.string.msg_file_copied), Toast.LENGTH_SHORT).show()
                    }
                )
                SheetActionRow(
                    icon = Icons.Default.DriveFileRenameOutline,
                    label = context.getString(R.string.action_rename),
                    onClick = {
                        activeSheetFile = null
                        renameTargetFile = target
                        showRenameDialog = true
                    }
                )
                SheetActionRow(
                    icon = Icons.Default.CheckCircleOutline,
                    label = if (selectedFiles.contains(target)) "Deselect" else "Select",
                    onClick = {
                        activeSheetFile = null
                        if (selectedFiles.contains(target)) selectedFiles.remove(target) else selectedFiles.add(target)
                    }
                )
                SheetActionRow(
                    icon = Icons.Default.Archive,
                    label = context.getString(R.string.dialog_archive_format),
                    onClick = {
                        activeSheetFile = null
                        archiveTargetFiles = listOf(target)
                        showArchiveDialog = true
                    }
                )
                if (!target.isDirectory) {
                    SheetActionRow(
                        icon = Icons.Default.Download,
                        label = "Download",
                        onClick = {
                            activeSheetFile = null
                            saveFileToDownloads(context, target)
                        }
                    )
                }
                SheetActionRow(
                    icon = Icons.Default.Security,
                    label = context.getString(R.string.action_permissions),
                    onClick = {
                        activeSheetFile = null
                        target.setExecutable(true)
                        Toast.makeText(context, context.getString(R.string.msg_permission_granted), Toast.LENGTH_SHORT).show()
                    }
                )
                SheetActionRow(
                    icon = Icons.Default.Delete,
                    label = context.getString(R.string.auto_text_delete),
                    isDestructive = true,
                    onClick = {
                        activeSheetFile = null
                        deleteTargetFiles = listOf(target)
                        showDeleteConfirmDialog = true
                    }
                )
            }
        }
    }

    if (showCreateDialog) {
        var inputName by remember(isCreatingFolder) { mutableStateOf(if (isCreatingFolder) "new_folder" else "new_file.txt") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = {
                Text(
                    text = if (isCreatingFolder) context.getString(R.string.action_create_folder)
                    else context.getString(R.string.action_create_file)
                )
            },
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text(context.getString(R.string.input_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = inputName.trim()
                        if (name.isNotEmpty()) {
                            val newFile = File(currentDir, name)
                            try {
                                if (isCreatingFolder) newFile.mkdirs() else newFile.createNewFile()
                                Toast.makeText(context, context.getString(R.string.msg_file_created), Toast.LENGTH_SHORT).show()
                                refreshDir()
                            } catch (e: Exception) {
                                Toast.makeText(context, context.getString(R.string.msg_file_creation_failed), Toast.LENGTH_SHORT).show()
                            }
                        }
                        showCreateDialog = false
                    }
                ) {
                    Text(context.getString(R.string.action_add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(context.getString(R.string.btn_cancel))
                }
            }
        )
    }

    if (showRenameDialog && renameTargetFile != null) {
        val target = renameTargetFile!!
        var newName by remember { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(context.getString(R.string.action_rename)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(context.getString(R.string.input_name_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newName.trim()
                        if (name.isNotEmpty()) {
                            target.renameTo(File(currentDir, name))
                            refreshDir()
                        }
                        showRenameDialog = false
                    }
                ) {
                    Text(context.getString(R.string.action_rename))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(context.getString(R.string.btn_cancel))
                }
            }
        )
    }

    if (showDeleteConfirmDialog && deleteTargetFiles.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(context.getString(R.string.dialog_delete_confirmation)) },
            text = {
                Text(
                    text = if (deleteTargetFiles.size == 1) {
                        context.getString(R.string.dialog_delete_item_message, deleteTargetFiles[0].name)
                    } else {
                        context.getString(R.string.dialog_delete_items_message, deleteTargetFiles.size)
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            for (f in deleteTargetFiles) {
                                f.deleteRecursively()
                            }
                            withContext(Dispatchers.Main) {
                                selectedFiles.removeAll(deleteTargetFiles)
                                showDeleteConfirmDialog = false
                                refreshDir()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(context.getString(R.string.auto_text_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(context.getString(R.string.config_cancel))
                }
            }
        )
    }

    if (showArchiveDialog && archiveTargetFiles.isNotEmpty()) {
        val formats = listOf(".zip", ".tar.gz", ".tar")
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = { Text(context.getString(R.string.dialog_archive_format)) },
            text = {
                Column {
                    formats.forEach { fmt ->
                        Surface(
                            onClick = {
                                showArchiveDialog = false
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        when (fmt) {
                                            ".zip" -> archiveZip(currentDir, archiveTargetFiles)
                                            ".tar.gz" -> archiveTarGz(currentDir, archiveTargetFiles)
                                            ".tar" -> archiveTar(currentDir, archiveTargetFiles)
                                        }
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, context.getString(R.string.msg_archived_success), Toast.LENGTH_SHORT).show()
                                            selectedFiles.clear()
                                            refreshDir()
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, context.getString(R.string.msg_failed_to_archive), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Archive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(fmt, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = false }) {
                    Text(context.getString(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
private fun BreadcrumbsBar(
    rootDir: File,
    currentDir: File,
    onNavigate: (File) -> Unit
) {
    val scrollState = rememberScrollState()
    val parts = remember(currentDir) {
        val list = mutableListOf<File>()
        var curr: File? = currentDir
        while (curr != null && curr.absolutePath.startsWith(rootDir.absolutePath)) {
            list.add(0, curr)
            if (curr.absolutePath == rootDir.absolutePath) break
            curr = curr.parentFile
        }
        if (list.isEmpty()) listOf(rootDir) else list
    }

    LaunchedEffect(parts.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        parts.forEachIndexed { index, dir ->
            val isLast = index == parts.size - 1
            val label = if (dir.absolutePath == rootDir.absolutePath) "root" else dir.name
            SuggestionChip(
                onClick = { onNavigate(dir) },
                label = {
                    Text(
                        text = label,
                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                        color = if (isLast) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = if (isLast) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = null,
                shape = RoundedCornerShape(8.dp)
            )
            if (!isLast) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileItemRow(
    file: File,
    isSelected: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit
) {
    val icon = if (isSelected) {
        Icons.Default.CheckCircle
    } else if (file.isDirectory) {
        Icons.Default.Folder
    } else {
        Icons.Default.InsertDriveFile
    }

    val iconContainerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else if (file.isDirectory) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val iconColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else if (file.isDirectory) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onItemClick,
                onLongClick = onItemLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!file.isDirectory) {
                    Text(
                        text = formatFileSize(file.length()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetActionRow(
    icon: ImageVector,
    label: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun openFile(
    context: Context,
    file: File,
    onNavigateToEditor: ((String) -> Unit)?
) {
    val mimeType = URLConnection.guessContentTypeFromName(file.name)
    val isText = file.name.endsWith(".php") || file.name.endsWith(".json") || file.name.endsWith(".yml") ||
            file.name.endsWith(".yaml") || file.name.endsWith(".properties") || file.name.endsWith(".md") ||
            file.name.endsWith(".log") || file.name.endsWith(".ini") || (mimeType != null && mimeType.startsWith("text/"))

    val isBinary = file.name.endsWith(".phar") || file.name.endsWith(".gz") || file.name.endsWith(".tar") ||
            file.name.endsWith(".zip") || file.name.endsWith(".so")

    if (!isText || isBinary) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = FileProvider.getUriForFile(context, context.packageName + ".provider", file)
            val determinedMime = when {
                file.name.endsWith(".zip") -> "application/zip"
                file.name.endsWith(".tar") || file.name.endsWith(".gz") -> "application/x-tar"
                file.name.endsWith(".phar") -> "application/octet-stream"
                mimeType != null -> mimeType
                else -> "*/*"
            }
            intent.setDataAndType(uri, determinedMime)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_open_with)))
        } catch (e: Exception) {
            Toast.makeText(context, context.getString(R.string.msg_cannot_open_file), Toast.LENGTH_SHORT).show()
        }
        return
    }

    if (onNavigateToEditor != null) {
        onNavigateToEditor(file.absolutePath)
    } else {
        try {
            val intent = Intent(context, CodeEditorActivity::class.java)
            intent.putExtra("filePath", file.absolutePath)
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                val uri = FileProvider.getUriForFile(context, context.packageName + ".provider", file)
                intent.setDataAndType(uri, "*/*")
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.action_open_with)))
            } catch (ex: Exception) {
                Toast.makeText(context, context.getString(R.string.auto_java_cannot_open_file), Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun saveFileToDownloads(context: Context, file: File) {
    try {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()
        val dest = File(downloadsDir, file.name)
        FileInputStream(file).use { input ->
            FileOutputStream(dest).use { output ->
                input.copyTo(output)
            }
        }
        Toast.makeText(context, String.format(context.getString(R.string.msg_file_downloaded), file.name), Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.msg_file_download_failed), Toast.LENGTH_SHORT).show()
    }
}

private fun archiveZip(parentDir: File, files: List<File>) {
    val zipName = if (files.size == 1) "${files[0].name}.zip" else "archive_${System.currentTimeMillis()}.zip"
    val zipFile = File(parentDir, zipName)
    FileOutputStream(zipFile).use { fos ->
        ZipOutputStream(fos).use { zos ->
            for (f in files) {
                addFileToZip(f, f.name, zos)
            }
        }
    }
}

private fun addFileToZip(fileToZip: File, fileName: String, zos: ZipOutputStream) {
    if (fileToZip.isHidden) return
    if (fileToZip.isDirectory) {
        val entryName = if (fileName.endsWith("/")) fileName else "$fileName/"
        zos.putNextEntry(ZipEntry(entryName))
        zos.closeEntry()
        fileToZip.listFiles()?.forEach { child ->
            addFileToZip(child, "$entryName${child.name}", zos)
        }
        return
    }
    FileInputStream(fileToZip).use { fis ->
        val zipEntry = ZipEntry(fileName)
        zos.putNextEntry(zipEntry)
        fis.copyTo(zos)
        zos.closeEntry()
    }
}

private fun archiveTar(parentDir: File, files: List<File>) {
    val tarName = if (files.size == 1) "${files[0].name}.tar" else "archive_${System.currentTimeMillis()}.tar"
    val tarFile = File(parentDir, tarName)
    FileOutputStream(tarFile).use { fos ->
        TarArchiveOutputStream(fos).use { taos ->
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
            for (f in files) {
                addFileToTar(f, f.name, taos)
            }
            taos.finish()
        }
    }
}

private fun archiveTarGz(parentDir: File, files: List<File>) {
    val tgzName = if (files.size == 1) "${files[0].name}.tar.gz" else "archive_${System.currentTimeMillis()}.tar.gz"
    val tgzFile = File(parentDir, tgzName)
    FileOutputStream(tgzFile).use { fos ->
        GZIPOutputStream(fos).use { gos ->
            TarArchiveOutputStream(gos).use { taos ->
                taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
                for (f in files) {
                    addFileToTar(f, f.name, taos)
                }
                taos.finish()
            }
        }
    }
}

private fun addFileToTar(fileToTar: File, fileName: String, taos: TarArchiveOutputStream) {
    if (fileToTar.isHidden) return
    val entry = TarArchiveEntry(fileToTar, fileName)
    taos.putArchiveEntry(entry)
    if (fileToTar.isFile) {
        FileInputStream(fileToTar).use { fis ->
            IOUtils.copy(fis, taos)
        }
        taos.closeArchiveEntry()
    } else if (fileToTar.isDirectory) {
        taos.closeArchiveEntry()
        fileToTar.listFiles()?.forEach { child ->
            addFileToTar(child, "$fileName/${child.name}", taos)
        }
    }
}

private fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(size / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}

