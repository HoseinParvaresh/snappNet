package com.hosein.snappnet.ui

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.hosein.snappnet.data.AppSettings
import com.hosein.snappnet.data.AppState
import com.hosein.snappnet.data.Keys
import androidx.datastore.preferences.core.Preferences

@Composable
fun SettingsTab(s: AppSettings, onSetBool: (Preferences.Key<Boolean>, Boolean) -> Unit, onResetAll: () -> Unit) {
    val ctx = LocalContext.current
    val version = remember(ctx) {
        runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "-"
    }
    ScreenTitle("تنظیمات")

    AppCard("کارت شناور") {
        SwitchRow("نمایش کارت درآمد خالص", s.overlayEnabled, "اگر خاموش باشد، تشخیص ادامه دارد اما کارتی نمایش داده نمی‌شود.") {
            onSetBool(Keys.OVERLAY, it)
        }
    }

    AppCard("ابزار پیشرفته", "فقط برای بررسی مشکل. حالت عیب‌یابی متن‌هایی را که سرویس از اسنپ‌باکس می‌خواند نشان می‌دهد.") {
        SwitchRow("حالت عیب‌یابی", s.debugMode) { onSetBool(Keys.DEBUG, it) }
        if (s.debugMode) DiagnosticsPanel()
    }

    AppCard("دربارهٔ برنامه") {
        Text("محاسبه درآمد سفر اسنپ — نسخهٔ $version", style = MaterialTheme.typography.bodyMedium)
        Text(
            "این برنامه هیچ سفری را قبول یا رد نمی‌کند و داده‌ای به اینترنت نمی‌فرستد؛ همهٔ محاسبه‌ها روی گوشی انجام می‌شود.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onResetAll, Modifier.fillMaxWidth()) { Text("بازنشانی همهٔ تنظیمات") }
    }
}

@Composable
private fun DiagnosticsPanel() {
    val diag by AppState.diag.collectAsState()
    val clipboard = LocalClipboardManager.current
    val report = buildString {
        appendLine("events=${diag.events} scans=${diag.scans}")
        appendLine("snappPkg=${diag.snappPackage} otherPkg=${diag.otherPackage} fare=${diag.extractedFare}")
        appendLine("windows=${diag.windows}")
        appendLine("--- texts ---")
        diag.texts.forEach { appendLine(it) }
    }
    Text("متن‌های خوانده‌شده", fontWeight = FontWeight.SemiBold)
    SelectionContainer { Text(report, style = MaterialTheme.typography.bodySmall) }
    OutlinedButton({ clipboard.setText(AnnotatedString(report)) }, Modifier.fillMaxWidth()) { Text("کپی گزارش") }
}
