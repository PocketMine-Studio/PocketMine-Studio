package net.eqozqq.pocketminestudio.compose.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SaveAs
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.viewinterop.AndroidView
import io.github.rosemoe.sora.event.ContentChangeEvent
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.EditorSearcher
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.eqozqq.pocketminestudio.R
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(
    filePath: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentFile = remember { File(filePath) }
    var fileContent by remember { mutableStateOf("") }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var isSettingText by remember { mutableStateOf(false) }

    var showSearchBar by remember { mutableStateOf(false) }
    var showReplaceBar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }

    var editorInstance by remember { mutableStateOf<CodeEditor?>(null) }

    LaunchedEffect(filePath) {
        withContext(Dispatchers.IO) {
            try {
                if (currentFile.exists()) {
                    FileInputStream(currentFile).use { fis ->
                        fileContent = fis.bufferedReader().readText()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to load file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun saveContent() {
        val editor = editorInstance ?: return
        coroutineScope.launch(Dispatchers.IO) {
            try {
                FileOutputStream(currentFile).use { fos ->
                    fos.write(editor.text.toString().toByteArray())
                }
                withContext(Dispatchers.Main) {
                    hasUnsavedChanges = false
                    Toast.makeText(context, context.getString(R.string.msg_saved_successfully), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.msg_failed_to_save), Toast.LENGTH_SHORT).show()
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = currentFile.name,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (hasUnsavedChanges) {
                                Text(
                                    text = "Unsaved changes",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            showSearchBar = !showSearchBar
                            if (!showSearchBar) {
                                showReplaceBar = false
                                editorInstance?.searcher?.stopSearch()
                            }
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = {
                            showSearchBar = true
                            showReplaceBar = !showReplaceBar
                        }) {
                            Icon(Icons.Default.FindReplace, contentDescription = "Replace")
                        }
                        IconButton(onClick = { saveContent() }) {
                            Icon(
                                imageVector = if (hasUnsavedChanges) Icons.Default.SaveAs else Icons.Default.Save,
                                contentDescription = "Save",
                                tint = if (hasUnsavedChanges) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedVisibility(visible = showSearchBar) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { q ->
                                    searchQuery = q
                                    if (q.isNotEmpty()) {
                                        val opt = EditorSearcher.SearchOptions(EditorSearcher.SearchOptions.TYPE_NORMAL, true)
                                        editorInstance?.searcher?.search(q, opt)
                                    } else {
                                        editorInstance?.searcher?.stopSearch()
                                    }
                                },
                                placeholder = { Text("Find...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            IconButton(onClick = { editorInstance?.searcher?.gotoPrevious() }) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Previous")
                            }
                            IconButton(onClick = { editorInstance?.searcher?.gotoNext() }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Next")
                            }
                            IconButton(onClick = {
                                showSearchBar = false
                                showReplaceBar = false
                                editorInstance?.searcher?.stopSearch()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close Search")
                            }
                        }

                        if (showReplaceBar) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = replaceQuery,
                                    onValueChange = { replaceQuery = it },
                                    placeholder = { Text("Replace with...") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Button(
                                    onClick = {
                                        val ed = editorInstance ?: return@Button
                                        if (searchQuery.isNotEmpty()) {
                                            val t = ed.text.toString()
                                            ed.setText(t.replace(searchQuery, replaceQuery))
                                            hasUnsavedChanges = true
                                            Toast.makeText(context, context.getString(R.string.msg_replaced_instance), Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Replace All")
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E1F22),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        CodeEditor(ctx).apply {
                            isLineNumberEnabled = true
                            setTextSize(14f)
                            setBackgroundColor(android.graphics.Color.parseColor("#1E1F22"))
                            try {
                                colorScheme = SchemeDarcula()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            if (fileContent.isNotEmpty()) {
                                isSettingText = true
                                setText(fileContent)
                                isSettingText = false
                            }
                            subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
                                if (!isSettingText) {
                                    hasUnsavedChanges = true
                                }
                            }
                            editorInstance = this
                        }
                    },
                    update = { editor ->
                        if (fileContent.isNotEmpty() && editor.text.toString() != fileContent) {
                            isSettingText = true
                            editor.setText(fileContent)
                            isSettingText = false
                            hasUnsavedChanges = false
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
