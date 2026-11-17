package com.hosein.snappnet.core

/** Net earnings = displayed fare x rate. Integer arithmetic only, rounded half-up. */
object FareCalculator {
    const val DEFAULT_RATE_PERCENT = 78

    fun netEarnings(fare: Long, ratePercent: Int = DEFAULT_RATE_PERCENT): Long {
        require(fare >= 0) { "fare must be >= 0" }
        require(ratePercent in 1..100) { "rate must be in 1..100" }
        return (fare * ratePercent + 50) / 100
    }

    data class Breakdown(val gross: Long, val net: Long, val deductionPercent: Int)

    fun rialToToman(rial: Long): Long = (rial + 5) / 10

    /** Gross/net in the chosen display unit. Snapp Box shows rials; toman = rial / 10. */
    fun breakdown(grossRial: Long, ratePercent: Int, inToman: Boolean): Breakdown {
        val gross = if (inToman) rialToToman(grossRial) else grossRial
        return Breakdown(gross, netEarnings(gross, ratePercent), deductionPercent(ratePercent))
    }

    fun deductionPercent(ratePercent: Int): Int = 100 - ratePercent
}
