package com.hosein.snappnet.core

/** A visible text plus how much screen it covers (px²). The main offer card's big price covers far more than a small badge. */
data class ScreenText(val text: String, val area: Long = 0L)
