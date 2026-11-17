package com.hosein.snappnet.core

/** Suppresses duplicate fares and decides when a fare has really disappeared. Single-threaded use. */
class FareTracker(private val missesToHide: Int = 2) {
    var current: Long? = null
        private set
    private var misses = 0

    val hasFare: Boolean get() = current != null

    /** @return true if the fare differs from the one currently shown (UI must update). */
    fun onFare(fare: Long): Boolean {
        misses = 0
        if (current == fare) return false
        current = fare
        return true
    }

    /** A full-screen scan found no fare. @return true when the shown fare must now be hidden. */
    fun onMiss(): Boolean {
        if (current == null) return false
        misses++
        if (misses >= missesToHide) { reset(); return true }
        return false
    }

    fun reset() { current = null; misses = 0 }
}
