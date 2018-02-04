package me.cooper.rick.crowdcontrollerclient.activity.friend.dummy

import java.util.*

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 *
 *
 * TODO: Replace all uses of this class before publishing your app.
 */
object DummyContent {

    private val userNames = listOf("Chris", "Tim", "Steve", "Rick", "Geoff")
    /**
     * An array of sample (dummy) items.
     */

    val FRIENDS: List<DummyItem> = (0 until 4).map(this::createDummyItem)

    private fun createDummyItem(position: Int): DummyItem {
        return DummyItem(userNames[position])
    }

    /**
     * A dummy item representing a piece of content.
     */
    class DummyItem(val content: String) {

        override fun toString(): String {
            return content
        }
    }
}
