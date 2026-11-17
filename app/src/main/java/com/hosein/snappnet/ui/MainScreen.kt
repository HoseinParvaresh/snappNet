package com.hosein.snappnet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.hosein.snappnet.data.AppSettings
import com.hosein.snappnet.data.Keys
import com.hosein.snappnet.data.SettingsRepository
import com.hosein.snappnet.permission.PermissionHelper
import kotlinx.coroutines.launch

private const val TAB_HOME = 0
private const val TAB_PERMISSIONS = 1
private const val TAB_POPUP = 2
private const val TAB_SETTINGS = 3

@Composable
fun MainScreen(repo: SettingsRepository, resumeTick: Int) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val s by repo.settings.collectAsState(initial = AppSettings())
    val accessibilityOn = remember(resumeTick) { PermissionHelper.isAccessibilityEnabled(ctx) }
    val batteryOk = remember(resumeTick) { PermissionHelper.isIgnoringBatteryOptimizations(ctx) }
    val overlayPermOn = remember(resumeTick) { PermissionHelper.canDrawOverlays(ctx) }
    var tab by rememberSaveable { mutableStateOf(TAB_HOME) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                BottomBar(
                    tab = tab, onTab = { tab = it },
                    powerOn = accessibilityOn && s.detectionEnabled,
                    onPower = {
                        if (!accessibilityOn) tab = TAB_PERMISSIONS
                        else scope.launch { repo.set(Keys.DETECTION, !s.detectionEnabled) }
                    },
                )
            },
        ) { pad ->
            if (tab == TAB_POPUP) {
                // Own layout: pinned live preview on top, settings scroll underneath.
                PopupTab(
                    s,
                    onReset = { scope.launch { repo.resetPopupStyle() } },
                    onSetInt = { k, v -> scope.launch { repo.set(k, v) } },
                    onSetBool = { k, v -> scope.launch { repo.set(k, v) } },
                    modifier = Modifier.padding(pad).fillMaxSize(),
                )
            } else {
                Column(
                    Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    when (tab) {
                        TAB_HOME -> HomeTab(
                            s, accessibilityOn,
                            onUnitChange = { scope.launch { repo.set(Keys.TOMAN, it) } },
                            onOpenPermissions = { tab = TAB_PERMISSIONS },
                        )
                        TAB_PERMISSIONS -> PermissionsTab(accessibilityOn, batteryOk, overlayPermOn)
                        else -> SettingsTab(
                            s,
                            onSetBool = { k, v -> scope.launch { repo.set(k, v) } },
                            onResetAll = { scope.launch { repo.resetAll() } },
                        )
                    }
                }
            }
        }
    }
}

/** Snapp-driver-style bar: two tabs, big round on/off button in the middle, two tabs. */
@Composable
private fun BottomBar(tab: Int, onTab: (Int) -> Unit, powerOn: Boolean, onPower: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 12.dp) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val m = Modifier.weight(1f)
            NavItem("خانه", Icons.Filled.Home, tab == TAB_HOME, m) { onTab(TAB_HOME) }
            NavItem("دسترسی‌ها", Icons.Filled.Lock, tab == TAB_PERMISSIONS, m) { onTab(TAB_PERMISSIONS) }
            PowerButton(powerOn, onPower)
            NavItem("پاپ‌آپ", Icons.Filled.Create, tab == TAB_POPUP, m) { onTab(TAB_POPUP) }
            NavItem("تنظیمات", Icons.Filled.Settings, tab == TAB_SETTINGS, m) { onTab(TAB_SETTINGS) }
        }
    }
}
