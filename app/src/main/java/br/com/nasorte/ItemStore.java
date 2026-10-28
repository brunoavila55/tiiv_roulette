package br.com.nasorte;

import android.content.Context;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class ItemStore {
    static final class Item {
        final String id, name, photo;
        Item(String id, String name, String photo) { this.id = id; this.name = name; this.photo = photo; }
    }
    final List<Item> items = new ArrayList<>();
    final Set<String> drawn = new HashSet<>();
    boolean noRepeat = false;
    String lastId = "";
    private final AtomicFile file;
    ItemStore(Context context) { file = new AtomicFile(new File(context.getFilesDir(), "items.json")); }
    JSONObject snapshot() throws JSONException {
        JSONArray entries = new JSONArray();
        for (Item item : items) entries.put(new JSONObject().put("id", item.id).put("name", item.name).put("photo", item.photo));
        return new JSONObject().put("items", entries).put("drawn", new JSONArray(drawn)).put("noRepeat", noRepeat).put("lastId", lastId);
    }
    void restore(JSONObject data) throws JSONException {
        List<Item> loaded = new ArrayList<>();
        JSONArray entries = data.getJSONArray("items");
        for (int i = 0; i < entries.length(); i++) {
            JSONObject entry = entries.getJSONObject(i);
            loaded.add(new Item(entry.getString("id"), entry.getString("name"), entry.getString("photo")));
        }
        Set<String> loadedDrawn = new HashSet<>();
        JSONArray wins = data.getJSONArray("drawn");
        for (int i = 0; i < wins.length(); i++) loadedDrawn.add(wins.getString(i));
        items.clear(); items.addAll(loaded); drawn.clear(); drawn.addAll(loadedDrawn);
        noRepeat = data.optBoolean("noRepeat"); lastId = data.optString("lastId");
    }
    void load() throws IOException, JSONException {
        try (InputStream stream = file.openRead()) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192]; int count;
            while ((count = stream.read(buffer)) != -1) bytes.write(buffer, 0, count);
            restore(new JSONObject(bytes.toString(StandardCharsets.UTF_8.name())));
        } catch (FileNotFoundException e) { if (file.getBaseFile().exists()) throw e; }
    }
    void save() throws IOException, JSONException {
        byte[] data = snapshot().toString().getBytes(StandardCharsets.UTF_8);
        FileOutputStream output = file.startWrite();
        try { output.write(data); file.finishWrite(output); }
        catch (IOException e) { file.failWrite(output); throw e; }
    }
    Item find(String id) { for (Item item : items) if (item.id.equals(id)) return item; return null; }
    List<String> eligible() {
        List<String> ids = new ArrayList<>();
        for (Item item : items) ids.add(item.id);
        return DrawPool.eligible(ids, drawn, noRepeat);
    }
}
