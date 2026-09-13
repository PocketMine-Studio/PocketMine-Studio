package net.eqozqq.pocketminestudio

import android.os.Bundle
import android.preference.PreferenceManager
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import net.eqozqq.pocketminestudio.compose.PocketMineStudioApp
import net.eqozqq.pocketminestudio.compose.theme.PocketMineStudioTheme

class HomeActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServerUtils.setContext(applicationContext)
        if (ServerFragment.prefs == null) {
            ServerFragment.prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        }
        AssetExtractor.extractAssets(this)
        ServerUtils.startSchedulerThread()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            PocketMineStudioTheme {
                PocketMineStudioApp()
            }
        }
    }
}
