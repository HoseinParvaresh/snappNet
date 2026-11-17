package com.hosein.snappnet.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mock accessibility tree. NOT real Snapp Box data: it only proves traversal + extraction logic. */
class NodeFlattenerTest {
    class Fake(
        override val text: CharSequence? = null,
        override val description: CharSequence? = null,
        override val isVisible: Boolean = true,
        private val area: Long = 0L,
        private val kids: List<Fake> = emptyList(),
    ) : TreeNode {
        var released = false
        override val visibleArea: Long get() = area
        override val childCount get() = kids.size
        override fun childAt(index: Int): TreeNode? = kids.getOrNull(index)
        override fun release() { released = true }
    }

    @Test fun collectsTextThenDescriptionInOrderAndSkipsHidden() {
        val offer = Fake(kids = listOf(
            Fake(text = "کرایه"),
            Fake(description = "۱۰۶٬۰۰۰ تومان"),
            Fake(text = "قبول درخواست"),
            Fake(text = "پنهان ۹۹٬۰۰۰ تومان", isVisible = false),
            Fake(kids = listOf(Fake(text = "مقصد"))),
        ))
        val out = ArrayList<ScreenText>()
        NodeFlattener.collect(offer, out)
        assertEquals(listOf("کرایه", "۱۰۶٬۰۰۰ تومان", "قبول درخواست", "مقصد"), out.map { it.text })
        assertEquals(106000L, FareExtractor.extract(out)?.amount)
    }
    @Test fun stackedOffersPickTheMainCardNotTheBadge() {
        // modelled on the screenshots: small "new offer" badge (425,000) + big main card price (395,000)
        val screen = Fake(kids = listOf(
            Fake(kids = listOf(Fake(text = "جدید", area = 800), Fake(text = "۴۲۵,۰۰۰", area = 3_000), Fake(text = "ریال", area = 1_200))),
            Fake(kids = listOf(Fake(text = "۳۹۵,۰۰۰", area = 14_000), Fake(text = "ریال", area = 3_500),
                Fake(text = "قبول درخواست سوپرمارکت", area = 60_000))),
        ))
        val out = ArrayList<ScreenText>()
        NodeFlattener.collect(screen, out)
        assertEquals(395000L, FareExtractor.extract(out)?.rial)
    }
    @Test fun releasesChildrenButNotRoot() {
        val child = Fake(text = "x")
        val root = Fake(kids = listOf(child))
        NodeFlattener.collect(root, ArrayList())
        assertTrue(child.released)
        assertEquals(false, root.released)
    }
    @Test fun nodeBudgetIsEnforced() {
        val wide = Fake(kids = List(2000) { Fake(text = "n$it") })
        val out = ArrayList<ScreenText>()
        NodeFlattener.collect(wide, out)
        assertTrue(out.size <= NodeFlattener.MAX_NODES)
    }
    @Test fun snappPackageMatching() {
        assertTrue(SnappPackages.isSnappBox("com.snappbox.bikerapp"))
        assertTrue(SnappPackages.isSnappBox("cab.snapp.snappbox"))
        assertTrue(!SnappPackages.isSnappBox("cab.snapp.passenger"))
        assertTrue(!SnappPackages.isSnappBox("com.whatsapp"))
        assertTrue(SnappPackages.isTransient("com.android.systemui"))
    }
}
