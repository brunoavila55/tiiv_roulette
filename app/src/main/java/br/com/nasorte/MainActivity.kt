package br.com.nasorte

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.text.InputFilter
import android.util.LruCache
import android.view.*
import android.view.animation.DecelerateInterpolator
import android.widget.*
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.math.*

@Suppress("DEPRECATION")
class MainActivity : Activity() {
    private val handler = Handler(Looper.getMainLooper())
    private val random = SecureRandom()
    private val images = Executors.newSingleThreadExecutor()
    private val cache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private lateinit var store: ItemStore
    private lateinit var content: LinearLayout
    private lateinit var drawButton: Button
    private lateinit var deck: DeckView
    private var healthy = true
    private var drawing = false
    private var animator: ValueAnimator? = null
    private var editor: Dialog? = null
    private var result: Dialog? = null
    private var editing = false
    private var importing = false
    private var editorGeneration = 0
    private var draftId = ""
    private var draftName = ""
    private var draftPhoto = ""
    private lateinit var nameInput: EditText
    private lateinit var editorAvatar: Avatar
    private lateinit var photoButton: Button
    private lateinit var saveButton: Button

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        store = ItemStore(this)
        try { store.load() } catch (_: Exception) { healthy = false }
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        render()
        if (!healthy) AlertDialog.Builder(this).setTitle("Não foi possível abrir os itens")
            .setMessage("Os dados foram preservados. Feche e abra o app para tentar novamente.")
            .setPositiveButton("Entendi", null).show()
        if (state?.getBoolean("editing") == true) {
            draftId = state.getString("draftId", "")
            draftName = state.getString("draftName", "")
            draftPhoto = state.getString("draftPhoto", "")
            showEditor()
        } else if (state?.getBoolean("result") == true) {
            store.find(store.lastId)?.let { showResult(it) }
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row() = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
    private fun gap(parent: LinearLayout, height: Int) { parent.addView(View(this), LinearLayout.LayoutParams(1, dp(height))) }
    private fun text(value: String, size: Int, color: Int = INK, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(color)
        typeface = Typeface.create(if (bold) "sans-serif-medium" else "sans-serif", Typeface.NORMAL)
        setLineSpacing(dp(2).toFloat(), 1f)
    }
    private fun display(value: String, size: Int) = text(value, size, INK, true).apply {
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        setLineSpacing(0f, .96f)
    }
    private fun shape(color: Int, radius: Int, border: Boolean = false) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        if (border) setStroke(dp(1), LINE)
    }
    private fun button(label: String, primary: Boolean = false, action: () -> Unit) = Button(this).apply {
        text = label; textSize = 16f; isAllCaps = false
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        setTextColor(if (primary) WHITE else BLUE)
        minHeight = dp(54); minimumHeight = dp(54)
        setPadding(dp(16), dp(10), dp(16), dp(10))
        background = RippleDrawable(ColorStateList.valueOf(0x22254EDB), shape(if (primary) BLUE else PALE, 16), null)
        setOnClickListener { action() }
    }
    private fun full(parent: LinearLayout, view: View) { parent.addView(view, LinearLayout.LayoutParams(-1, -2)) }
    private fun centered(parent: LinearLayout, view: View, width: Int, height: Int) {
        parent.addView(view, LinearLayout.LayoutParams(dp(width), dp(height)).apply { gravity = Gravity.CENTER_HORIZONTAL })
    }
    private fun showDialog(body: LinearLayout): Dialog = Dialog(this).apply {
        setContentView(ScrollView(this@MainActivity).apply { addView(body) })
        show()
        window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(min(resources.displayMetrics.widthPixels - dp(24), dp(480)), -2)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }
    private fun dialogBody() = column().apply {
        setPadding(dp(24), dp(28), dp(24), dp(24)); background = shape(WHITE, 28)
    }

