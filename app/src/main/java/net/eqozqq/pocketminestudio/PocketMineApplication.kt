package net.eqozqq.pocketminestudio

import android.app.Application
import android.preference.PreferenceManager

class PocketMineApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServerUtils.setContext(this)
        ServerFragment.prefs = PreferenceManager.getDefaultSharedPreferences(this)
        AssetExtractor.extractAssets(this)
    }
}

