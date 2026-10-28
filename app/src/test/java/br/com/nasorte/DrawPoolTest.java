package br.com.nasorte;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class DrawPoolTest {
    @Test public void noRepeatExcludesOnlyDrawnIds() {
        assertEquals(List.of("b", "c"), DrawPool.eligible(List.of("a", "b", "c"), Set.of("a", "deleted"), true));
    }
    @Test public void repeatAllowsAllEntries() {
        assertEquals(List.of("a", "b"), DrawPool.eligible(List.of("a", "b"), Set.of("a"), false));
    }
    @Test public void aRoundDrawsEachEntryOnceAndResetRestoresPool() {
        List<String> ids = List.of("1", "2", "3", "4");
        Set<String> drawn = new HashSet<>();
        for (int i = 0; i < ids.size(); i++) {
            String picked = DrawPool.pick(DrawPool.eligible(ids, drawn, true), new Random(i));
            assertTrue(drawn.add(picked));
        }
        assertTrue(DrawPool.eligible(ids, drawn, true).isEmpty());
        drawn.clear();
        assertEquals(ids, DrawPool.eligible(ids, drawn, true));
    }
    @Test(expected = IllegalStateException.class) public void emptyPoolCannotDraw() {
        DrawPool.pick(List.of(), new Random());
    }
    @Test public void singleEntryIsAlwaysReturned() {
        assertEquals("only", DrawPool.pick(List.of("only"), new Random()));
    }
}
