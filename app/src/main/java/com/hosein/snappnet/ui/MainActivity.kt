package com.hosein.snappnet.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.hosein.snappnet.data.SettingsRepository

class MainActivity : ComponentActivity() {
    private var resumeTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = SettingsRepository(this)
        setContent { SnappNetTheme { MainScreen(repo, resumeTick) } }
    }

    // Re-check permission status when returning from Android Settings.
    override fun onResume() {
        super.onResume()
        resumeTick++
    }
}
