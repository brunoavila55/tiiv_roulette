package br.com.nasorte

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.util.UUID

/** Independent draw lists, with migration from the original single-list JSON. */
internal class ItemStore(private val directory: File) {
    constructor(context: Context) : this(context.filesDir)

    data class Item(val id: String, val name: String, val photo: String)
    data class DrawList(
        val id: String,
        var name: String,
        val items: MutableList<Item> = mutableListOf(),
        val drawn: MutableSet<String> = mutableSetOf(),
        var noRepeat: Boolean = false,
        var lastId: String = ""
    )

    private val savedLists = mutableListOf(DrawList("default", "Meus itens"))
    val lists: List<DrawList> get() = savedLists
    var selectedListId = "default"
        private set
    val current: DrawList get() = savedLists.first { it.id == selectedListId }
    val items: MutableList<Item> get() = current.items
    val drawn: MutableSet<String> get() = current.drawn
    var noRepeat: Boolean
        get() = current.noRepeat
        set(value) { current.noRepeat = value }
    var lastId: String
        get() = current.lastId
        set(value) { current.lastId = value }
    private val file by lazy { AtomicFile(File(directory, "items.json")) }

    fun selectList(id: String) {
        require(savedLists.any { it.id == id }) { "Sorteio não encontrado" }
        selectedListId = id
    }
    fun createList(name: String): DrawList {
        val cleanName = validateName(name)
        return DrawList(UUID.randomUUID().toString(), cleanName).also {
            savedLists.add(it); selectedListId = it.id
        }
    }
    fun renameList(id: String, name: String) {
        val list = requireNotNull(savedLists.find { it.id == id }) { "Sorteio não encontrado" }
        list.name = validateName(name, id)
    }
    fun deleteList(id: String): DrawList {
        require(savedLists.size > 1) { "Mantenha pelo menos um sorteio" }
        val index = savedLists.indexOfFirst { it.id == id }
        require(index >= 0) { "Sorteio não encontrado" }
        val removed = savedLists.removeAt(index)
        if (selectedListId == id) selectedListId = savedLists[minOf(index, savedLists.lastIndex)].id
        return removed
    }
    private fun validateName(name: String, exceptId: String? = null): String {
        val clean = name.trim()
        require(clean.isNotEmpty()) { "Escreva um nome para o sorteio" }
        require(clean.length <= 80) { "Use até 80 caracteres" }
        require(savedLists.none { it.id != exceptId && it.name.equals(clean, ignoreCase = true) }) {
            "Já existe um sorteio com esse nome"
        }
        return clean
    }
    fun usesPhoto(photo: String): Boolean = savedLists.any { list -> list.items.any { it.photo == photo } }

    fun snapshot(): JSONObject = JSONObject().apply {
        put("version", 2)
        put("selectedListId", selectedListId)
        put("lists", JSONArray().apply {
            savedLists.forEach { list -> put(JSONObject().apply {
                put("id", list.id); put("name", list.name)
                put("items", JSONArray().apply {
                    list.items.forEach { item -> put(JSONObject().apply {
                        put("id", item.id); put("name", item.name); put("photo", item.photo)
                    }) }
                })
                put("drawn", JSONArray(list.drawn.toList()))
                put("noRepeat", list.noRepeat); put("lastId", list.lastId)
            }) }
        })
    }

    fun restore(data: JSONObject) {
        val loaded = if (data.has("lists")) {
            val entries = data.getJSONArray("lists")
            (0 until entries.length()).map { index ->
                val entry = entries.getJSONObject(index)
                readList(entry, entry.getString("id"), entry.getString("name"))
            }
        } else listOf(readList(data, "default", "Meus itens"))
        require(loaded.isNotEmpty()) { "Nenhum sorteio salvo" }
        require(loaded.all { it.id.isNotBlank() && it.name.isNotBlank() }) { "Sorteio inválido" }
        require(loaded.map { it.id }.distinct().size == loaded.size) { "Identificadores duplicados" }
        val selected = data.optString("selectedListId").takeIf { id -> loaded.any { it.id == id } } ?: loaded.first().id
        // Parse and validate every list before replacing any live state.
        savedLists.clear(); savedLists.addAll(loaded); selectedListId = selected
    }
    private fun readList(data: JSONObject, id: String, name: String): DrawList {
        val entries = data.getJSONArray("items")
        val items = (0 until entries.length()).map { index ->
            entries.getJSONObject(index).let { Item(it.getString("id"), it.getString("name"), it.getString("photo")) }
        }
        val wins = data.getJSONArray("drawn")
        val drawn = (0 until wins.length()).map { wins.getString(it) }
        return DrawList(id, name, items.toMutableList(), drawn.toMutableSet(), data.optBoolean("noRepeat"), data.optString("lastId"))
    }

    fun load() {
        try {
            file.openRead().bufferedReader(Charsets.UTF_8).use { restore(JSONObject(it.readText())) }
        } catch (e: FileNotFoundException) {
            if (file.baseFile.exists()) throw e
        }
    }
    fun save() {
        val bytes = snapshot().toString().toByteArray(Charsets.UTF_8)
        val output = file.startWrite()
        try { output.write(bytes); file.finishWrite(output) }
        catch (e: Exception) { file.failWrite(output); throw e }
    }
    fun find(id: String): Item? = items.find { it.id == id }
    fun eligible(): List<String> = DrawPool.eligible(items.map { it.id }, drawn, noRepeat)
}
