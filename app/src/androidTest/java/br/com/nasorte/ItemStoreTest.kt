package br.com.nasorte

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.json.JSONObject
import java.io.File
import java.util.UUID

/** Isolated on-device storage tests; never reads or writes the installed user's list. */
class ItemStoreTest {
    private lateinit var directory: File
    private lateinit var isolatedContext: Context
    @Before fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        directory = File(context.cacheDir, "store-test-${UUID.randomUUID()}").apply { mkdirs() }
        isolatedContext = object : ContextWrapper(context) { override fun getFilesDir() = directory }
    }
    @After fun tearDown() { directory.deleteRecursively() }

    @Test fun testReadsJavaSchemaAndPreservesPhotosAndRound() {
        File(directory, "items.json").writeText("""{"items":[{"id":"a","name":"Pizza","photo":"existing.jpg"},{"id":"b","name":"Pizza","photo":""}],"drawn":["a"],"noRepeat":true,"lastId":"a"}""")
        val store = ItemStore(isolatedContext)
        store.load()
        assertEquals(2, store.items.size)
        assertEquals("existing.jpg", store.find("a")?.photo)
        assertEquals(listOf("b"), store.eligible())
        assertEquals("a", store.lastId)
        store.save()
        val reloaded = ItemStore(isolatedContext).apply { load() }
        assertEquals(store.items, reloaded.items)
        assertEquals(store.drawn, reloaded.drawn)
        assertTrue(reloaded.noRepeat)
        assertEquals("a", reloaded.lastId)
    }
    @Test fun testInvalidImportDoesNotPartiallyReplaceItems() {
        val store = ItemStore(isolatedContext)
        store.items.add(ItemStore.Item("a", "Original", ""))
        try {
            store.restore(JSONObject("""{"items":[{"id":"b","name":"Bad"}],"drawn":[]}"""))
            fail("Malformed data must fail")
        } catch (_: org.json.JSONException) { }
        assertEquals("Original", store.items.single().name)
    }
    @Test fun testSavedChoiceSurvivesReloadBeforeReveal() {
        val store = ItemStore(isolatedContext)
        store.items.add(ItemStore.Item("a", "A", ""))
        store.drawn.add("a"); store.lastId = "a"; store.noRepeat = true; store.save()
        val reloaded = ItemStore(isolatedContext).apply { load() }
        assertTrue(reloaded.eligible().isEmpty())
        assertEquals("a", reloaded.lastId)
    }
    @Test fun testMultipleListsAndSelectionSurviveActualDiskReload() {
        val store = ItemStore(isolatedContext)
        val days = store.createList("Dia da semana")
        store.items.add(ItemStore.Item("segunda", "Segunda", "dia.jpg"))
        store.noRepeat = true; store.drawn.add("segunda"); store.lastId = "segunda"
        store.createList("Roupa")
        store.items.add(ItemStore.Item("camiseta", "Camiseta", "roupa.jpg"))
        store.createList("Calçado")
        store.items.add(ItemStore.Item("tenis", "Tênis", "tenis.jpg"))
        store.selectList(days.id); store.save()
        val reloaded = ItemStore(isolatedContext).apply { load() }
        assertEquals(store.lists, reloaded.lists)
        assertEquals(days.id, reloaded.selectedListId)
        assertTrue(reloaded.eligible().isEmpty())
        assertTrue(reloaded.usesPhoto("roupa.jpg"))
    }

}
