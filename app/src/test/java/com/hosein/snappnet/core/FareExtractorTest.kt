package com.hosein.snappnet.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FareExtractorTest {
    private val accept = "قبول درخواست"
    /** Adds the accept-button text, as on a real offer screen. */
    private fun ex(vararg t: String) = FareExtractor.extract(t.toList() + accept)
    private fun rial(vararg t: String) = ex(*t)?.rial

    // ---- layouts modelled on the real screenshots (330,000 / 290,000 rial) ----
    @Test fun realOfferNumberAndRialInSeparateNodes() {
        assertEquals(330000L, rial("۳۳۰,۰۰۰", "ریال", "ریشهر", "ریشهر، سلمان فارسی، پارسیان، دانشگاه علوم پزشکی بوشهر..."))
        assertEquals(290000L, rial("سوپرمارکت", "۲۹۰,۰۰۰", "ریال", "بوشهر بلوار خلیج فارس کوچه دریا ۱ ساختمان دلفین پ ۲۱۵"))
    }
    @Test fun realOfferNumberAndRialInOneNode() {
        assertEquals(330000L, rial("330,000 ریال"))
        assertEquals(330000L, rial("ریال ۳۳۰٬۰۰۰"))
    }
    @Test fun rialOrderReversedInTree() {
        assertEquals(330000L, rial("ریال", "۳۳۰,۰۰۰"))
    }
    @Test fun unitIsReported() {
        assertEquals(MoneyUnit.RIAL, ex("۳۳۰,۰۰۰", "ریال")?.unit)
        assertEquals(MoneyUnit.TOMAN, ex("۳۳٬۰۰۰ تومان")?.unit)
        assertEquals(330000L, rial("۳۳٬۰۰۰ تومان")) // toman -> rial
    }
    @Test fun idleScreenWithBalanceIsIgnored() {
        // wallet balance shown in rial on the idle map screen: no accept button -> no fare
        assertNull(FareExtractor.extract(listOf("نمایش اعتبار", "۱۲۰٬۰۰۰", "ریال", "مالی", "سفارش‌ها")))
    }
    @Test fun ignoresUnrelatedNumbers() {
        assertNull(ex("09123456789", "پلاک ۲۱۵", "1403/05/02", "10:15", "شناسه سفر 8844123"))
        assertNull(ex("۳۳۰٬۰۰۰")) // no currency
    }
    @Test fun outOfRangeIsRejected() {
        assertNull(ex("۵۰۰ ریال"))
        assertNull(ex("۹۰٬۰۰۰٬۰۰۰ ریال"))
        assertNull(ex("۹٬۰۰۰٬۰۰۰ تومان"))
    }
    @Test fun labelWinsOverOtherAmounts() {
        assertEquals(330000L, rial("پاداش ۱۰۰٬۰۰۰ ریال", "کرایه", "۳۳۰٬۰۰۰ ریال"))
    }
    @Test fun largerOnScreenAmountWinsOverBadge() {
        val t = listOf(
            ScreenText("۴۲۵,۰۰۰", 3_000), ScreenText("ریال", 1_000),
            ScreenText("۳۹۵,۰۰۰", 14_000), ScreenText("ریال", 3_000), ScreenText("قبول درخواست", 50_000),
        )
        assertEquals(395000L, FareExtractor.extract(t)?.rial)
    }
    @Test fun partlyOffscreenNeighbourCardLoses() {
        val t = listOf(
            ScreenText("۳۹۵,۰۰۰", 14_000), ScreenText("ریال", 3_000),
            ScreenText("۲۹۰,۰۰۰", 2_500), ScreenText("ریال", 700), ScreenText("قبول درخواست", 50_000),
        )
        assertEquals(395000L, FareExtractor.extract(t)?.rial)
    }
    @Test fun ambiguousFaresGiveNothing() {
        assertNull(ex("۳۳۰٬۰۰۰ ریال", "۲۹۰٬۰۰۰ ریال"))
    }
    @Test fun sameFareRepeatedIsNotAmbiguous() {
        assertEquals(330000L, rial("۳۳۰٬۰۰۰ ریال", "۳۳۰٬۰۰۰ ریال"))
    }
    @Test fun bothUnitsInOneTextIsNotGuessed() {
        assertNull(ex("۳۳۰٬۰۰۰ ریال (۳۳٬۰۰۰ تومان)"))
    }
    @Test fun emptyInput() {
        assertNull(FareExtractor.extract(emptyList<String>()))
        assertNull(ex("", "ریال"))
    }
}
