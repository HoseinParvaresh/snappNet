package com.hosein.snappnet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.hosein.snappnet.permission.PermissionHelper

@Composable
fun PermissionsTab(accessibilityOn: Boolean, batteryOk: Boolean, overlayPermOn: Boolean) {
    val ctx = LocalContext.current
    ScreenTitle("دسترسی‌ها", "اندروید اجازهٔ فعال‌سازی خودکار نمی‌دهد؛ هر مورد را خودتان در تنظیمات روشن کنید.")

    PermissionItem(
        title = "سرویس دسترس‌پذیری", badge = "ضروری", granted = accessibilityOn,
        desc = "برای خواندن مبلغ نمایش‌داده‌شده در اسنپ‌باکس لازم است. این سرویس فقط متن روی صفحه را می‌خواند و هیچ سفری را قبول یا رد نمی‌کند.",
        action = if (accessibilityOn) "باز کردن تنظیمات" else "فعال‌سازی در تنظیمات",
        onClick = { PermissionHelper.openAccessibilitySettings(ctx) },
    )
    PermissionItem(
        title = "استثنا از بهینه‌سازی باتری", badge = "پیشنهادی", granted = batteryOk,
        desc = "بعضی گوشی‌ها برنامه‌های پس‌زمینه را می‌بندند. برنامه را در فهرست «بدون محدودیت» قرار دهید تا سرویس پایدار بماند. در شیائومی، هواوی و سامسونگ، «شروع خودکار» را هم روشن کنید.",
        action = "باز کردن تنظیمات باتری",
        onClick = { PermissionHelper.openBatterySettings(ctx) },
    )
    PermissionItem(
        title = "نمایش روی سایر برنامه‌ها", badge = "اختیاری", granted = overlayPermOn,
        desc = "برای کارت شناور لازم نیست؛ کارت از طریق خود سرویس دسترس‌پذیری نمایش داده می‌شود. فقط اگر در گوشی شما کارت دیده نشد، آن را بدهید.",
        action = "باز کردن تنظیمات",
        onClick = { PermissionHelper.openOverlaySettings(ctx) },
    )
}

@Composable
private fun PermissionItem(title: String, badge: String, granted: Boolean, desc: String, action: String, onClick: () -> Unit) {
    AppCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(badge, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Chip(if (granted) "فعال" else "غیرفعال", granted)
        }
        Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (granted) {
            OutlinedButton(onClick, Modifier.fillMaxWidth()) { Text(action) }
        } else {
            Button(onClick, Modifier.fillMaxWidth()) { Text(action) }
        }
    }
}
