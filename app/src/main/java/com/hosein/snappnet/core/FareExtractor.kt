package com.hosein.snappnet.core

enum class MoneyUnit { RIAL, TOMAN }

data class Fare(val amount: Long, val unit: MoneyUnit) {
    val rial: Long get() = if (unit == MoneyUnit.TOMAN) amount * 10 else amount
}

/**
 * Picks the trip fare out of the visible texts of a screen (in tree order).
 *
 * Rules (based on real Snapp Box offer screens, which show e.g. "۳۳۰,۰۰۰" next to "ریال"):
 *  - the screen must contain the accept button text ("قبول ..."), so idle screens (e.g. wallet balance) are ignored;
 *  - a number is a candidate only if tied to a currency word (ریال / تومان / Rial / Toman), in the same text or,
 *    for a number-only text, in the neighbouring text, and is inside the plausible range for that unit;
 *  - fare-ish labels nearby raise the score;
 *  - when several amounts are on screen (a swipeable stack of offers also shows a small "new offer" badge and
 *    the neighbouring card), the one covering the largest screen area wins: that is the main card's price.
 *    Only a complete tie between different amounts returns null (no guessing).
 */
object FareExtractor {
    const val MIN_TOMAN = 5_000L
    const val MAX_TOMAN = 3_000_000L
    const val MIN_RIAL = 50_000L
    const val MAX_RIAL = 30_000_000L

    private val TOMAN_WORDS = arrayOf("تومان", "تومن", "toman")
    private val RIAL_WORDS = arrayOf("ریال", "rial")
    private val LABELS = arrayOf("کرایه", "مبلغ", "هزینه", "قیمت", "fare", "price", "amount")
    private val ANCHORS = arrayOf("قبول")

    @JvmName("extractFromStrings")
    fun extract(texts: List<String>): Fare? = extract(texts.map { ScreenText(it) })

    fun extract(texts: List<ScreenText>): Fare? {
        val n = texts.size
        if (n == 0) return null
        val norm = ArrayList<String>(n)
        var anchored = false
        for (t in texts) {
            val s = NumberNormalizer.normalize(t.text)
            norm.add(s)
            if (!anchored && containsAny(s, ANCHORS)) anchored = true
        }
        if (!anchored) return null

        var best: Fare? = null
        var bestScore = 0
        var bestSize = -1L
        var ambiguous = false

        for (i in 0 until n) {
            val t = norm[i]
            val tokens = NumberNormalizer.findNumbers(t)
            if (tokens.isEmpty()) continue
            for (tok in tokens) {
                val numberOnly = t.substring(0, tok.start).isBlank() && t.substring(tok.end).isBlank()
                var unit = unitOf(t)
                if (unit == null && numberOnly) {
                    unit = (if (i + 1 < n) unitOf(norm[i + 1]) else null)
                        ?: (if (i > 0) unitOf(norm[i - 1]) else null)
                }
                if (unit == null) continue
                val ok = if (unit == MoneyUnit.RIAL) tok.value in MIN_RIAL..MAX_RIAL else tok.value in MIN_TOMAN..MAX_TOMAN
                if (!ok) continue

                var score = 1
                if (containsAny(t, LABELS) || (i > 0 && containsAny(norm[i - 1], LABELS))) score += 2
                else if (i + 1 < n && containsAny(norm[i + 1], LABELS)) score += 1

                val fare = Fare(tok.value, unit)
                val size = texts[i].area
                if (score > bestScore || (score == bestScore && size > bestSize)) {
                    best = fare; bestScore = score; bestSize = size; ambiguous = false
                } else if (score == bestScore && size == bestSize && fare.rial != best?.rial) {
                    ambiguous = true
                }
            }
        }
        return if (ambiguous) null else best
    }

    /** Unit named in this text; null if none, or if both units are named (can't tell which one the number has). */
    private fun unitOf(s: String): MoneyUnit? {
        val t = containsAny(s, TOMAN_WORDS)
        val r = containsAny(s, RIAL_WORDS)
        return when {
            t && !r -> MoneyUnit.TOMAN
            r && !t -> MoneyUnit.RIAL
            else -> null
        }
    }

    private fun containsAny(s: String, words: Array<String>): Boolean {
        for (w in words) if (s.contains(w, ignoreCase = true)) return true
        return false
    }
}
