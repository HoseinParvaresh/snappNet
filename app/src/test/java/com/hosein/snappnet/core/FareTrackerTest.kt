package com.hosein.snappnet.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FareTrackerTest {
    @Test fun duplicateSuppressedChangeDetected() {
        val t = FareTracker()
        assertTrue(t.onFare(106000))
        assertFalse(t.onFare(106000))
        assertTrue(t.onFare(29000))
        assertEquals(29000L, t.current)
    }
    @Test fun hidesOnlyAfterConsecutiveMisses() {
        val t = FareTracker(2)
        t.onFare(106000)
        assertFalse(t.onMiss())
        assertTrue(t.onMiss())
        assertNull(t.current)
        assertFalse(t.onMiss())
    }
    @Test fun fareReappearingResetsMisses() {
        val t = FareTracker(2)
        t.onFare(106000)
        assertFalse(t.onMiss())
        assertFalse(t.onFare(106000))
        assertFalse(t.onMiss())
    }
    @Test fun sameFareShownAgainAfterHide() {
        val t = FareTracker(1)
        t.onFare(50000)
        assertTrue(t.onMiss())
        assertTrue(t.onFare(50000))
    }
    @Test fun resetClears() {
        val t = FareTracker(); t.onFare(1); t.reset()
        assertFalse(t.hasFare)
    }
}
