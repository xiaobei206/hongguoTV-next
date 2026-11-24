package com.xiaobei206.hongguotvnext.model

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedTest {
    @Test
    fun removesDuplicateIdsAndStopsWhenTheNextPageAddsNothing() = runTest {
        val first = Card(id = "series-1", title = "A")
        val feed = Feed(backgroundScope) { cursor ->
            if (cursor.isEmpty()) {
                FeedPage(listOf(first, first), cursor = "page-2", more = true)
            } else {
                FeedPage(listOf(first), cursor = "page-3", more = true)
            }
        }

        feed.loadMore()
        advanceUntilIdle()
        assertEquals(listOf("series-1"), feed.cards.map { it.id })
        assertTrue(feed.more)

        feed.loadMore()
        advanceUntilIdle()
        assertEquals(1, feed.cards.size)
        assertFalse("A page without new card IDs must stop automatic pagination", feed.more)
    }

    @Test
    fun staticFeedDoesNotRequestMorePages() = runTest {
        val feed = Feed.of(backgroundScope, listOf(Card(id = "cached", title = "Cached")))
        feed.loadMore()
        advanceUntilIdle()

        assertEquals(1, feed.cards.size)
        assertFalse(feed.more)
        assertFalse(feed.loading)
    }
}
