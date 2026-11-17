package com.hosein.snappnet.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hosein.snappnet.core.FareCalculator
import com.hosein.snappnet.core.NumberNormalizer
import com.hosein.snappnet.data.AppSettings
import com.hosein.snappnet.data.Keys
import androidx.datastore.preferences.core.Preferences

/** Placeholder amount used ONLY to draw the preview below; never shown in the real floating card. */
private const val PREVIEW_FARE_RIAL = 330_000L

@Composable
fun PopupTab(
    s: AppSettings,
    onReset: () -> Unit,
    onSetInt: (Preferences.Key<Int>, Int) -> Unit,
    onSetBool: (Preferences.Key<Boolean>, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val st = s.style
    // Draft look: follows sliders while dragging so the preview reacts instantly; saved when the slider is released.
    var draft by remember(s.style) { mutableStateOf(s.style) }

    Column(modifier) {
        // Pinned: stays in view while the settings below scroll.
        Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("پیش‌نمایش زنده", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                PopupPreview(s.copy(style = draft))
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ScreenTitle("پاپ‌آپ", "ظاهر کارت شناور را کاملاً مطابق سلیقهٔ خودتان تنظیم کنید.")

    AppCard("رنگ‌ها") {
        ColorRow("رنگ پس‌زمینه", st.bgColor) { onSetInt(Keys.BG, it) }
        ColorRow("رنگ درآمد خالص", st.netColor) { onSetInt(Keys.NET_COLOR, it) }
        ColorRow("رنگ متن‌ها", st.textColor) { onSetInt(Keys.TEXT_COLOR, it) }
    }

    AppCard("اندازه و شکل") {
        fun n(v: Int) = NumberNormalizer.format(v.toLong(), true)
        SliderRow("شفافیت پس‌زمینه", st.bgAlpha, 20..100, { "${n(it)}٪" }, onLive = { draft = draft.copy(bgAlpha = it) }) { onSetInt(Keys.BG_ALPHA, it) }
        SliderRow("اندازهٔ متن درآمد خالص", st.netSize, 14..56, { n(it) }, onLive = { draft = draft.copy(netSize = it) }) { onSetInt(Keys.NET_SIZE, it) }
        SliderRow("اندازهٔ سایر متن‌ها", st.textSize, 8..24, { n(it) }, onLive = { draft = draft.copy(textSize = it) }) { onSetInt(Keys.TEXT_SIZE, it) }
        SliderRow("گردی گوشه‌ها", st.corner, 0..40, { n(it) }, onLive = { draft = draft.copy(corner = it) }) { onSetInt(Keys.CORNER, it) }
        SliderRow("فاصلهٔ داخلی", st.padding, 4..32, { n(it) }, onLive = { draft = draft.copy(padding = it) }) { onSetInt(Keys.PADDING, it) }
        SliderRow("عرض کارت (۰ = خودکار)", st.widthDp, 0..420, { if (it == 0) "خودکار" else n(it) }, onLive = { draft = draft.copy(widthDp = it) }) { onSetInt(Keys.WIDTH, it) }
    }

    AppCard("محتوای کارت") {
        SwitchRow("نمایش عنوان «درآمد خالص»", s.showTitle) { onSetBool(Keys.TITLE, it) }
        SwitchRow("نمایش کرایهٔ اصلی", s.showGross) { onSetBool(Keys.GROSS, it) }
        SwitchRow("نمایش درصد کسورات", s.showDeductions) { onSetBool(Keys.DEDUCTIONS, it) }
        SwitchRow("شروع به‌صورت کوچک‌شده", s.startMinimized, "فقط درآمد خالص نشان داده می‌شود؛ با دکمهٔ + باز می‌شود.") { onSetBool(Keys.START_MIN, it) }
    }

    AppCard("هشدار درآمد کم", "اگر درآمد خالص سفر کمتر از این مقدار باشد، عدد کارت قرمز می‌شود. صفر یعنی خاموش.") {
        var text by remember(s.lowThresholdToman) { mutableStateOf(if (s.lowThresholdToman == 0) "" else s.lowThresholdToman.toString()) }
        OutlinedTextField(
            value = text,
            onValueChange = { v ->
                val d = v.filter { it.isDigit() }.take(8)
                text = d
                onSetInt(Keys.LOW, NumberNormalizer.normalize(d).toIntOrNull() ?: 0)
            },
            label = { Text("حداقل درآمد خالص (تومان)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
    }

    OutlinedButton(onReset, Modifier.fillMaxWidth()) { Text("بازگشت ظاهر پاپ‌آپ به حالت پیش‌فرض") }
    Text(
        "جای کارت را می‌توانید هنگام نمایش، با انگشت جابه‌جا کنید؛ جایگاه ذخیره می‌شود.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
        }
    }
}

@Composable
private fun PopupPreview(s: AppSettings) {
    val st = s.style
    val b = FareCalculator.breakdown(PREVIEW_FARE_RIAL, FareCalculator.DEFAULT_RATE_PERCENT, s.displayInToman)
    val unit = if (s.displayInToman) "تومان" else "ریال"
    val low = s.lowThresholdToman > 0 && FareCalculator.netEarnings(PREVIEW_FARE_RIAL) < s.lowThresholdToman.toLong() * 10
    val bg = Color((((st.bgAlpha * 255) / 100) shl 24) or st.bgColor)
    val text = Color(0xFF000000.toInt() or st.textColor)
    val netColor = if (low) Color(0xFFFF5252) else Color(0xFF000000.toInt() or st.netColor)
    val shape = RoundedCornerShape(st.corner.dp)

    Box(
        Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF9AA5B1)).padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.then(if (st.widthDp > 0) Modifier.width(st.widthDp.dp) else Modifier)
                .clip(shape).background(bg).border(1.dp, Color(0x33FFFFFF), shape).padding(st.padding.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (s.showTitle) Text("درآمد خالص", color = text, fontSize = st.textSize.sp)
                Text("–", color = text, fontSize = (st.textSize + 6).sp)
            }
            Text(
                "${NumberNormalizer.format(b.net, true)} $unit",
                color = netColor, fontSize = st.netSize.sp, fontWeight = FontWeight.Bold,
            )
            if (s.showGross) Text("کرایه نمایش‌داده‌شده: ${NumberNormalizer.format(b.gross, true)} $unit", color = text, fontSize = st.textSize.sp)
            if (s.showDeductions) Text("کسورات: ${NumberNormalizer.format(b.deductionPercent.toLong(), true)}٪", color = text, fontSize = st.textSize.sp)
        }
    }
}
