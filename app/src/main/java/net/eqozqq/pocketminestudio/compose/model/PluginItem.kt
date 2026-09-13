package net.eqozqq.pocketminestudio.compose.model

data class PluginItem(
    val name: String,
    val version: String,
    val tagline: String,
    val iconUrl: String,
    val descUrl: String,
    val downloadUrl: String,
    val author: String,
    val categories: List<String>,
    val minApi: String,
    val maxApi: String
)

