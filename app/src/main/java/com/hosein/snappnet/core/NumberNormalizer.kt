package com.hosein.snappnet.core

/** Persian/Arabic-Indic/ASCII digit handling, number discovery and formatting. */
object NumberNormalizer {
    data class NumberToken(val value: Long, val start: Int, val end: Int)

    // Not preceded by digit/comma/dot; not followed by "," or "." plus a digit (rejects 1,234,56 and 106000.5).
    private val NUMBER = Regex("""(?<![\d,.])(\d{1,3}(?:,\d{3})+|\d+)(?![\d,.]\d)(?!\d)""")

    /**
     * Length-preserving normalisation: all digits become ASCII, thousands separators become ','
     * and invisible/space-like marks become ' '. Arabic yeh/kaf are mapped to their Persian forms.
     */
    fun normalize(text: CharSequence): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            sb.append(
                when (ch) {
                    in '\u06F0'..'\u06F9' -> '0' + (ch - '\u06F0')
                    in '\u0660'..'\u0669' -> '0' + (ch - '\u0660')
                    '\u066C', '\u060C' -> ','
                    '\u066B' -> '.'
                    '\u200C', '\u200E', '\u200F', '\u00A0' -> ' '
                    '\u064A' -> '\u06CC'
                    '\u0643' -> '\u06A9'
                    else -> ch
                }
            )
        }
        return sb.toString()
    }

    /** Finds integer amounts in already-normalised text. Rejects leading-zero tokens (phone numbers). */
    fun findNumbers(normalized: String): List<NumberToken> {
        val out = ArrayList<NumberToken>(2)
        for (m in NUMBER.findAll(normalized)) {
            val raw = m.value
            if (!raw.contains(',') && raw.length > 1 && raw[0] == '0') continue
            val v = raw.replace(",", "").toLongOrNull() ?: continue
            out.add(NumberToken(v, m.range.first, m.range.last + 1))
        }
        return out
    }

    /** Normalise then parse a text that is expected to be exactly one number. */
    fun parseSingle(text: CharSequence): Long? {
        val t = normalize(text).trim()
        val tokens = findNumbers(t)
        return if (tokens.size == 1 && tokens[0].start == 0 && tokens[0].end == t.length) tokens[0].value else null
    }

    /** 106000 -> "106,000" (or "۱۰۶٬۰۰۰" with persianDigits = true). */
    fun format(value: Long, persianDigits: Boolean = false): String {
        val digits = Math.abs(value).toString()
        val sep = if (persianDigits) '\u066C' else ','
        val sb = StringBuilder()
        for ((i, c) in digits.withIndex()) {
            if (i > 0 && (digits.length - i) % 3 == 0) sb.append(sep)
            sb.append(if (persianDigits) '\u06F0' + (c - '0') else c)
        }
        return if (value < 0) "-$sb" else sb.toString()
    }
}
