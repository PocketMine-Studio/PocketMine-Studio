package net.eqozqq.pocketminestudio.compose

import android.preference.PreferenceManager
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import net.eqozqq.pocketminestudio.R
import net.eqozqq.pocketminestudio.ServerFragment
import net.eqozqq.pocketminestudio.ServerUtils
import net.eqozqq.pocketminestudio.compose.navigation.Screen
import net.eqozqq.pocketminestudio.compose.screens.*
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class BottomNavItem(
    val route: String,
    val title: String,
    val selectedRes: Int,
    val unselectedRes: Int
)

@Composable
fun PocketMineStudioApp(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    if (ServerUtils.getContext() == null) {
        ServerUtils.setContext(context.applicationContext)
    }
    if (ServerFragment.prefs == null) {
        ServerFragment.prefs = PreferenceManager.getDefaultSharedPreferences(context.applicationContext)
    }
    LaunchedEffect(Unit) {
        net.eqozqq.pocketminestudio.compose.repository.PluginRepository.loadPlugins()
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = remember {
        listOf(
            BottomNavItem(
                Screen.Plugins.route,
                context.getString(R.string.title_activity_plugins),
                R.drawable.ic_extension_24px_fill,
                R.drawable.ic_extension_24px_outline
            ),
            BottomNavItem(
                Screen.Server.route,
                context.getString(R.string.auto_title_server),
                R.drawable.ic_dns_24px_fill,
                R.drawable.ic_dns_24px_outline
            ),
            BottomNavItem(
                Screen.Files.route,
                context.getString(R.string.auto_text_files),
                R.drawable.ic_folder_open_24px_fill,
                R.drawable.ic_folder_open_24px_outline
            ),
            BottomNavItem(
                Screen.Config.route,
                context.getString(R.string.auto_title_options),
                R.drawable.ic_nav_settings_24px_fill,
                R.drawable.ic_nav_settings_24px_outline
            )
        )
    }

    val isTopLevelRoute = bottomNavItems.any { it.route == currentDestination?.route }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = isTopLevelRoute,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shadowElevation = 8.dp,
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(74.dp)
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp,
                            windowInsets = WindowInsets(0, 0, 0, 0),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            bottomNavItems.forEach { item ->
                                val selected = currentDestination?.route == item.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        if (currentDestination?.route != item.route) {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            painter = painterResource(if (selected) item.selectedRes else item.unselectedRes),
                                            contentDescription = item.title,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    },
                                    alwaysShowLabel = false
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { _ ->
        val startRoute = if (!ServerUtils.checkIfInstalled()) Screen.VersionManager.route else Screen.Server.route
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220)) },
            exitTransition = { fadeOut(animationSpec = tween(180)) },
            popEnterTransition = { fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220)) },
            popExitTransition = { fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.96f, animationSpec = tween(180)) }
        ) {
            composable(Screen.Plugins.route) {
                PluginsScreen(
                    onNavigateToDetails = { name, version, iconUrl, descUrl, dlUrl, author ->
                        navController.navigate(
                            Screen.PluginDetails.createRoute(name, version, iconUrl, descUrl, dlUrl, author)
                        )
                    }
                )
            }

            composable(
                route = Screen.PluginDetails.route,
                arguments = listOf(
                    navArgument("name") { type = NavType.StringType },
                    navArgument("version") { type = NavType.StringType },
                    navArgument("iconUrl") { type = NavType.StringType },
                    navArgument("descUrl") { type = NavType.StringType },
                    navArgument("dlUrl") { type = NavType.StringType },
                    navArgument("author") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val name = URLDecoder.decode(backStackEntry.arguments?.getString("name") ?: "", StandardCharsets.UTF_8.toString())
                val version = URLDecoder.decode(backStackEntry.arguments?.getString("version") ?: "", StandardCharsets.UTF_8.toString())
                val iconUrl = URLDecoder.decode(backStackEntry.arguments?.getString("iconUrl") ?: "", StandardCharsets.UTF_8.toString())
                val descUrl = URLDecoder.decode(backStackEntry.arguments?.getString("descUrl") ?: "", StandardCharsets.UTF_8.toString())
                val dlUrl = URLDecoder.decode(backStackEntry.arguments?.getString("dlUrl") ?: "", StandardCharsets.UTF_8.toString())
                val author = URLDecoder.decode(backStackEntry.arguments?.getString("author") ?: "", StandardCharsets.UTF_8.toString())

                PluginDetailsScreen(
                    name = name,
                    version = version,
                    iconUrl = iconUrl,
                    descUrl = descUrl,
                    dlUrl = dlUrl,
                    author = author,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Server.route) {
                ServerScreen(
                    onNavigateToConsole = { navController.navigate(Screen.Console.route) },
                    onNavigateToPlayers = { navController.navigate(Screen.ManagePlayers.route) },
                    onNavigateToWhitelist = { navController.navigate(Screen.ManageWhitelist.route) },
                    onNavigateToWorlds = {
                        navController.navigate(Screen.ManageGrid.createRoute(ServerUtils.getDataDirectory() + "/worlds", "Worlds"))
                    },
                    onNavigateToPlugins = {
                        navController.navigate(Screen.ManageGrid.createRoute(ServerUtils.getDataDirectory() + "/plugins", "Plugins"))
                    },
                    onNavigateToVersions = { navController.navigate(Screen.VersionManager.route) },
                    onNavigateToScheduler = { navController.navigate(Screen.Scheduler.route) }
                )
            }

            composable(Screen.Console.route) {
                ConsoleScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Files.route) {
                FilesScreen(
                    onNavigateToEditor = { path ->
                        val encoded = URLEncoder.encode(path, StandardCharsets.UTF_8.toString())
                        navController.navigate(Screen.CodeEditor.createRoute(encoded))
                    }
                )
            }

            composable(Screen.Config.route) {
                ConfigScreen()
            }

            composable(Screen.VersionManager.route) {
                VersionManagerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onInstallComplete = { navController.navigate(Screen.Server.route) }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ManagePlayers.route) {
                ManagePlayersScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ManageWhitelist.route) {
                ManagePlayersScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.ManageGrid.route,
                arguments = listOf(
                    navArgument("path") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val encodedPath = backStackEntry.arguments?.getString("path") ?: ""
                val encodedTitle = backStackEntry.arguments?.getString("title") ?: ""
                val decodedPath = try {
                    URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.toString())
                } catch (e: Exception) {
                    encodedPath
                }
                val decodedTitle = try {
                    URLDecoder.decode(encodedTitle, StandardCharsets.UTF_8.toString())
                } catch (e: Exception) {
                    encodedTitle
                }
                ManageGridScreen(
                    title = decodedTitle,
                    path = decodedPath,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Scheduler.route) {
                SchedulerScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = Screen.CodeEditor.route,
                arguments = listOf(navArgument("filePath") { type = NavType.StringType })
            ) { backStackEntry ->
                val encodedPath = backStackEntry.arguments?.getString("filePath") ?: ""
                val decodedPath = try {
                    URLDecoder.decode(encodedPath, StandardCharsets.UTF_8.toString())
                } catch (e: Exception) {
                    encodedPath
                }
                CodeEditorScreen(
                    filePath = decodedPath,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
