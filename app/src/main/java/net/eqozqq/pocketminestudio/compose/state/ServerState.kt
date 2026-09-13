package net.eqozqq.pocketminestudio.compose.state

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ServerState {
    private val mainHandler = Handler(Looper.getMainLooper())

    var isStarted by mutableStateOf(false)
    var isServerReady by mutableStateOf(false)
    var onlinePlayers by mutableStateOf("0/0")
    var ram by mutableStateOf("0 MB")
    var upload by mutableStateOf("0")
    var download by mutableStateOf("0")
    var tps by mutableStateOf("20.0")
    var publicIp by mutableStateOf("Loading...")
    var isPublicIpBlurred by mutableStateOf(true)
    var localIp by mutableStateOf("Unknown")
    
    val players = mutableStateListOf<String>()
    val ramHistory = mutableStateListOf<Float>(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
    val uploadHistory = mutableStateListOf<Float>(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
    val downloadHistory = mutableStateListOf<Float>(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
    val consoleLogs = mutableStateListOf<String>()

    fun updateStats(online: String, newRam: String, up: String, down: String, newTps: String) {
        val rVal = try {
            newRam.split("[ /]".toRegex()).firstOrNull()?.replace("[^0-9.]".toRegex(), "")?.toFloatOrNull() ?: 0f
        } catch (_: Exception) { 0f }
        val uVal = try {
            up.split("[ /]".toRegex()).firstOrNull()?.replace("[^0-9.]".toRegex(), "")?.toFloatOrNull() ?: 0f
        } catch (_: Exception) { 0f }
        val dVal = try {
            down.split("[ /]".toRegex()).firstOrNull()?.replace("[^0-9.]".toRegex(), "")?.toFloatOrNull() ?: 0f
        } catch (_: Exception) { 0f }

        val action = Runnable {
            onlinePlayers = online
            ram = newRam
            upload = up
            download = down
            tps = newTps

            if (newRam == "0 MB" && up == "0" && down == "0") {
                ramHistory.clear()
                uploadHistory.clear()
                downloadHistory.clear()
                repeat(10) {
                    ramHistory.add(0f)
                    uploadHistory.add(0f)
                    downloadHistory.add(0f)
                }
            } else {
                ramHistory.add(rVal)
                if (ramHistory.size > 60) ramHistory.removeAt(0)

                uploadHistory.add(uVal)
                if (uploadHistory.size > 60) uploadHistory.removeAt(0)

                downloadHistory.add(dVal)
                if (downloadHistory.size > 60) downloadHistory.removeAt(0)
            }
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run()
        } else {
            mainHandler.post(action)
        }
    }

    fun addLog(line: String) {
        val action = Runnable {
            if (consoleLogs.size > 1500) {
                consoleLogs.removeAt(0)
            }
            consoleLogs.add(line)
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run()
        } else {
            mainHandler.post(action)
        }
    }

    fun clearLogs() {
        val action = Runnable {
            consoleLogs.clear()
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run()
        } else {
            mainHandler.post(action)
        }
    }
}
