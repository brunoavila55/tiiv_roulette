package br.com.nasorte

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException

/** Keeps the original JSON schema and photo filenames so updates preserve existing lists. */
internal class ItemStore(context: Context) {
    data class Item(val id: String, val name: String, val photo: String)
    val items = mutableListOf<Item>()
    val drawn = mutableSetOf<String>()
    var noRepeat = false
    var lastId = ""
    private val file = AtomicFile(File(context.filesDir, "items.json"))

    fun snapshot(): JSONObject = JSONObject().apply {
        put("items", JSONArray().apply {
            items.forEach { item -> put(JSONObject().apply {
                put("id", item.id); put("name", item.name); put("photo", item.photo)
            }) }
        })
        put("drawn", JSONArray(drawn.toList()))
        put("noRepeat", noRepeat)
        put("lastId", lastId)
    }

    fun restore(data: JSONObject) {
        val entries = data.getJSONArray("items")
        val loaded = (0 until entries.length()).map { index ->
            entries.getJSONObject(index).let { Item(it.getString("id"), it.getString("name"), it.getString("photo")) }
        }
        val wins = data.getJSONArray("drawn")
        val loadedDrawn = (0 until wins.length()).map { wins.getString(it) }
        items.clear(); items.addAll(loaded)
        drawn.clear(); drawn.addAll(loadedDrawn)
        noRepeat = data.optBoolean("noRepeat")
        lastId = data.optString("lastId")
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
