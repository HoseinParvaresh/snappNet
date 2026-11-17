package com.hosein.snappnet.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hosein.snappnet.R
import com.hosein.snappnet.core.FareCalculator
import com.hosein.snappnet.core.NumberNormalizer
import com.hosein.snappnet.data.AppSettings

@Composable
fun HomeTab(
    s: AppSettings,
    accessibilityOn: Boolean,
    onUnitChange: (inToman: Boolean) -> Unit,
    onOpenPermissions: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Image(
            painterResource(R.drawable.app_logo), contentDescription = null,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)),
        )
        Column {
            Text("محاسبه درآمد سفر اسنپ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("درآمد خالص را همان لحظه ببینید", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    StatusHero(accessibilityOn, s.detectionEnabled, onOpenPermissions)

    AppCard("واحد نمایش مبلغ", "درآمد خالص و کرایه، در کارت شناور و ماشین‌حساب با این واحد نشان داده می‌شود.") {
        RadioRow("تومان", s.displayInToman) { onUnitChange(true) }
        RadioRow("ریال", !s.displayInToman) { onUnitChange(false) }
    }

    CalculatorCard(s.displayInToman)
}

@Composable
private fun StatusHero(accessibilityOn: Boolean, detectionOn: Boolean, onOpenPermissions: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val state = when {
        !accessibilityOn -> 0
        !detectionOn -> 1
        else -> 2
    }
    val container = when (state) {
        0 -> Danger.copy(alpha = 0.12f)
        1 -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val accent = when (state) { 0 -> Danger; 1 -> MaterialTheme.colorScheme.onSurfaceVariant; else -> primary }
    val title = when (state) { 0 -> "نیاز به دسترسی"; 1 -> "متوقف شده"; else -> "فعال و آماده" }
    val body = when (state) {
        0 -> "برای خواندن مبلغ سفر، سرویس دسترس‌پذیری را در بخش دسترسی‌ها روشن کنید."
        1 -> "برای شروع، دکمهٔ وسط پایین صفحه را بزنید."
        else -> "اسنپ‌باکس را باز کنید؛ با آمدن پیشنهاد سفر، درآمد خالص خودکار نمایش داده می‌شود."
    }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = container)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = accent, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            if (state == 0) {
                Button(onOpenPermissions, Modifier.fillMaxWidth()) { Text("رفتن به دسترسی‌ها") }
            }
        }
    }
}

@Composable
private fun CalculatorCard(inToman: Boolean) {
    var input by rememberSaveable { mutableStateOf("") }
    val unit = if (inToman) "تومان" else "ریال"
    val gross = NumberNormalizer.normalize(input).toLongOrNull()

    AppCard("ماشین‌حساب درآمد", "مبلغ اصلی را دستی وارد کنید تا درآمد خالص را ببینید.") {
        OutlinedTextField(
            value = input,
            onValueChange = { v -> input = v.filter { it.isDigit() }.take(12) },
            label = { Text("مبلغ اصلی ($unit)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        if (gross != null && gross > 0) {
            val net = FareCalculator.netEarnings(gross)
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer).padding(14.dp),
            ) {
                Text("درآمد خالص", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    "${NumberNormalizer.format(net, true)} $unit",
                    fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "کسورات: ${NumberNormalizer.format(FareCalculator.deductionPercent(FareCalculator.DEFAULT_RATE_PERCENT).toLong(), true)}٪",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}
