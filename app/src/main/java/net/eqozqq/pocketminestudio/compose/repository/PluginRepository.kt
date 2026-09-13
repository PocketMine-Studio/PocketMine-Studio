package net.eqozqq.pocketminestudio.compose.repository

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.eqozqq.pocketminestudio.compose.model.PluginItem
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.JSONValue
import java.net.HttpURLConnection
import java.net.URL

object PluginRepository {
    var plugins by mutableStateOf<List<PluginItem>>(emptyList())
    var categories by mutableStateOf<List<String>>(listOf("All"))
    var isLoading by mutableStateOf(false)
    var isLoaded by mutableStateOf(false)
    var isError by mutableStateOf(false)

    suspend fun loadPlugins(force: Boolean = false) {
        if (isLoaded && !force) return
        if (isLoading) return
        isLoading = true
        isError = false
        withContext(Dispatchers.IO) {
            try {
                val url = URL("https://poggit.pmmp.io/releases.json")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 10000
                conn.readTimeout = 15000
                val jsonString = conn.inputStream.bufferedReader().use { it.readText() }
                val array = JSONValue.parse(jsonString) as? JSONArray
                if (array != null) {
                    val uniquePlugins = LinkedHashMap<String, JSONObject>()
                    for (i in 0 until array.size) {
                        val obj = array[i] as? JSONObject ?: continue
                        val name = obj["name"] as? String ?: continue
                        if (!uniquePlugins.containsKey(name)) {
                            uniquePlugins[name] = obj
                        }
                    }

                    val parsedList = mutableListOf<PluginItem>()
                    val categorySet = linkedSetOf("All")

                    for ((name, obj) in uniquePlugins) {
                        val version = obj["version"] as? String ?: ""
                        val tagline = obj["tagline"] as? String ?: ""
                        val iconUrl = obj["icon_url"] as? String ?: ""
                        val descUrl = obj["description_url"] as? String ?: ""
                        val artifactUrl = obj["artifact_url"] as? String ?: ""
                        val repoName = obj["repo_name"] as? String ?: ""
                        val author = if (repoName.contains("/")) repoName.split("/")[0] else "Unknown"

                        val catList = mutableListOf<String>()
                        val catArr = obj["categories"] as? JSONArray
                        if (catArr != null) {
                            for (j in 0 until catArr.size) {
                                val cObj = catArr[j] as? JSONObject
                                val cName = cObj?.get("category_name") as? String
                                if (!cName.isNullOrBlank()) {
                                    val trimmed = cName.trim()
                                    catList.add(trimmed)
                                    categorySet.add(trimmed)
                                }
                            }
                        }

                        val apiArr = obj["api"] as? JSONArray
                        var minApi = ""
                        var maxApi = ""
                        if (apiArr != null && apiArr.size > 0) {
                            val api0 = apiArr[0] as? JSONObject
                            minApi = api0?.get("from") as? String ?: ""
                            maxApi = api0?.get("to") as? String ?: ""
                        }

                        parsedList.add(
                            PluginItem(
                                name = name,
                                version = version,
                                tagline = tagline,
                                iconUrl = iconUrl,
                                descUrl = descUrl,
                                downloadUrl = "$artifactUrl/$name.phar",
                                author = author,
                                categories = catList,
                                minApi = minApi,
                                maxApi = maxApi
                            )
                        )
                    }

                    withContext(Dispatchers.Main) {
                        plugins = parsedList
                        categories = categorySet.toList()
                        isLoaded = true
                        isLoading = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isError = true
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isError = true
                    isLoading = false
                }
            }
        }
    }
}
