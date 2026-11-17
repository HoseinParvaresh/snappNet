package com.hosein.snappnet.core

/**
 * Which foreground packages count as Snapp Box. Known driver app id: com.snappbox.bikerapp
 * (reported by the user; the loose fallback match below covers other variants).
 */
object SnappPackages {
    const val DRIVER_PACKAGE = "com.snappbox.bikerapp"

    fun isSnappBox(pkg: String): Boolean =
        pkg == DRIVER_PACKAGE ||
        pkg.contains("snappbox", ignoreCase = true) ||
            (pkg.startsWith("cab.snapp", ignoreCase = true) && pkg.contains("box", ignoreCase = true))

    /** Packages whose window changes must not be treated as "left Snapp Box". */
    fun isTransient(pkg: String): Boolean =
        pkg == "com.android.systemui" || pkg.contains("inputmethod", true) || pkg.contains("keyboard", true)
}
