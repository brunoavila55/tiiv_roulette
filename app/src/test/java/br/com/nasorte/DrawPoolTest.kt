package br.com.nasorte

import org.junit.Assert.*
import org.junit.Test
import java.util.Random

class DrawPoolTest {
    @Test fun noRepeatExcludesOnlyDrawnIds() {
        assertEquals(listOf("b", "c"), DrawPool.eligible(listOf("a", "b", "c"), setOf("a", "deleted"), true))
    }
    @Test fun repeatAllowsAllEntries() {
        assertEquals(listOf("a", "b"), DrawPool.eligible(listOf("a", "b"), setOf("a"), false))
    }
    @Test fun roundDrawsEveryEntryOnceAndResetRestoresPool() {
        val ids = listOf("1", "2", "3", "4")
        val drawn = mutableSetOf<String>()
        repeat(ids.size) {
            assertTrue(drawn.add(DrawPool.pick(DrawPool.eligible(ids, drawn, true), Random(it.toLong()))))
        }
        assertTrue(DrawPool.eligible(ids, drawn, true).isEmpty())
        drawn.clear()
        assertEquals(ids, DrawPool.eligible(ids, drawn, true))
    }
    @Test(expected = IllegalStateException::class) fun emptyPoolCannotDraw() {
        DrawPool.pick(emptyList(), Random())
    }
    @Test fun singleEntryIsAlwaysReturned() {
        assertEquals("only", DrawPool.pick(listOf("only"), Random()))
    }
    @Test fun distinctIdsKeepSeparateTickets() {
        val ids = listOf("same-name-1", "same-name-2")
        assertEquals(ids, DrawPool.eligible(ids, emptySet(), true))
        assertEquals(listOf("same-name-2"), DrawPool.eligible(ids, setOf("same-name-1"), true))
    }
    @Test fun selectionUsesEntireEligibleRange() {
        val ids = listOf("a", "b", "c")
        ids.indices.forEach { index ->
            val random = object : Random() {
                override fun nextInt(bound: Int): Int { assertEquals(ids.size, bound); return index }
            }
            assertEquals(ids[index], DrawPool.pick(ids, random))
        }
    }
}
