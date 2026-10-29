package br.com.nasorte

import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Exercises the actual JSON codec and list state without Android storage stubs. */
class ItemStoreStateTest {
    private fun store() = ItemStore(File("unused-by-state-tests"))
    private fun add(store: ItemStore, id: String, photo: String = "") {
        store.items.add(ItemStore.Item(id, id, photo))
    }
    @Test fun migratesExistingItemsPhotosAndRoundIntoOneNamedList() {
        val state = store()
        state.restore(JSONObject("""{"items":[{"id":"a","name":"Pizza","photo":"existing.jpg"}],"drawn":["a"],"noRepeat":true,"lastId":"a"}"""))
        assertEquals("Meus itens", state.current.name)
        assertEquals(1, state.lists.size)
        assertEquals("existing.jpg", state.find("a")?.photo)
        assertTrue(state.eligible().isEmpty())
        assertEquals("a", state.lastId)
        val restored = store().apply { restore(state.snapshot()) }
        assertEquals(state.lists, restored.lists)
    }
    @Test fun switchingListsKeepsItemsRepeatSettingsAndResultsSeparate() {
        val state = store()
        val days = state.createList("Dia da semana")
        add(state, "segunda"); add(state, "terca")
        state.noRepeat = true; state.drawn.add("segunda"); state.lastId = "segunda"
        val clothes = state.createList("Roupa")
        add(state, "camiseta")
        assertFalse(state.noRepeat)
        assertTrue(state.drawn.isEmpty())
        assertEquals("", state.lastId)
        assertEquals(listOf("camiseta"), state.eligible())
        state.selectList(days.id)
        assertEquals(listOf("terca"), state.eligible())
        assertEquals("segunda", state.lastId)
        state.selectList(clothes.id)
        assertNull(state.find("segunda"))
    }
    @Test fun serializedStateRestoresAllListsAndActiveSelection() {
        val state = store()
        val clothes = state.createList("Roupa")
        add(state, "camiseta", "roupa.jpg"); state.drawn.add("camiseta"); state.lastId = "camiseta"
        state.createList("Calçado"); add(state, "tenis", "tenis.jpg"); state.noRepeat = true
        state.selectList(clothes.id)
        val restored = store().apply { restore(JSONObject(state.snapshot().toString())) }
        assertEquals(state.lists, restored.lists)
        assertEquals(clothes.id, restored.selectedListId)
        assertEquals("camiseta", restored.lastId)
    }
    @Test fun restartingCurrentRoundLeavesOtherRoundsUntouched() {
        val state = store()
        val first = state.current.id
        add(state, "a"); state.drawn.add("a"); state.lastId = "a"
        state.createList("Roupa"); add(state, "b"); state.drawn.add("b"); state.lastId = "b"
        state.drawn.clear(); state.lastId = ""
        state.selectList(first)
        assertEquals(setOf("a"), state.drawn)
        assertEquals("a", state.lastId)
    }
    @Test fun deletingActiveListSelectsSurvivingListAndKeepsItsItems() {
        val state = store()
        val first = state.current.id
        add(state, "a")
        val second = state.createList("Calçado")
        add(state, "b")
        state.deleteList(second.id)
        assertEquals(first, state.selectedListId)
        assertEquals(listOf("a"), state.items.map { it.id })
    }
    @Test(expected = IllegalArgumentException::class) fun cannotDeleteLastList() {
        val state = store(); state.deleteList(state.current.id)
    }
    @Test fun renamingPreservesIdentityItemsAndRound() {
        val state = store(); add(state, "a"); state.drawn.add("a")
        val id = state.current.id
        state.renameList(id, "  Dia da semana  ")
        assertEquals(id, state.selectedListId)
        assertEquals("Dia da semana", state.current.name)
        assertEquals("a", state.items.single().id)
        assertEquals(setOf("a"), state.drawn)
    }
    @Test fun invalidNamesAreRejectedWithoutCreatingLists() {
        val state = store(); state.createList("Roupa")
        listOf("", "  ", " roupa ", "a".repeat(81)).forEach { invalid ->
            try { state.createList(invalid); fail("Should reject $invalid") }
            catch (_: IllegalArgumentException) { }
        }
        assertEquals(2, state.lists.size)
    }
    @Test fun photoReferencesAreCheckedAcrossInactiveLists() {
        val state = store(); val first = state.current.id
        add(state, "a", "shared.jpg")
        val second = state.createList("Calçado"); add(state, "b", "shared.jpg")
        state.deleteList(first)
        assertTrue(state.usesPhoto("shared.jpg"))
        state.createList("Roupa"); state.deleteList(second.id)
        assertFalse(state.usesPhoto("shared.jpg"))
    }
    @Test fun malformedSecondListDoesNotPartiallyReplaceLiveState() {
        val state = store(); add(state, "a")
        val before = state.snapshot().toString()
        val invalid = JSONObject("""{"lists":[{"id":"ok","name":"Ok","items":[],"drawn":[]},{"id":"bad","name":"Bad","items":[{"id":"x"}],"drawn":[]}]}""")
        try { state.restore(invalid); fail("Malformed item must fail") }
        catch (_: JSONException) { }
        assertEquals(before, state.snapshot().toString())
    }
    @Test fun missingSelectedListFallsBackToExistingList() {
        val state = store(); state.createList("Roupa")
        val json = state.snapshot().put("selectedListId", "deleted")
        val restored = store().apply { restore(json) }
        assertEquals(restored.lists.first().id, restored.selectedListId)
    }
    @Test fun emptyOrDuplicateListsCannotReplaceLiveState() {
        val state = store(); val before = state.snapshot().toString()
        val duplicate = state.snapshot().apply {
            val lists = getJSONArray("lists"); lists.put(lists.getJSONObject(0))
        }
        listOf(JSONObject("""{"lists":[]}"""), duplicate).forEach { invalid ->
            try { state.restore(invalid); fail("Invalid list set must fail") }
            catch (_: IllegalArgumentException) { }
            assertEquals(before, state.snapshot().toString())
        }
    }
}