    private fun render() {
        val root = column().apply {
            setBackgroundColor(BG)
            setOnApplyWindowInsetsListener { view, insets ->
                if (android.os.Build.VERSION.SDK_INT >= 30) {
                    val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                    view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                } else view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
                insets
            }
        }
        content = column().apply { setPadding(dp(22), dp(20), dp(22), dp(24)) }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(content) }
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val action = column().apply { setPadding(dp(22), dp(12), dp(22), dp(14)); setBackgroundColor(WHITE) }
        val available = store.eligible().size
        drawButton = button(if (available == 0 && store.items.isNotEmpty()) "Reiniciar rodada" else "Sortear uma carta", true) {
            if (store.eligible().isEmpty() && store.items.isNotEmpty()) resetRound() else draw()
        }.apply { isEnabled = healthy && store.items.isNotEmpty(); alpha = if (isEnabled) 1f else .45f }
        full(action, drawButton); root.addView(action)
        setContentView(root)
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            val flags = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            window.insetsController?.setSystemBarsAppearance(flags, flags)
        }
        val brand = row()
        brand.addView(text("tiiv", 28, BLUE, true))
        brand.addView(text(" / roulette", 17, MUTED), LinearLayout.LayoutParams(0, -2, 1f))
        brand.addView(button("+ Item") { edit(null) }.apply { isEnabled = healthy })
        content.addView(brand); gap(content, 14)
        val listActions = row()
        listActions.addView(button("${store.current.name}  ▾") { chooseList() }.apply {
            isEnabled = healthy; contentDescription = "Trocar sorteio: ${store.current.name}"
        }, LinearLayout.LayoutParams(0, -2, 1f))
        listActions.addView(button("Novo") { nameList() }.apply {
            isEnabled = healthy; contentDescription = "Criar novo sorteio"
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }, LinearLayout.LayoutParams(dp(64), dp(54)).apply { leftMargin = dp(8) })
        listActions.addView(button("⋮") { listMenu() }.apply {
            isEnabled = healthy; textSize = 24f; setPadding(0, 0, 0, 0)
            contentDescription = "Opções do sorteio ${store.current.name}"
        }, LinearLayout.LayoutParams(dp(48), dp(54)).apply { leftMargin = dp(8) })
        content.addView(listActions); gap(content, 18)
        content.addView(display("Uma carta decide.", 34)); gap(content, 8)
        content.addView(text("Cada sorteio tem suas próprias cartas.", 15, MUTED)); gap(content, 12)
        deck = DeckView().apply { contentDescription = "Cartas do sorteio" }
        full(content, deck)
        val ready = when {
            store.items.isEmpty() -> "Adicione seu primeiro item para começar."
            available == 0 -> "Todos já saíram. Comece uma nova rodada."
            else -> "$available ${if (available == 1) "carta na mesa" else "cartas na mesa"}. Qual será a sua?"
        }
        full(content, text(ready, 15, MUTED).apply { gravity = Gravity.CENTER })
        gap(content, 26)
        val option = row()
        val labels = column().apply {
            addView(text("Sem repetir", 16, INK, true))
            addView(text("Uma chance por item em cada rodada", 12, MUTED))
        }
        option.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        option.addView(Switch(this).apply {
            minHeight = dp(48); minWidth = dp(52)
            contentDescription = "Sortear sem repetir"; isChecked = store.noRepeat; isEnabled = healthy
            thumbTintList = ColorStateList.valueOf(BLUE)
            setOnCheckedChangeListener { _, checked -> if (change { store.noRepeat = checked }) render() }
        })
        content.addView(option)
        if (store.drawn.isNotEmpty()) {
            gap(content, 8); full(content, button("Reiniciar rodada") { resetRound() }.apply { isEnabled = healthy })
        }
        gap(content, 28)
        val heading = row()
        heading.addView(display("Na mesa", 28), LinearLayout.LayoutParams(0, -2, 1f))
        heading.addView(text("${store.items.size} ${if (store.items.size == 1) "item" else "itens"}", 14, MUTED))
        content.addView(heading); gap(content, 12)
        if (store.items.isEmpty()) {
            full(content, text("Este sorteio ainda está vazio.\nAdicione as opções de ${store.current.name}.", 16, MUTED).apply {
                setPadding(dp(18), dp(24), dp(18), dp(24)); background = shape(WHITE, 16, true)
            })
            gap(content, 12); full(content, button("Adicionar primeiro item") { edit(null) }.apply { isEnabled = healthy })
        }
        store.items.forEach { item ->
            val card = row().apply { setPadding(0, dp(12), 0, dp(12)) }
            card.addView(Avatar(item.name, item.photo, 14), LinearLayout.LayoutParams(dp(58), dp(64)))
            val info = column().apply {
                addView(text(item.name, 17, INK, true))
                addView(text(if (item.id in store.drawn) "Já saiu nesta rodada" else "Na próxima escolha", 12, MUTED))
            }
            card.addView(info, LinearLayout.LayoutParams(0, -2, 1f).apply { leftMargin = dp(14) })
            card.addView(button("⋮") { itemMenu(item) }.apply {
                textSize = 24f; contentDescription = "Opções de ${item.name}"; isEnabled = healthy
                setPadding(0, 0, 0, 0)
            }, LinearLayout.LayoutParams(dp(48), dp(52)))
            content.addView(card)
            content.addView(View(this).apply { setBackgroundColor(LINE) }, LinearLayout.LayoutParams(-1, dp(1)))
        }
        gap(content, 20)
        content.addView(text("Seus itens ficam salvos neste aparelho.", 12, MUTED))
    }

    private fun change(mutation: () -> Unit): Boolean {
        if (!healthy) return false
        var previous: JSONObject? = null
        return try { previous = store.snapshot(); mutation(); store.save(); true }
        catch (_: Exception) {
            try { previous?.let { store.restore(it) } } catch (_: Exception) { healthy = false }
            toast("Não foi possível salvar. Verifique o espaço do aparelho e tente novamente."); false
        }
    }
    private fun chooseList() {
        val lists = store.lists.toList()
        val labels = lists.map { "${it.name} (${it.items.size})" }.toTypedArray()
        AlertDialog.Builder(this).setTitle("Qual sorteio vamos abrir?")
            .setSingleChoiceItems(labels, lists.indexOfFirst { it.id == store.selectedListId }) { dialog, which ->
                if (change { store.selectList(lists[which].id) }) { dialog.dismiss(); render() }
            }.setPositiveButton("Novo sorteio") { _, _ -> nameList() }
            .setNegativeButton("Cancelar", null).show()
    }
    private fun nameList(existing: ItemStore.DrawList? = null) {
        val body = dialogBody()
        body.addView(display(if (existing == null) "Um novo sorteio." else "Nome do sorteio.", 30)); gap(body, 8)
        body.addView(text("Separe suas escolhas por assunto.", 15, MUTED)); gap(body, 20)
        body.addView(text("Nome do sorteio", 14, INK, true)); gap(body, 8)
        val input = EditText(this).apply {
            setSingleLine(true); textSize = 17f; setTextColor(INK); setHintTextColor(MUTED)
            hint = "Ex.: Dia da semana, Roupa, Calçado"
            filters = arrayOf(InputFilter.LengthFilter(80))
            setPadding(dp(14), dp(12), dp(14), dp(12)); background = shape(BG, 12, true)
            contentDescription = "Nome do sorteio"; setText(existing?.name.orEmpty())
        }
        full(body, input); gap(body, 20)
        lateinit var dialog: Dialog
        full(body, button(if (existing == null) "Criar sorteio" else "Salvar nome", true) {
            val name = input.text.toString().trim()
            if (name.isEmpty()) { input.error = "Escreva um nome para o sorteio"; input.requestFocus() }
            else if (store.lists.any { it.id != existing?.id && it.name.equals(name, ignoreCase = true) }) {
                input.error = "Já existe um sorteio com esse nome"; input.requestFocus()
            } else if (change {
                if (existing == null) store.createList(name) else store.renameList(existing.id, name)
            }) { dialog.dismiss(); render() }
        })
        gap(body, 8); full(body, button("Cancelar") { dialog.dismiss() })
        dialog = showDialog(body)
    }
    private fun listMenu() {
        val list = store.current
        val options = if (store.lists.size > 1) arrayOf("Renomear sorteio", "Excluir sorteio") else arrayOf("Renomear sorteio")
        AlertDialog.Builder(this).setTitle(list.name).setItems(options) { _, which ->
            if (which == 0) nameList(list)
            else AlertDialog.Builder(this).setTitle("Excluir “${list.name}”?")
                .setMessage("Este sorteio e seus ${list.items.size} itens serão removidos. Os outros sorteios serão mantidos.")
                .setNegativeButton("Cancelar", null).setPositiveButton("Excluir") { _, _ ->
                    val photos = list.items.map { it.photo }
                    if (change { store.deleteList(list.id) }) { photos.forEach { discardPhoto(it) }; render() }
                }.show()
        }.show()
    }
    private fun resetRound() {
        AlertDialog.Builder(this).setTitle("Reiniciar a rodada?").setMessage("Os itens de “${store.current.name}” poderão ser sorteados novamente.")
            .setNegativeButton("Cancelar", null).setPositiveButton("Reiniciar") { _, _ ->
                if (change { store.drawn.clear(); store.lastId = "" }) render()
            }.show()
    }
    private fun itemMenu(item: ItemStore.Item) {
        AlertDialog.Builder(this).setTitle(item.name).setItems(arrayOf("Editar nome e foto", "Excluir item")) { _, which ->
            if (which == 0) edit(item)
            else AlertDialog.Builder(this).setTitle("Excluir “${item.name}”?").setMessage("O item será removido da sua lista.")
                .setNegativeButton("Cancelar", null).setPositiveButton("Excluir") { _, _ ->
                    if (change {
                        store.items.remove(item); store.drawn.remove(item.id)
                        if (store.lastId == item.id) store.lastId = ""
                    }) { discardPhoto(item.photo); render() }
                }.show()
        }.show()
    }
    private fun edit(item: ItemStore.Item?) {
        draftId = item?.id.orEmpty(); draftName = item?.name.orEmpty(); draftPhoto = item?.photo.orEmpty()
        showEditor()
    }
    private fun showEditor() {
        editorGeneration++; importing = false; editing = true
        val body = dialogBody()
        body.addView(display(if (draftId.isEmpty()) "Mais uma carta." else "Sua carta.", 32)); gap(body, 8)
        body.addView(text("Dê um nome. A foto é opcional.", 15, MUTED)); gap(body, 22)
        editorAvatar = Avatar(draftName, draftPhoto, 20)
        centered(body, editorAvatar, 136, 148); gap(body, 14)
        photoButton = button("Escolher foto") { pickPhoto() }; full(body, photoButton); gap(body, 6)
        full(body, button("Remover foto") {
            val old = draftPhoto; draftPhoto = ""; discardPhoto(old); editorAvatar.set(nameInput.text.toString(), "")
        })
        gap(body, 18); body.addView(text("Nome do item", 14, INK, true)); gap(body, 8)
        nameInput = EditText(this).apply {
            setSingleLine(true); textSize = 17f; setTextColor(INK); setHintTextColor(MUTED)
            hint = "Ex.: Pizza de sexta"; filters = arrayOf(InputFilter.LengthFilter(80))
            setPadding(dp(14), dp(12), dp(14), dp(12)); background = shape(BG, 12, true)
            setText(draftName); contentDescription = "Nome do item"
        }
        full(body, nameInput); gap(body, 22)
        saveButton = button("Salvar item", true) { saveDraft() }; full(body, saveButton); gap(body, 8)
        full(body, button("Cancelar") { editor?.dismiss() })
        editor = showDialog(body).apply {
            setOnDismissListener { editing = false; editorGeneration++; discardPhoto(draftPhoto) }
        }
    }
    private fun saveDraft() {
        if (importing) return
        val name = nameInput.text.toString().trim()
        if (name.isEmpty()) { nameInput.error = "Escreva um nome para o item"; nameInput.requestFocus(); return }
        val old = store.find(draftId)
        val next = ItemStore.Item(old?.id ?: UUID.randomUUID().toString(), name, draftPhoto)
        if (change { if (old == null) store.items.add(next) else store.items[store.items.indexOf(old)] = next }) {
            editor?.dismiss(); old?.let { discardPhoto(it.photo) }; render(); toast("Item salvo")
        }
    }
    private fun pickPhoto() {
        draftName = nameInput.text.toString()
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            try { startActivityForResult(Intent(MediaStore.ACTION_PICK_IMAGES).setType("image/*"), 10); return }
            catch (_: ActivityNotFoundException) { /* Fall back to the device gallery. */ }
        }
        try {
            startActivityForResult(Intent(Intent.ACTION_PICK).setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*"), 10)
        } catch (_: ActivityNotFoundException) { toast("Nenhum seletor de fotos disponível neste aparelho.") }
    }
    override fun onActivityResult(request: Int, status: Int, data: Intent?) {
        super.onActivityResult(request, status, data)
        val uri = data?.data ?: return
        if (request != 10 || status != RESULT_OK || !editing) return
        val generation = editorGeneration
        importing = true; photoButton.isEnabled = false; saveButton.isEnabled = false
        photoButton.setText(R.string.preparing_photo)
        images.execute {
            val destination = File(filesDir, "${UUID.randomUUID()}.jpg")
            try {
                val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)) { decoder, info, _ ->
                    val scale = min(1f, 720f / max(info.size.width, info.size.height))
                    decoder.setTargetSize(max(1, (info.size.width * scale).toInt()), max(1, (info.size.height * scale).toInt()))
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
                try {
                    destination.outputStream().use { if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it)) throw IOException() }
                } finally { bitmap.recycle() }
                handler.post {
                    if (isDestroyed || !editing || generation != editorGeneration) destination.delete()
                    else {
                        val old = draftPhoto; draftPhoto = destination.name; discardPhoto(old)
                        editorAvatar.set(nameInput.text.toString(), draftPhoto); finishImport()
                    }
                }
            } catch (_: Exception) { importFailed(destination, generation) }
            catch (_: OutOfMemoryError) { importFailed(destination, generation) }
        }
    }
    private fun importFailed(destination: File, generation: Int) {
        destination.delete()
        handler.post {
            if (!isDestroyed && editing && generation == editorGeneration) {
                finishImport(); toast("Não foi possível abrir essa imagem. Tente outra foto.")
            }
        }
    }
    private fun finishImport() {
        importing = false; photoButton.isEnabled = true; saveButton.isEnabled = true
        photoButton.setText(R.string.change_photo)
    }
    private fun discardPhoto(photo: String) {
        if (photo.isEmpty() || store.usesPhoto(photo)) return
        cache.remove("$photo:thumb"); cache.remove("$photo:full"); File(filesDir, photo).delete()
    }
    private fun bitmap(photo: String, thumb: Boolean): Bitmap? {
        if (photo.isEmpty()) return null
        val key = "$photo:${if (thumb) "thumb" else "full"}"
        return cache.get(key) ?: BitmapFactory.decodeFile(File(filesDir, photo).path,
            BitmapFactory.Options().apply { inSampleSize = if (thumb) 4 else 1 })?.also { cache.put(key, it) }
    }

    private fun draw() {
        val eligible = store.eligible()
        if (drawing || !healthy || eligible.isEmpty()) return
        val winner = store.find(DrawPool.pick(eligible, random)) ?: return
        // Persist the actual choice before the purely visual shuffle starts.
        if (!change { store.drawn.add(winner.id); store.lastId = winner.id }) return
        drawing = true; enableChildren(content, false); drawButton.isEnabled = false
        drawButton.setText(R.string.drawing)
        val motion = Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        if (motion == 0f) { finishDraw(winner); return }
        deck.candidates = eligible.mapNotNull { store.find(it) }
        deck.winner = winner
        deck.announceForAccessibility("Sorteando uma carta")
        // Bring the stage into view when drawing again from a scrolled list.
        (content.parent as? ScrollView)?.smoothScrollTo(0, max(0, deck.top - dp(16)))
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2400
            interpolator = android.view.animation.LinearInterpolator()
            addUpdateListener { deck.progress = it.animatedValue as Float; deck.invalidate() }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) { animator = null; finishDraw(winner) }
            })
            start()
        }
    }
    private fun finishDraw(winner: ItemStore.Item) {
        drawing = false
        if (!isDestroyed && !isFinishing) { render(); showResult(winner) }
    }
    private fun enableChildren(view: View, enabled: Boolean) {
        view.isEnabled = enabled
        if (view is ViewGroup) repeat(view.childCount) { enableChildren(view.getChildAt(it), enabled) }
    }
    private fun showResult(winner: ItemStore.Item) {
        if (isDestroyed || isFinishing) return
        val body = dialogBody()
        full(body, text("Sua carta saiu!", 18, BLUE, true).apply { gravity = Gravity.CENTER }); gap(body, 20)
        centered(body, Avatar(winner.name, winner.photo, 22).apply {
            contentDescription = "Foto de ${winner.name}"; importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }, 208, 224)
        gap(body, 20)
        val name = display(winner.name, 34).apply { gravity = Gravity.CENTER; accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        full(body, name); gap(body, 8)
        full(body, text("Sorteio: ${store.current.name}", 15, MUTED).apply { gravity = Gravity.CENTER }); gap(body, 24)
        val exhausted = store.eligible().isEmpty()
        full(body, button(if (exhausted) "Voltar à lista" else "Sortear outra carta", true) {
            result?.dismiss(); if (!exhausted) draw()
        })
        if (!exhausted) { gap(body, 8); full(body, button("Voltar à lista") { result?.dismiss() }) }
        result = showDialog(body)
        name.announceForAccessibility("Item sorteado: ${winner.name}")
    }
    private fun toast(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        out.putBoolean("editing", editing); out.putString("draftId", draftId); out.putString("draftPhoto", draftPhoto)
        out.putString("draftName", if (editing) nameInput.text.toString() else draftName)
        out.putBoolean("result", drawing || result?.isShowing == true)
    }
    override fun onDestroy() {
        animator?.removeAllListeners(); animator?.cancel(); animator = null
        handler.removeCallbacksAndMessages(null); images.shutdown()
        super.onDestroy()
    }

    private inner class Avatar(name: String, photo: String, private val radius: Int) : View(this@MainActivity) {
        private val photo = PhotoPainter()
        private val bounds = RectF()
        init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO; set(name, photo) }
        fun set(name: String, photo: String) {
            val initial = name.trim().let { if (it.isEmpty()) "+" else String(Character.toChars(it.codePointAt(0))).uppercase() }
            this.photo.set(bitmap(photo, radius == 14), initial); invalidate()
        }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            bounds.set(0f, 0f, width.toFloat(), height.toFloat())
            photo.draw(canvas, bounds, dp(radius).toFloat())
        }
    }
    private class PhotoPainter {
        private var image: Bitmap? = null
        private var initial = "+"
        private var shader: BitmapShader? = null
        private val matrix = Matrix()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            typeface = Typeface.create("sans-serif-rounded", Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        fun set(image: Bitmap?, initial: String) {
            this.image = image; this.initial = initial
            shader = image?.let { BitmapShader(it, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) }
        }
        fun draw(canvas: Canvas, bounds: RectF, radius: Float) {
            val bitmap = image
            if (bitmap != null) {
                val scale = max(bounds.width() / bitmap.width, bounds.height() / bitmap.height)
                matrix.setScale(scale, scale)
                matrix.postTranslate(bounds.centerX() - bitmap.width * scale / 2, bounds.centerY() - bitmap.height * scale / 2)
                shader?.setLocalMatrix(matrix)
                paint.shader = shader; canvas.drawRoundRect(bounds, radius, radius, paint); paint.shader = null
            } else {
                paint.color = PALE; canvas.drawRoundRect(bounds, radius, radius, paint)
                paint.color = BLUE; paint.textSize = bounds.width() * .34f
                canvas.drawText(initial, bounds.centerX(), bounds.centerY() - (paint.ascent() + paint.descent()) / 2, paint)
            }
        }
    }

    /** A deck cuts sideways, slows down, then turns its last card face up. No spinning wheel. */
    private inner class DeckView : View(this@MainActivity) {
        var progress = 0f
        var candidates = emptyList<ItemStore.Item>()
        var winner: ItemStore.Item? = null
            set(value) {
                field = value
                value?.let { photo.set(bitmap(it.photo, true), String(Character.toChars(it.name.codePointAt(0))).uppercase()) }
            }
        private val photo = PhotoPainter()
        private val photoBounds = RectF(-66f, -86f, 66f, 42f)
        private val cardBounds = RectF(-84f, -110f, 84f, 108f)
        private val shadowBounds = RectF(-85f, -106f, 85f, 114f)
        private val borderBounds = RectF(-74f, -100f, 74f, 98f)
        private val decelerate = DecelerateInterpolator(1.4f)
        private val titleFace = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        private val labelFace = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        private val labelPaint = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 14f; typeface = labelFace }
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        override fun onMeasure(widthSpec: Int, heightSpec: Int) {
            setMeasuredDimension(MeasureSpec.getSize(widthSpec), dp(230))
        }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val unit = min(width / 330f, height / 258f)
            canvas.save(); canvas.translate(width / 2f, height / 2f); canvas.scale(unit, unit)
            val p = progress
            val shuffle = decelerate.getInterpolation(min(p / .78f, 1f)) * 11f
            val cut = if (p in .001f.. .78f) sin((shuffle % 1f) * Math.PI).toFloat() else 0f
            val direction = if (shuffle.toInt() % 2 == 0) 1 else -1
            cardBack(canvas, -25f, 7f, -12f, PEACH)
            cardBack(canvas, 25f, 0f, 12f, PALE)
            canvas.save()
            canvas.translate(cut * 72f * direction, -cut * 12f)
            canvas.rotate(cut * 7f * direction)
            if (p < .78f || winner == null) {
                cardBack(canvas, 0f, 0f, 0f, BLUE)
                paint.color = WHITE; paint.textAlign = Paint.Align.CENTER
                paint.typeface = titleFace; paint.textSize = 60f
                canvas.drawText("?", 0f, 8f, paint)
                paint.textSize = 14f; paint.typeface = labelFace
                val label = if (candidates.isEmpty()) "Sua próxima escolha" else candidates[shuffle.toInt() % candidates.size].name
                drawLabel(canvas, label, 55f, WHITE)
            } else {
                val reveal = ((p - .78f) / .22f).coerceIn(0f, 1f)
                val flip = abs(cos(reveal * Math.PI)).toFloat().coerceAtLeast(.025f)
                canvas.scale(flip, 1f)
                if (reveal < .5f) cardBack(canvas, 0f, 0f, 0f, BLUE)
                else {
                    cardBack(canvas, 0f, 0f, 0f, WHITE)
                    val item = winner!!
                    photo.draw(canvas, photoBounds, 12f)
                    drawLabel(canvas, item.name, 72f, INK)
                }
            }
            canvas.restore(); canvas.restore()
        }
        private fun cardBack(canvas: Canvas, x: Float, y: Float, angle: Float, color: Int) {
            canvas.save(); canvas.translate(x, y); canvas.rotate(angle)
            paint.shader = null; paint.color = LINE
            canvas.drawRoundRect(shadowBounds, 19f, 19f, paint)
            paint.color = color
            canvas.drawRoundRect(cardBounds, 18f, 18f, paint)
            if (color == BLUE) {
                paint.color = 0xFF5476ED.toInt(); paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.3f
                canvas.drawRoundRect(borderBounds, 12f, 12f, paint)
                paint.style = Paint.Style.FILL
            }
            canvas.restore()
        }
        private fun drawLabel(canvas: Canvas, label: String, y: Float, color: Int) {
            paint.color = color; paint.textSize = 14f; paint.textAlign = Paint.Align.CENTER
            paint.typeface = labelFace
            val fitted = android.text.TextUtils.ellipsize(label, labelPaint, 140f, android.text.TextUtils.TruncateAt.END)
            canvas.drawText(fitted.toString(), 0f, y, paint)
        }
    }
    companion object {
        private val BG = 0xFFF1F5FB.toInt()
        private val INK = 0xFF172D4D.toInt()
        private val MUTED = 0xFF586A83.toInt()
        private val BLUE = 0xFF254EDB.toInt()
        private val PALE = 0xFFDFE8FF.toInt()
        private val PEACH = 0xFFFFD9BA.toInt()
        private val LINE = 0xFFD4DEEC.toInt()
        private val WHITE = Color.WHITE
    }
}
