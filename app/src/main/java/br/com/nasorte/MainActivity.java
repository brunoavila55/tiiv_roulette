package br.com.nasorte;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.InputFilter;
import android.view.*;
import android.widget.*;
import android.util.LruCache;
import java.io.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.*;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int BG = 0xfff8f6f2, INK = 0xff262333, MUTED = 0xff6d687a,
        PURPLE = 0xff6243c5, LILAC = 0xffeee8fb, LINE = 0xffded9e5, WHITE = 0xffffffff;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SecureRandom random = new SecureRandom();
    private final ExecutorService images = Executors.newSingleThreadExecutor();
    private final LruCache<String, Bitmap> cache = new LruCache<>(16 * 1024 * 1024) {
        @Override protected int sizeOf(String key, Bitmap bitmap) { return bitmap.getByteCount(); }
    };
    private ItemStore store;
    private LinearLayout content;
    private Button drawButton;
    private boolean drawing, healthy = true, importing;
    private Dialog editor, result;
    private EditText nameInput;
    private Avatar editorAvatar;
    private Button photoButton, saveButton;
    private String draftId = "", draftName = "", draftPhoto = "";
    private boolean editing;
    private int editorGeneration;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new ItemStore(this);
        try { store.load(); } catch (Exception e) { healthy = false; }
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        render();
        if (!healthy) new AlertDialog.Builder(this).setTitle("Não foi possível abrir os itens")
            .setMessage("Os dados foram preservados. Feche e abra o app para tentar novamente.")
            .setPositiveButton("Entendi", null).show();
        if (state != null && state.getBoolean("editing")) {
            draftId = state.getString("draftId", ""); draftName = state.getString("draftName", "");
            draftPhoto = state.getString("draftPhoto", ""); showEditor();
        } else if (state != null && state.getBoolean("result")) {
            ItemStore.Item last = store.find(store.lastId); if (last != null) showResult(last);
        }
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private LinearLayout column() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout row() { LinearLayout v = new LinearLayout(this); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private void gap(LinearLayout parent, int height) { View v = new View(this); parent.addView(v, new LinearLayout.LayoutParams(1, dp(height))); }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this); v.setText(value); v.setTextSize(size); v.setTextColor(color);
        v.setFontFeatureSettings("kern"); v.setLineSpacing(dp(2), 1);
        if (bold) v.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        return v;
    }
    private GradientDrawable shape(int color, int radius, boolean border) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius));
        if (border) d.setStroke(dp(1), LINE); return d;
    }
    private Button button(String label, boolean primary, Runnable click) {
        Button b = new Button(this); b.setText(label); b.setTextSize(16); b.setAllCaps(false);
        b.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        b.setTextColor(primary ? WHITE : PURPLE); b.setMinHeight(dp(52)); b.setMinimumHeight(dp(52));
        b.setPadding(dp(16), dp(8), dp(16), dp(8));
        b.setBackground(new RippleDrawable(android.content.res.ColorStateList.valueOf(0x226243c5),
            shape(primary ? PURPLE : LILAC, 16, false), null));
        b.setOnClickListener(v -> click.run()); return b;
    }
    private void fullButton(LinearLayout parent, Button b) {
        parent.addView(b, new LinearLayout.LayoutParams(-1, -2));
    }
    private void insetWindow(Dialog dialog) {
        Window window = dialog.getWindow();
        if (window != null) { window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(Math.min(getResources().getDisplayMetrics().widthPixels - dp(24), dp(480)), -2);
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE); }
    }

    private void render() {
        LinearLayout root = column(); root.setBackgroundColor(BG);
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            }
            else v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        content = column(); content.setPadding(dp(24), dp(24), dp(24), dp(28));
        scroll.addView(content); root.addView(scroll, new LinearLayout.LayoutParams(-1, -1)); setContentView(root);
        if (Build.VERSION.SDK_INT >= 30 && getWindow().getInsetsController() != null) {
            int appearance = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
            getWindow().getInsetsController().setSystemBarsAppearance(appearance, appearance);
        }

        LinearLayout brand = row();
        ImageView logo = new ImageView(this); logo.setImageResource(br.com.nasorte.R.drawable.ic_launcher);
        logo.setClipToOutline(true); logo.setBackground(shape(PURPLE, 14, false));
        brand.addView(logo, new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView title = text(getString(R.string.app_name), 23, INK, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1); titleParams.leftMargin = dp(12); brand.addView(title, titleParams);
        TextView tag = text("OFFLINE", 11, PURPLE, true); tag.setPadding(dp(10), dp(7), dp(10), dp(7)); tag.setBackground(shape(LILAC, 10, false)); brand.addView(tag);
        content.addView(brand); gap(content, 32);
        content.addView(text("Deixe a sorte\nescolher.", 34, INK, true)); gap(content, 8);
        content.addView(text("Seus nomes, suas fotos. Um toque e pronto.", 16, MUTED, false)); gap(content, 24);

        LinearLayout drawCard = column(); drawCard.setPadding(dp(20), dp(24), dp(20), dp(20)); drawCard.setBackground(shape(LILAC, 24, false));
        TextView star = text("✦", 52, PURPLE, false); star.setGravity(Gravity.CENTER); star.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); drawCard.addView(star);
        int available = store.eligible().size();
        TextView ready = text(store.items.isEmpty() ? "Tudo começa com um item" : available == 0 ? "Rodada completa!" : "Quem será a próxima escolha?", 20, INK, true);
        ready.setGravity(Gravity.CENTER); drawCard.addView(ready); gap(drawCard, 8);
        TextView hint = text(store.items.isEmpty() ? "Adicione um nome e uma foto para começar." : available == 0 ? "Reinicie a rodada para sortear todos de novo." : available + (available == 1 ? " item disponível para sortear" : " itens disponíveis para sortear"), 14, MUTED, false);
        hint.setGravity(Gravity.CENTER); drawCard.addView(hint); gap(drawCard, 20);
        drawButton = button(available == 0 && !store.items.isEmpty() ? "Reiniciar rodada" : "Sortear agora", true,
            () -> { if (store.eligible().isEmpty() && !store.items.isEmpty()) resetRound(); else draw(); });
        drawButton.setEnabled(healthy && !store.items.isEmpty()); drawButton.setAlpha(drawButton.isEnabled() ? 1 : .45f); fullButton(drawCard, drawButton);
        content.addView(drawCard); gap(content, 18);

        LinearLayout option = row(); LinearLayout optionText = column();
        optionText.addView(text("Sem repetir", 16, INK, true)); optionText.addView(text("Cada item sai uma vez por rodada", 13, MUTED, false));
        option.addView(optionText, new LinearLayout.LayoutParams(0, -2, 1));
        Switch repeat = new Switch(this); repeat.setMinWidth(dp(56)); repeat.setMinHeight(dp(48)); repeat.setContentDescription("Sortear sem repetir"); repeat.setChecked(store.noRepeat); repeat.setEnabled(healthy);
        repeat.setOnCheckedChangeListener((v, checked) -> { if (change(() -> store.noRepeat = checked)) render(); }); option.addView(repeat); content.addView(option);

        if (!store.drawn.isEmpty()) { gap(content, 8); fullButton(content, button("Reiniciar rodada", false, this::resetRound)); }
        gap(content, 28);
        LinearLayout heading = row(); heading.addView(text("Meus itens", 22, INK, true), new LinearLayout.LayoutParams(0, -2, 1));
        heading.addView(text(String.format(Locale.getDefault(), "%02d", store.items.size()), 16, MUTED, true)); content.addView(heading); gap(content, 14);
        Button add = button("+  Adicionar item", false, () -> edit(null)); add.setEnabled(healthy); fullButton(content, add); gap(content, 14);
        if (store.items.isEmpty()) {
            LinearLayout empty = column(); empty.setPadding(dp(24), dp(28), dp(24), dp(28)); empty.setBackground(shape(WHITE, 20, true));
            TextView a = text("Sua lista está esperando", 18, INK, true); a.setGravity(Gravity.CENTER); empty.addView(a); gap(empty, 8);
            TextView b = text("Pessoas, pratos, filmes…\nO que vamos sortear hoje?", 15, MUTED, false); b.setGravity(Gravity.CENTER); empty.addView(b); content.addView(empty);
        }
        for (ItemStore.Item item : store.items) {
            LinearLayout card = row(); card.setPadding(dp(12), dp(12), dp(8), dp(12)); card.setBackground(shape(WHITE, 18, true));
            card.addView(new Avatar(item.name, item.photo, 16), new LinearLayout.LayoutParams(dp(60), dp(60)));
            LinearLayout info = column(); LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0, -2, 1); infoParams.leftMargin = dp(12);
            info.addView(text(item.name, 17, INK, true));
            info.addView(text(store.drawn.contains(item.id) ? "Já sorteado nesta rodada" : "Pronto para o sorteio", 12, MUTED, false)); card.addView(info, infoParams);
            Button more = button("⋮", false, () -> itemMenu(item)); more.setTextSize(24); more.setContentDescription("Opções de " + item.name); more.setPadding(0, 0, 0, 0);
            card.addView(more, new LinearLayout.LayoutParams(dp(48), dp(52))); content.addView(card); gap(content, 8);
        }
        gap(content, 16);
        TextView footer = text("Feito para decidir com leveza.\nSeus itens ficam salvos neste aparelho.", 12, MUTED, false); footer.setGravity(Gravity.CENTER); content.addView(footer);
    }

    private boolean change(Runnable mutation) {
        if (!healthy) return false;
        JSONObject previous = null;
        try { previous = store.snapshot(); mutation.run(); store.save(); return true; }
        catch (Exception e) {
            try { if (previous != null) store.restore(previous); } catch (Exception ignored) { healthy = false; }
            toast("Não foi possível salvar. Verifique o espaço do aparelho e tente novamente."); return false;
        }
    }
    private void resetRound() {
        new AlertDialog.Builder(this).setTitle("Reiniciar a rodada?").setMessage("Todos os itens poderão ser sorteados novamente.")
            .setNegativeButton("Cancelar", null).setPositiveButton("Reiniciar", (d, w) -> {
                if (change(() -> { store.drawn.clear(); store.lastId = ""; })) render();
            }).show();
    }
    private void itemMenu(ItemStore.Item item) {
        new AlertDialog.Builder(this).setTitle(item.name).setItems(new String[]{"Editar nome e foto", "Excluir item"}, (d, which) -> {
            if (which == 0) edit(item);
            else new AlertDialog.Builder(this).setTitle("Excluir “" + item.name + "”?").setMessage("O item será removido da sua lista.")
                .setNegativeButton("Cancelar", null).setPositiveButton("Excluir", (a, b) -> {
                    if (change(() -> { store.items.remove(item); store.drawn.remove(item.id); if (store.lastId.equals(item.id)) store.lastId = ""; })) { discardPhoto(item.photo); render(); }
                }).show();
        }).show();
    }
    private void edit(ItemStore.Item item) {
        draftId = item == null ? "" : item.id; draftName = item == null ? "" : item.name; draftPhoto = item == null ? "" : item.photo; showEditor();
    }
    private void showEditor() {
        editorGeneration++;
        importing = false;
        editing = true; editor = new Dialog(this);
        LinearLayout body = column(); body.setPadding(dp(24), dp(24), dp(24), dp(24)); body.setBackground(shape(BG, 24, false));
        body.addView(text(draftId.isEmpty() ? "Um novo item" : "Do seu jeito", 26, INK, true)); gap(body, 6);
        body.addView(text("Escolha a foto e dê um nome.", 15, MUTED, false)); gap(body, 20);
        editorAvatar = new Avatar(draftName, draftPhoto, 24);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(140), dp(140)); avatarParams.gravity = Gravity.CENTER_HORIZONTAL;
        body.addView(editorAvatar, avatarParams); gap(body, 12);
        photoButton = button("Escolher foto", false, this::pickPhoto); fullButton(body, photoButton);
        fullButton(body, button("Remover foto", false, () -> { String old = draftPhoto; draftPhoto = ""; discardPhoto(old); editorAvatar.set(draftName, ""); })); gap(body, 16);
        body.addView(text("Nome do item", 14, INK, true)); gap(body, 6);
        nameInput = new EditText(this); nameInput.setSingleLine(true); nameInput.setTextSize(17); nameInput.setTextColor(INK); nameInput.setHintTextColor(MUTED); nameInput.setHint("Ex.: Pizza de sexta");
        nameInput.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)}); nameInput.setPadding(dp(14), dp(12), dp(14), dp(12)); nameInput.setBackground(shape(WHITE, 12, true)); nameInput.setText(draftName);
        nameInput.setContentDescription("Nome do item"); body.addView(nameInput, new LinearLayout.LayoutParams(-1, -2)); gap(body, 20);
        saveButton = button("Salvar item", true, this::saveDraft); fullButton(body, saveButton); gap(body, 8);
        fullButton(body, button("Cancelar", false, () -> editor.dismiss()));
        ScrollView scroll = new ScrollView(this); scroll.addView(body); editor.setContentView(scroll);
        editor.setOnDismissListener(d -> { editing = false; discardPhoto(draftPhoto); }); editor.show(); insetWindow(editor);
    }
    private void saveDraft() {
        if (importing) return;
        String name = nameInput.getText().toString().trim();
        if (name.isEmpty()) { nameInput.setError("Escreva um nome para o item"); nameInput.requestFocus(); return; }
        ItemStore.Item old = store.find(draftId);
        String id = old == null ? UUID.randomUUID().toString() : old.id;
        ItemStore.Item next = new ItemStore.Item(id, name, draftPhoto);
        if (change(() -> { if (old == null) store.items.add(next); else store.items.set(store.items.indexOf(old), next); })) {
            editor.dismiss(); if (old != null) discardPhoto(old.photo); render(); toast("Item salvo");
        }
    }
    private void pickPhoto() {
        draftName = nameInput.getText().toString();
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);
        try { startActivityForResult(intent, 10); }
        catch (ActivityNotFoundException e) { toast("Nenhum seletor de fotos disponível neste aparelho."); }
    }
    @Override public void onActivityResult(int request, int status, Intent data) {
        super.onActivityResult(request, status, data);
        if (request != 10 || status != RESULT_OK || data == null || data.getData() == null || !editing) return;
        Uri uri = data.getData(); int generation = editorGeneration; importing = true; photoButton.setEnabled(false); saveButton.setEnabled(false); photoButton.setText(R.string.preparing_photo);
        images.execute(() -> {
            File destination = new File(getFilesDir(), UUID.randomUUID() + ".jpg");
            try {
                Bitmap bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri), (decoder, info, source) -> {
                    int w = info.getSize().getWidth(), h = info.getSize().getHeight();
                    float scale = Math.min(1f, 720f / Math.max(w, h));
                    decoder.setTargetSize(Math.max(1, (int)(w * scale)), Math.max(1, (int)(h * scale)));
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                });
                try (FileOutputStream stream = new FileOutputStream(destination)) { if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 88, stream)) throw new IOException(); }
                bitmap.recycle();
                handler.post(() -> {
                    if (isDestroyed() || !editing || generation != editorGeneration) { destination.delete(); return; }
                    String old = draftPhoto; draftPhoto = destination.getName(); discardPhoto(old); editorAvatar.set(nameInput.getText().toString(), draftPhoto); finishImport();
                });
            } catch (Exception | OutOfMemoryError e) {
                destination.delete(); handler.post(() -> { if (!isDestroyed() && editing && generation == editorGeneration) { finishImport(); toast("Não foi possível abrir essa imagem. Tente outra foto."); } });
            }
        });
    }
    private void finishImport() { importing = false; photoButton.setEnabled(true); saveButton.setEnabled(true); photoButton.setText(R.string.change_photo); }
    private void discardPhoto(String photo) {
        if (photo.isEmpty()) return;
        for (ItemStore.Item item : store.items) if (item.photo.equals(photo)) return;
        cache.remove(photo + ":thumb"); cache.remove(photo + ":full"); new File(getFilesDir(), photo).delete();
    }

    private void draw() {
        if (drawing || !healthy || store.eligible().isEmpty()) return;
        String id = DrawPool.pick(store.eligible(), random); ItemStore.Item winner = store.find(id);
        // Commit the result before animation, so leaving the app cannot redraw an excluded entry.
        if (!change(() -> { store.drawn.add(id); store.lastId = id; })) return;
        drawing = true; drawButton.setEnabled(false); drawButton.setText(R.string.drawing);
        enableChildren(content, false);
        float animations = Settings.Global.getFloat(getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1);
        handler.postDelayed(() -> { drawing = false; render(); showResult(winner); }, animations == 0 ? 0 : 850);
    }
    private void enableChildren(View view, boolean enabled) {
        view.setEnabled(enabled);
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup)view;
            for (int i = 0; i < group.getChildCount(); i++) enableChildren(group.getChildAt(i), enabled);
        }
    }
    private void showResult(ItemStore.Item winner) {
        if (isFinishing() || isDestroyed()) return;
        result = new Dialog(this);
        LinearLayout body = column(); body.setPadding(dp(24), dp(28), dp(24), dp(24)); body.setBackground(shape(BG, 28, false));
        TextView eyebrow = text("A SORTE ESCOLHEU", 13, PURPLE, true); eyebrow.setGravity(Gravity.CENTER); body.addView(eyebrow); gap(body, 20);
        Avatar photo = new Avatar(winner.name, winner.photo, 24); photo.setContentDescription("Foto de " + winner.name); photo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(220), dp(220)); p.gravity = Gravity.CENTER_HORIZONTAL; body.addView(photo, p); gap(body, 20);
        TextView name = text(winner.name, 30, INK, true); name.setGravity(Gravity.CENTER); name.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE); body.addView(name); gap(body, 8);
        TextView message = text("Temos uma escolha!", 16, MUTED, false); message.setGravity(Gravity.CENTER); body.addView(message); gap(body, 24);
        boolean exhausted = store.eligible().isEmpty();
        fullButton(body, button(exhausted ? "Voltar à lista" : "Sortear de novo", true, () -> { result.dismiss(); if (!exhausted) draw(); })); gap(body, 8);
        if (!exhausted) fullButton(body, button("Voltar à lista", false, () -> result.dismiss()));
        ScrollView scroll = new ScrollView(this); scroll.addView(body); result.setContentView(scroll); result.show(); insetWindow(result);
        name.announceForAccessibility("Item sorteado: " + winner.name);
    }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out); out.putBoolean("editing", editing);
        out.putString("draftId", draftId); out.putString("draftPhoto", draftPhoto);
        out.putString("draftName", nameInput != null && editing ? nameInput.getText().toString() : draftName);
        out.putBoolean("result", drawing || (result != null && result.isShowing()));
    }
    @Override protected void onDestroy() { handler.removeCallbacksAndMessages(null); images.shutdown(); super.onDestroy(); }

    private final class Avatar extends View {
        private String initial; private Bitmap bitmap; private BitmapShader shader; private final int radius;
        private final RectF bounds = new RectF();
        private final Matrix matrix = new Matrix();
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        Avatar(String name, String photo, int radius) { super(MainActivity.this); this.radius = radius; setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO); set(name, photo); }
        void set(String name, String photo) {
            initial = name.trim().isEmpty() ? "+" : new String(Character.toChars(name.trim().codePointAt(0))).toUpperCase(Locale.getDefault());
            bitmap = null;
            if (!photo.isEmpty()) {
                String key = photo + (radius == 16 ? ":thumb" : ":full");
                bitmap = cache.get(key);
                if (bitmap == null) {
                    BitmapFactory.Options options = new BitmapFactory.Options(); options.inSampleSize = radius == 16 ? 4 : 1;
                    bitmap = BitmapFactory.decodeFile(new File(getFilesDir(), photo).getPath(), options);
                    if (bitmap != null) cache.put(key, bitmap);
                }
            }
            shader = bitmap == null ? null : new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            invalidate();
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas); bounds.set(0, 0, getWidth(), getHeight());
            if (bitmap != null) {
                float scale = Math.max((float)getWidth()/bitmap.getWidth(), (float)getHeight()/bitmap.getHeight());
                matrix.setScale(scale, scale); matrix.postTranslate((getWidth()-bitmap.getWidth()*scale)/2, (getHeight()-bitmap.getHeight()*scale)/2);
                shader.setLocalMatrix(matrix); paint.setShader(shader); canvas.drawRoundRect(bounds, dp(radius), dp(radius), paint); paint.setShader(null);
            } else {
                paint.setColor(LILAC); canvas.drawRoundRect(bounds, dp(radius), dp(radius), paint);
                paint.setColor(PURPLE); paint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); paint.setTextSize(getWidth() * .35f); paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(initial, getWidth()/2f, getHeight()/2f-(paint.ascent()+paint.descent())/2, paint);
            }
        }
    }
}
