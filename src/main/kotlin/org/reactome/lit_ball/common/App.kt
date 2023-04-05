package org.reactome.lit_ball.common

import androidx.compose.runtime.Composable

object App {
    private lateinit var map: DBType
    fun beforeWindow() {
        SerialDB.open()
        map = SerialDB.get()
    }

//    fun getDBSize() : String { return querySet.size.toString() }

    fun afterWindow() {
    }

    fun buttonNew() {}
    fun buttonInfo() {}

    @Composable
    fun buttonSettings() {
        val settingsKey = "settings"
        val settings = map[settingsKey]
        SettingsDialog(settings)
    }
    fun buttonExit() {}
}
