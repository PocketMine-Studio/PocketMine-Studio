package net.eqozqq.pocketminestudio.compose.navigation

sealed class Screen(val route: String) {
    object Server : Screen("server")
    object Plugins : Screen("plugins")
    object Files : Screen("files")
    object Config : Screen("config")
    object Console : Screen("console")
    object VersionManager : Screen("version_manager")
    object ManagePlayers : Screen("manage_players")
    object ManageWhitelist : Screen("manage_whitelist")
    object ManageGrid : Screen("manage_grid/{path}/{title}") {
        fun createRoute(path: String, title: String): String = "manage_grid/${java.net.URLEncoder.encode(path, "UTF-8")}/${java.net.URLEncoder.encode(title, "UTF-8")}"
    }
    object Scheduler : Screen("scheduler")
    object CodeEditor : Screen("code_editor/{filePath}") {
        fun createRoute(filePath: String): String = "code_editor/${java.net.URLEncoder.encode(filePath, "UTF-8")}"
    }
    object Settings : Screen("settings")
    object About : Screen("about")
    object PluginDetails : Screen("plugin_details/{name}/{version}/{iconUrl}/{descUrl}/{dlUrl}/{author}") {
        fun createRoute(name: String, version: String, iconUrl: String, descUrl: String, dlUrl: String, author: String): String {
            val enc: (String) -> String = { s -> java.net.URLEncoder.encode(s.ifEmpty { "none" }, "UTF-8") }
            return "plugin_details/${enc(name)}/${enc(version)}/${enc(iconUrl)}/${enc(descUrl)}/${enc(dlUrl)}/${enc(author)}"
        }
    }
}

