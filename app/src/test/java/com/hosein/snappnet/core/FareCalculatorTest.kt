package com.hosein.snappnet.core

import org.junit.Assert.assertEquals
import org.junit.Test

class FareCalculatorTest {
    @Test fun specExamples() {
        assertEquals(82680L, FareCalculator.netEarnings(106000))
        assertEquals(22620L, FareCalculator.netEarnings(29000))
        assertEquals(55770L, FareCalculator.netEarnings(71500))
        assertEquals(78000L, FareCalculator.netEarnings(100000))
        assertEquals(39000L, FareCalculator.netEarnings(50000))
    }
    @Test fun roundsHalfUpWithoutFloats() {
        assertEquals(1L, FareCalculator.netEarnings(1))      // 0.78 -> 1
        assertEquals(78L, FareCalculator.netEarnings(100))
        assertEquals(0L, FareCalculator.netEarnings(0))
        assertEquals(78_000_000L, FareCalculator.netEarnings(100_000_000))
    }
    @Test fun customRate() {
        assertEquals(80000L, FareCalculator.netEarnings(100000, 80))
        assertEquals(100000L, FareCalculator.netEarnings(100000, 100))
        assertEquals(22, FareCalculator.deductionPercent(78))
    }
    @Test(expected = IllegalArgumentException::class) fun rejectsNegativeFare() { FareCalculator.netEarnings(-1) }
    @Test(expected = IllegalArgumentException::class) fun rejectsBadRate() { FareCalculator.netEarnings(1000, 0) }
    @Test fun breakdownFromRealScreenshots() {
        val t = FareCalculator.breakdown(330000, 78, inToman = true)
        assertEquals(33000L, t.gross); assertEquals(25740L, t.net); assertEquals(22, t.deductionPercent)
        val r = FareCalculator.breakdown(330000, 78, inToman = false)
        assertEquals(330000L, r.gross); assertEquals(257400L, r.net)
        assertEquals(22620L, FareCalculator.breakdown(290000, 78, true).net)
    }
}
