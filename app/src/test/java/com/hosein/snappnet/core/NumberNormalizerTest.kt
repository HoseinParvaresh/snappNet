package com.hosein.snappnet.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumberNormalizerTest {
    @Test fun parsesAllRequiredForms() {
        assertEquals(106000L, NumberNormalizer.parseSingle("۱۰۶٬۰۰۰"))
        assertEquals(106000L, NumberNormalizer.parseSingle("106,000"))
        assertEquals(106000L, NumberNormalizer.parseSingle("106000"))
        assertEquals(106000L, NumberNormalizer.parseSingle("۱۰۶۰۰۰"))
        assertEquals(106000L, NumberNormalizer.parseSingle("١٠٦٬٠٠٠"))      // Arabic-Indic
        assertEquals(106000L, NumberNormalizer.parseSingle("۱۰۶،۰۰۰"))      // Arabic comma
        assertEquals(106000L, NumberNormalizer.parseSingle("\u200F۱۰۶٬۰۰۰ "))  // RLM + space
    }
    @Test fun rejectsInvalidNumbers() {
        assertNull(NumberNormalizer.parseSingle("09123456789"))   // phone, leading zero
        assertNull(NumberNormalizer.parseSingle("1,23,456"))      // bad grouping
        assertNull(NumberNormalizer.parseSingle("106000.5"))      // decimal
        assertNull(NumberNormalizer.parseSingle("abc"))
        assertNull(NumberNormalizer.parseSingle(""))
        assertNull(NumberNormalizer.parseSingle("12 34"))
    }
    @Test fun findsNumbersInsideText() {
        val t = NumberNormalizer.normalize("کرایه: ۱۰۶٬۰۰۰ تومان")
        val tokens = NumberNormalizer.findNumbers(t)
        assertEquals(1, tokens.size)
        assertEquals(106000L, tokens[0].value)
    }
    @Test fun normalizeKeepsLength() {
        val s = "ك ي ۱۰۶٬۰۰۰"
        assertEquals(s.length, NumberNormalizer.normalize(s).length)
    }
    @Test fun formatting() {
        assertEquals("106,000", NumberNormalizer.format(106000))
        assertEquals("۱۰۶٬۰۰۰", NumberNormalizer.format(106000, persianDigits = true))
        assertEquals("999", NumberNormalizer.format(999))
        assertEquals("1,000", NumberNormalizer.format(1000))
        assertEquals("1,234,567", NumberNormalizer.format(1234567))
        assertEquals("0", NumberNormalizer.format(0))
        assertEquals("-1,500", NumberNormalizer.format(-1500))
    }
}
