package com.xiaobei206.hongguotvnext.model

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedTest {
    @Test
    fun removesDuplicateIdsAndStopsWhenTheNextPageAddsNothing() = runTest {
        val first = Card(id = "series-1", title = "A")
        var calls = 0
        val feed = Feed(this) { cursor ->
            calls++
            if (cursor.isEmpty()) {
                FeedPage(listOf(first, first), cursor = "page-2", more = true)
            } else {
                FeedPage(listOf(first), cursor = "page-3", more = true)
            }
        }

        feed.loadMore()
        runCurrent()
        assertEquals(1, calls)
        assertEquals(listOf("series-1"), feed.cards.map { it.id })
        assertTrue(feed.more)

        feed.loadMore()
        runCurrent()
        assertEquals(2, calls)
        assertEquals(1, feed.cards.size)
        assertFalse("A page without new card IDs must stop automatic pagination", feed.more)
    }

    @Test
    fun staticFeedDoesNotRequestMorePages() = runTest {
        val feed = Feed.of(this, listOf(Card(id = "cached", title = "Cached")))
        feed.loadMore()
        runCurrent()

        assertEquals(1, feed.cards.size)
        assertFalse(feed.more)
        assertFalse(feed.loading)
    }
}
