package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiMemeSuggester
import com.example.ai.GeminiMemeSuggestion
import com.example.data.AppRepository
import com.example.data.StickerEntity
import com.example.data.StickerPackEntity
import com.example.data.SubscriptionEntity
import com.example.data.UserEntity
import com.example.engine.CutoutAlgorithm
import com.example.engine.StickerEngine
import com.example.model.StickerEmojiItem
import com.example.model.StickerFontFamily
import com.example.model.StickerTextAlign
import com.example.model.StickerTextBox
import com.example.model.StickerTextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class Screen {
    LANDING,
    AUTH,
    DASHBOARD,
    CAMERA,
    RECORTE,
    EDITOR,
    BIBLIOTECA,
    PACOTES,
    ASSINATURA,
    CONFIGURACOES
}

data class PackWithStickers(
    val id: String,
    val name: String,
    val description: String = "",
    val stickers: List<StickerEntity> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class EditorState(
    val captionText: String = "",
    val captionColor: Int = Color.WHITE,
    val outlineColor: Int = Color.WHITE,
    val outlineThickness: Float = 14f,
    val outlineStyle: String = "SOLID",
    val backgroundType: String = "TRANSPARENT",
    val customBgColor: Int = Color.TRANSPARENT,
    val filter: String = "NONE",
    val accessory: String = "NONE",
    val speechBalloonText: String = "",
    val emojiText: String = "",
    val memeCategory: String = "😂 Engraçado",
    val textBoxes: List<StickerTextBox> = emptyList(),
    val selectedTextBoxId: String? = null,
    val emojiItems: List<StickerEmojiItem> = emptyList(),
    val selectedEmojiId: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = AppRepository(application.applicationContext)

    private val _currentScreen = MutableStateFlow(Screen.LANDING)
    val currentScreen = _currentScreen.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser = _currentUser.asStateFlow()

    private val _subscription = MutableStateFlow<SubscriptionEntity?>(null)
    val subscription = _subscription.asStateFlow()

    private val _stickers = MutableStateFlow<List<StickerEntity>>(emptyList())
    val stickers = _stickers.asStateFlow()

    private val _packs = MutableStateFlow<List<StickerPackEntity>>(emptyList())
    val packs = _packs.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _processingMessage = MutableStateFlow("Processando...")
    val processingMessage = _processingMessage.asStateFlow()

    // Photo & Cutout states
    var rawPhotoBitmap: Bitmap? = null
        private set
    var cutoutBitmap: Bitmap? = null
        private set
    var previewStickerBitmap: Bitmap? = null
        private set
    private val _previewStickerBitmapFlow = MutableStateFlow<Bitmap?>(null)
    val previewStickerBitmapFlow = _previewStickerBitmapFlow.asStateFlow()

    private val _selectedCutoutAlgorithm = MutableStateFlow(CutoutAlgorithm.ADAPTIVE_CHROMA_DEPTH)
    val selectedCutoutAlgorithm = _selectedCutoutAlgorithm.asStateFlow()

    private val _cutoutSensitivity = MutableStateFlow(1.0f)
    val cutoutSensitivity = _cutoutSensitivity.asStateFlow()

    // Gemini AI Meme Suggestions State
    private val _geminiSuggestions = MutableStateFlow<List<GeminiMemeSuggestion>>(emptyList())
    val geminiSuggestions = _geminiSuggestions.asStateFlow()

    private val _isGeneratingMemeAi = MutableStateFlow(false)
    val isGeneratingMemeAi = _isGeneratingMemeAi.asStateFlow()

    private val _geminiAiError = MutableStateFlow<String?>(null)
    val geminiAiError = _geminiAiError.asStateFlow()

    // Editor live state
    private val _editorState = MutableStateFlow(EditorState())
    val editorState = _editorState.asStateFlow()

    // Undo / Redo & Action Feedback
    data class EditorSnapshot(
        val state: EditorState,
        val cutoutBitmap: Bitmap?,
        val actionName: String
    )

    private val undoStack = mutableListOf<EditorSnapshot>()
    private val redoStack = mutableListOf<EditorSnapshot>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo = _canRedo.asStateFlow()

    private val _lastActionMessage = MutableStateFlow<String?>(null)
    val lastActionMessage = _lastActionMessage.asStateFlow()

    fun dismissLastActionMessage() {
        _lastActionMessage.value = null
    }

    private fun pushUndo(actionName: String) {
        undoStack.add(
            EditorSnapshot(
                state = _editorState.value,
                cutoutBitmap = cutoutBitmap,
                actionName = actionName
            )
        )
        if (undoStack.size > 25) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = false
        _lastActionMessage.value = actionName
    }

    fun undo(): Boolean {
        if (undoStack.isEmpty()) return false
        val previous = undoStack.removeAt(undoStack.lastIndex)
        redoStack.add(
            EditorSnapshot(
                state = _editorState.value,
                cutoutBitmap = cutoutBitmap,
                actionName = previous.actionName
            )
        )
        _editorState.value = previous.state
        if (previous.cutoutBitmap != null) {
            cutoutBitmap = previous.cutoutBitmap
        }
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        _lastActionMessage.value = "Desfeito: ${previous.actionName}"
        updateStickerPreview()
        return true
    }

    fun redo(): Boolean {
        if (redoStack.isEmpty()) return false
        val next = redoStack.removeAt(redoStack.lastIndex)
        undoStack.add(
            EditorSnapshot(
                state = _editorState.value,
                cutoutBitmap = cutoutBitmap,
                actionName = next.actionName
            )
        )
        _editorState.value = next.state
        if (next.cutoutBitmap != null) {
            cutoutBitmap = next.cutoutBitmap
        }
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        _lastActionMessage.value = "Refeito: ${next.actionName}"
        updateStickerPreview()
        return true
    }

    // Active sticker to view in modal
    private val _selectedSticker = MutableStateFlow<StickerEntity?>(null)
    val selectedSticker = _selectedSticker.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initialize()
            repository.currentUser.collectLatest { user ->
                _currentUser.value = user
                if (user != null) {
                    launch {
                        repository.getSubscriptionFlow(user.id).collectLatest { sub ->
                            _subscription.value = sub
                        }
                    }
                    launch {
                        repository.getStickersFlow(user.id).collectLatest { list ->
                            _stickers.value = list
                        }
                    }
                    launch {
                        repository.getPacksFlow(user.id).collectLatest { list ->
                            _packs.value = list
                        }
                    }
                }
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setSelectedSticker(sticker: StickerEntity?) {
        _selectedSticker.value = sticker
    }

    // Photo input
    fun onPhotoSelected(bitmap: Bitmap) {
        rawPhotoBitmap = bitmap
        startCutoutProcess(bitmap)
    }

    fun onPhotoUriSelected(uri: Uri) {
        loadBitmapFromUri(uri) { bmp ->
            rawPhotoBitmap = bmp
            startCutoutProcess(bmp)
        }
    }

    fun loadBitmapFromUri(uri: Uri, onLoaded: (Bitmap) -> Unit) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Carregando foto com alta definição..."
            val bmp = StickerEngine.loadAndResizeBitmap(getApplication(), uri)
            _isProcessing.value = false
            if (bmp != null) {
                onLoaded(bmp)
            } else {
                Toast.makeText(getApplication(), "Não foi possível carregar a imagem", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCutoutProcess(source: Bitmap) {
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Processando imagem e removendo fundo..."
            val cutout = StickerEngine.autoCutoutPerson(
                source = source,
                algorithm = _selectedCutoutAlgorithm.value,
                sensitivity = _cutoutSensitivity.value
            )
            cutoutBitmap = cutout
            _isProcessing.value = false
            _currentScreen.value = Screen.RECORTE
        }
    }

    fun setCutoutAlgorithm(algorithm: CutoutAlgorithm) {
        _selectedCutoutAlgorithm.value = algorithm
        reprocessCutout()
    }

    fun setCutoutSensitivity(sensitivity: Float) {
        _cutoutSensitivity.value = sensitivity
        reprocessCutout()
    }

    fun reprocessCutout() {
        val source = rawPhotoBitmap ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Aplicando algoritmo: ${_selectedCutoutAlgorithm.value.displayName}..."
            val cutout = StickerEngine.autoCutoutPerson(
                source = source,
                algorithm = _selectedCutoutAlgorithm.value,
                sensitivity = _cutoutSensitivity.value
            )
            cutoutBitmap = cutout
            _isProcessing.value = false
        }
    }

    fun manualCutoutUpdate(updatedCutout: Bitmap) {
        cutoutBitmap = updatedCutout
    }

    fun proceedToEditor() {
        if (cutoutBitmap != null) {
            if (_editorState.value.textBoxes.isEmpty()) {
                val defaultBox = StickerTextBox(
                    text = "MINHA FIGURINHA",
                    textColor = Color.WHITE,
                    textStyle = StickerTextStyle.MEME_STROKE,
                    fontFamily = StickerFontFamily.DEFAULT,
                    normalizedY = 0.86f
                )
                _editorState.value = _editorState.value.copy(
                    textBoxes = listOf(defaultBox),
                    selectedTextBoxId = defaultBox.id,
                    captionText = defaultBox.text
                )
            }
            updateStickerPreview()
            _currentScreen.value = Screen.EDITOR
            // Automatically request Gemini meme suggestions based on the captured photo
            loadGeminiMemeSuggestions()
        }
    }

    /**
     * Chama a API Gemini para analisar a foto capturada e sugerir automaticamente
     * frases de memes e legendas engraçadas adaptadas à expressão e imagem.
     */
    fun loadGeminiMemeSuggestions() {
        val targetBitmap = cutoutBitmap ?: rawPhotoBitmap ?: return
        viewModelScope.launch {
            _isGeneratingMemeAi.value = true
            _geminiAiError.value = null
            try {
                val suggestions = GeminiMemeSuggester.suggestMemeCaptions(targetBitmap)
                _geminiSuggestions.value = suggestions
            } catch (e: Exception) {
                _geminiAiError.value = "Não foi possível carregar sugestões: ${e.localizedMessage}"
                _geminiSuggestions.value = GeminiMemeSuggester.getLocalFallbackSuggestions()
            } finally {
                _isGeneratingMemeAi.value = false
            }
        }
    }

    // Editor controls
    fun updateCaption(text: String, color: Int = _editorState.value.captionColor) {
        pushUndo("Alterar Legenda")
        _editorState.value = _editorState.value.copy(captionText = text, captionColor = color)
        updateStickerPreview()
    }

    fun updateOutline(color: Int, thickness: Float, style: String = _editorState.value.outlineStyle) {
        pushUndo("Borda $style")
        _editorState.value = _editorState.value.copy(
            outlineColor = color,
            outlineThickness = thickness,
            outlineStyle = style
        )
        updateStickerPreview()
    }

    fun updateBackground(type: String, customColor: Int = Color.TRANSPARENT) {
        pushUndo("Fundo $type")
        _editorState.value = _editorState.value.copy(backgroundType = type, customBgColor = customColor)
        updateStickerPreview()
    }

    fun updateFilter(filter: String) {
        pushUndo("Filtro $filter")
        _editorState.value = _editorState.value.copy(filter = filter)
        updateStickerPreview()
    }

    fun updateAccessory(accessory: String) {
        pushUndo("Acessório $accessory")
        _editorState.value = _editorState.value.copy(accessory = accessory)
        updateStickerPreview()
    }

    fun updateSpeechBalloon(text: String) {
        pushUndo("Balão de Fala")
        _editorState.value = _editorState.value.copy(speechBalloonText = text)
        updateStickerPreview()
    }

    fun updateEmoji(emoji: String) {
        _editorState.value = _editorState.value.copy(emojiText = emoji)
        updateStickerPreview()
    }

    fun updateMemeCategory(category: String, phrase: String = "") {
        pushUndo("Categoria $category")
        _editorState.value = _editorState.value.copy(
            memeCategory = category,
            captionText = if (phrase.isNotBlank()) phrase else _editorState.value.captionText
        )
        // If phrase is provided, also synchronize or add as a text box
        if (phrase.isNotBlank()) {
            if (_editorState.value.textBoxes.isEmpty()) {
                addTextBox(phrase)
            } else {
                updateSelectedTextBox { it.copy(text = phrase) }
            }
        }
        updateStickerPreview()
    }

    // --- Dynamic Text Boxes Management ---
    private var textBoxDragInitialSnapshot: EditorState? = null

    fun onStartTextBoxDrag(id: String) {
        selectTextBox(id)
        textBoxDragInitialSnapshot = _editorState.value
    }

    fun onMoveTextBox(id: String, newX: Float, newY: Float) {
        val updated = _editorState.value.textBoxes.map { box ->
            if (box.id == id) box.copy(normalizedX = newX, normalizedY = newY) else box
        }
        _editorState.value = _editorState.value.copy(textBoxes = updated)
        updateStickerPreview()
    }

    fun onEndTextBoxDrag() {
        val initial = textBoxDragInitialSnapshot
        if (initial != null && initial.textBoxes != _editorState.value.textBoxes) {
            undoStack.add(EditorSnapshot(initial, cutoutBitmap, "Mover texto"))
            redoStack.clear()
            _canUndo.value = true
            _canRedo.value = false
            _lastActionMessage.value = "Texto reposicionado"
        }
        textBoxDragInitialSnapshot = null
    }

    fun addTextBox(
        initialText: String = "NOVO TEXTO",
        color: Int = Color.WHITE,
        style: StickerTextStyle = StickerTextStyle.MEME_STROKE,
        font: StickerFontFamily = StickerFontFamily.DEFAULT
    ) {
        pushUndo("Adicionar Texto")
        val count = _editorState.value.textBoxes.size
        // Stagger vertical position so new boxes don't overlap completely
        val posY = when (count) {
            0 -> 0.85f // bottom caption
            1 -> 0.15f // top caption
            2 -> 0.50f // middle caption
            else -> (0.2f + (count * 0.15f)).coerceIn(0.1f, 0.9f)
        }
        val newBox = StickerTextBox(
            text = initialText,
            textColor = color,
            textStyle = style,
            fontFamily = font,
            normalizedY = posY
        )
        val updatedList = _editorState.value.textBoxes + newBox
        _editorState.value = _editorState.value.copy(
            textBoxes = updatedList,
            selectedTextBoxId = newBox.id,
            captionText = newBox.text
        )
        updateStickerPreview()
    }

    fun selectTextBox(id: String?) {
        _editorState.value = _editorState.value.copy(selectedTextBoxId = id)
    }

    fun updateSelectedTextBox(transform: (StickerTextBox) -> StickerTextBox) {
        val selectedId = _editorState.value.selectedTextBoxId ?: _editorState.value.textBoxes.firstOrNull()?.id ?: return
        val updatedList = _editorState.value.textBoxes.map { box ->
            if (box.id == selectedId) transform(box) else box
        }
        _editorState.value = _editorState.value.copy(textBoxes = updatedList)
        updateStickerPreview()
    }

    fun removeSelectedTextBox() {
        val selectedId = _editorState.value.selectedTextBoxId ?: return
        removeTextBox(selectedId)
    }

    fun removeTextBox(id: String) {
        pushUndo("Remover Caixa de Texto")
        val updatedList = _editorState.value.textBoxes.filter { it.id != id }
        _editorState.value = _editorState.value.copy(
            textBoxes = updatedList,
            selectedTextBoxId = updatedList.firstOrNull()?.id
        )
        updateStickerPreview()
    }

    fun duplicateTextBox(id: String) {
        val box = _editorState.value.textBoxes.find { it.id == id } ?: return
        pushUndo("Duplicar Texto")
        val duplicate = box.copy(
            id = java.util.UUID.randomUUID().toString(),
            normalizedY = (box.normalizedY + 0.12f).coerceIn(0.1f, 0.9f)
        )
        val updatedList = _editorState.value.textBoxes + duplicate
        _editorState.value = _editorState.value.copy(
            textBoxes = updatedList,
            selectedTextBoxId = duplicate.id
        )
        updateStickerPreview()
    }

    fun addMemeTemplateTextBoxes(
        topText: String = "QUANDO VOCÊ",
        bottomText: String = "PERCEBE O QUE FEZ"
    ) {
        pushUndo("Template Meme (Topo & Base)")
        val topBox = StickerTextBox(
            text = topText,
            textColor = Color.WHITE,
            strokeColor = Color.BLACK,
            fontSize = 42f,
            fontFamily = StickerFontFamily.IMPACT_MEME,
            textStyle = StickerTextStyle.MEME_STROKE,
            isBold = true,
            isUppercase = true,
            normalizedX = 0.5f,
            normalizedY = 0.12f
        )
        val bottomBox = StickerTextBox(
            text = bottomText,
            textColor = Color.WHITE,
            strokeColor = Color.BLACK,
            fontSize = 42f,
            fontFamily = StickerFontFamily.IMPACT_MEME,
            textStyle = StickerTextStyle.MEME_STROKE,
            isBold = true,
            isUppercase = true,
            normalizedX = 0.5f,
            normalizedY = 0.88f
        )
        val updatedList = listOf(topBox, bottomBox)
        _editorState.value = _editorState.value.copy(
            textBoxes = updatedList,
            selectedTextBoxId = bottomBox.id,
            captionText = bottomText
        )
        updateStickerPreview()
    }

    fun updateTextBoxRotation(id: String, rotationDegrees: Float) {
        val normRot = ((rotationDegrees + 180f) % 360f) - 180f
        val updatedList = _editorState.value.textBoxes.map { box ->
            if (box.id == id) box.copy(rotationDegrees = normRot) else box
        }
        _editorState.value = _editorState.value.copy(textBoxes = updatedList)
        updateStickerPreview()
    }

    fun rotateSelectedTextBoxBy(degrees: Float) {
        val selectedId = _editorState.value.selectedTextBoxId ?: return
        val box = _editorState.value.textBoxes.find { it.id == selectedId } ?: return
        updateTextBoxRotation(selectedId, box.rotationDegrees + degrees)
    }

    fun bringTextBoxToFront(id: String) {
        val box = _editorState.value.textBoxes.find { it.id == id } ?: return
        pushUndo("Trazer Texto para Frente")
        val updatedList = _editorState.value.textBoxes.filter { it.id != id } + box
        _editorState.value = _editorState.value.copy(
            textBoxes = updatedList,
            selectedTextBoxId = id
        )
        updateStickerPreview()
    }

    // --- Crop & Transformation Controls ---
    fun cropCutoutSquare() {
        val current = cutoutBitmap ?: return
        pushUndo("Cortar Quadrado 1:1")
        val cropped = StickerEngine.cropSquare(current)
        cutoutBitmap = cropped
        updateStickerPreview()
    }

    fun trimCutoutBorders() {
        val current = cutoutBitmap ?: return
        pushUndo("Ajustar Bordas")
        val trimmed = StickerEngine.trimTransparentBorders(current)
        cutoutBitmap = trimmed
        updateStickerPreview()
    }

    fun rotateCutout(degrees: Float) {
        val current = cutoutBitmap ?: return
        pushUndo("Girar ${degrees.toInt()}°")
        val rotated = StickerEngine.rotateBitmap(current, degrees)
        cutoutBitmap = rotated
        updateStickerPreview()
    }

    fun flipCutoutHorizontal() {
        val current = cutoutBitmap ?: return
        pushUndo("Espelhar Recorte")
        val flipped = StickerEngine.flipHorizontal(current)
        cutoutBitmap = flipped
        updateStickerPreview()
    }

    // --- Dynamic Overlaid Emojis Management ---
    private var emojiDragInitialSnapshot: EditorState? = null

    fun addEmojiItem(emoji: String) {
        pushUndo("Adicionar Emoji $emoji")
        val count = _editorState.value.emojiItems.size
        // Posição inicial centralizada com ligeiro offset para múltiplos emojis
        val offsetX = ((count % 3) - 1) * 0.12f
        val offsetY = (((count / 3) % 3) - 1) * 0.12f
        val posX = (0.5f + offsetX).coerceIn(0.2f, 0.8f)
        val posY = (0.5f + offsetY).coerceIn(0.2f, 0.8f)

        val newItem = StickerEmojiItem(
            emoji = emoji,
            normalizedX = posX,
            normalizedY = posY,
            scale = 1.0f,
            rotationDegrees = 0f
        )
        val updatedList = _editorState.value.emojiItems + newItem
        _editorState.value = _editorState.value.copy(
            emojiItems = updatedList,
            selectedEmojiId = newItem.id
        )
        updateStickerPreview()
    }

    fun selectEmojiItem(id: String?) {
        _editorState.value = _editorState.value.copy(selectedEmojiId = id)
    }

    fun updateSelectedEmoji(transform: (StickerEmojiItem) -> StickerEmojiItem) {
        val selectedId = _editorState.value.selectedEmojiId ?: return
        val updatedList = _editorState.value.emojiItems.map { item ->
            if (item.id == selectedId) transform(item) else item
        }
        _editorState.value = _editorState.value.copy(emojiItems = updatedList)
        updateStickerPreview()
    }

    fun updateEmojiScale(id: String, scale: Float) {
        val clamped = scale.coerceIn(0.4f, 3.5f)
        val updatedList = _editorState.value.emojiItems.map { item ->
            if (item.id == id) item.copy(scale = clamped) else item
        }
        _editorState.value = _editorState.value.copy(emojiItems = updatedList)
        updateStickerPreview()
    }

    fun updateEmojiRotation(id: String, rotation: Float) {
        val normRot = ((rotation + 180f) % 360f) - 180f
        val updatedList = _editorState.value.emojiItems.map { item ->
            if (item.id == id) item.copy(rotationDegrees = normRot) else item
        }
        _editorState.value = _editorState.value.copy(emojiItems = updatedList)
        updateStickerPreview()
    }

    fun rotateSelectedEmojiBy(degrees: Float) {
        val selectedId = _editorState.value.selectedEmojiId ?: return
        val item = _editorState.value.emojiItems.find { it.id == selectedId } ?: return
        updateEmojiRotation(selectedId, item.rotationDegrees + degrees)
    }

    fun flipSelectedEmoji() {
        val selectedId = _editorState.value.selectedEmojiId ?: return
        pushUndo("Espelhar Emoji")
        val updatedList = _editorState.value.emojiItems.map { item ->
            if (item.id == selectedId) item.copy(isFlipped = !item.isFlipped) else item
        }
        _editorState.value = _editorState.value.copy(emojiItems = updatedList)
        updateStickerPreview()
    }

    fun duplicateEmoji(id: String) {
        val item = _editorState.value.emojiItems.find { it.id == id } ?: return
        pushUndo("Duplicar Emoji ${item.emoji}")
        val duplicate = item.copy(
            id = java.util.UUID.randomUUID().toString(),
            normalizedX = (item.normalizedX + 0.08f).coerceIn(0.1f, 0.9f),
            normalizedY = (item.normalizedY + 0.08f).coerceIn(0.1f, 0.9f)
        )
        val updatedList = _editorState.value.emojiItems + duplicate
        _editorState.value = _editorState.value.copy(
            emojiItems = updatedList,
            selectedEmojiId = duplicate.id
        )
        updateStickerPreview()
    }

    fun onStartEmojiDrag(id: String) {
        selectEmojiItem(id)
        emojiDragInitialSnapshot = _editorState.value
    }

    fun onMoveEmoji(id: String, newX: Float, newY: Float) {
        val updated = _editorState.value.emojiItems.map { item ->
            if (item.id == id) item.copy(normalizedX = newX, normalizedY = newY) else item
        }
        _editorState.value = _editorState.value.copy(emojiItems = updated)
        updateStickerPreview()
    }

    fun onEndEmojiDrag() {
        val initial = emojiDragInitialSnapshot
        if (initial != null && initial.emojiItems != _editorState.value.emojiItems) {
            undoStack.add(EditorSnapshot(initial, cutoutBitmap, "Mover emoji"))
            redoStack.clear()
            _canUndo.value = true
            _canRedo.value = false
            _lastActionMessage.value = "Emoji reposicionado"
        }
        emojiDragInitialSnapshot = null
    }

    fun removeSelectedEmoji() {
        val selectedId = _editorState.value.selectedEmojiId ?: return
        removeEmoji(selectedId)
    }

    fun removeEmoji(id: String) {
        pushUndo("Remover Emoji")
        val updated = _editorState.value.emojiItems.filter { it.id != id }
        _editorState.value = _editorState.value.copy(
            emojiItems = updated,
            selectedEmojiId = updated.lastOrNull()?.id
        )
        updateStickerPreview()
    }

    fun clearAllEmojis() {
        if (_editorState.value.emojiItems.isEmpty()) return
        pushUndo("Limpar Todos os Emojis")
        _editorState.value = _editorState.value.copy(
            emojiItems = emptyList(),
            selectedEmojiId = null
        )
        updateStickerPreview()
    }

    fun updateStickerPreview() {
        val currentCutout = cutoutBitmap ?: return
        val state = _editorState.value
        val rendered = StickerEngine.renderFinalSticker(
            cutoutBitmap = currentCutout,
            outlineColor = state.outlineColor,
            outlineThickness = state.outlineThickness,
            outlineStyle = state.outlineStyle,
            backgroundType = state.backgroundType,
            customBgColor = state.customBgColor,
            filter = state.filter,
            captionText = state.captionText,
            captionColor = state.captionColor,
            speechBalloonText = state.speechBalloonText,
            emojiText = state.emojiText,
            accessory = state.accessory,
            textBoxes = state.textBoxes,
            emojiItems = state.emojiItems
        )
        previewStickerBitmap = rendered
        _previewStickerBitmapFlow.value = rendered
    }

    // Save & Share
    fun saveCurrentSticker(onSuccess: (StickerEntity) -> Unit = {}) {
        val bitmap = previewStickerBitmap ?: cutoutBitmap ?: return
        val user = _currentUser.value ?: return

        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Salvando figurinha na biblioteca..."
            val title = if (_editorState.value.captionText.isNotBlank()) {
                _editorState.value.captionText
            } else {
                "Figurinha ${System.currentTimeMillis() % 10000}"
            }
            val saved = repository.saveSticker(user.id, title, bitmap, _editorState.value.memeCategory)
            _isProcessing.value = false
            Toast.makeText(getApplication(), "Figurinha salva com sucesso!", Toast.LENGTH_SHORT).show()
            onSuccess(saved)
        }
    }

    fun shareCurrentStickerWhatsApp() {
        exportCurrentStickerWhatsAppWebp()
    }

    fun exportCurrentStickerWhatsAppWebp(packName: String = "Meu Pacote WhatsApp", onSuccess: () -> Unit = {}) {
        val bitmap = previewStickerBitmap ?: cutoutBitmap ?: return
        val user = _currentUser.value

        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Formatando WebP transparente para o WhatsApp..."
            val webpFile = StickerEngine.saveBitmapAsWebpFile(
                getApplication(),
                bitmap,
                "sticker_${System.currentTimeMillis()}.webp"
            )

            // Also persist into the user's library with the designated pack name
            if (user != null) {
                val title = if (_editorState.value.captionText.isNotBlank()) {
                    _editorState.value.captionText
                } else {
                    "Sticker WebP ${System.currentTimeMillis() % 10000}"
                }
                repository.saveSticker(user.id, title, bitmap, packName)
            }

            _isProcessing.value = false
            Toast.makeText(getApplication(), "Abrindo WhatsApp com sua figurinha WebP!", Toast.LENGTH_SHORT).show()
            repository.shareStickerWebp(webpFile.absolutePath, directWhatsApp = true)
            onSuccess()
        }
    }

    fun shareCurrentStickerWebpOtherApps() {
        val bitmap = previewStickerBitmap ?: cutoutBitmap ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Gerando arquivo WebP transparente..."
            val webpFile = StickerEngine.saveBitmapAsWebpFile(
                getApplication(),
                bitmap,
                "sticker_share_${System.currentTimeMillis()}.webp"
            )
            _isProcessing.value = false
            repository.shareStickerWebp(webpFile.absolutePath, directWhatsApp = false)
        }
    }

    fun downloadCurrentStickerWebp() {
        val bitmap = previewStickerBitmap ?: cutoutBitmap ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Salvando WebP transparente no dispositivo..."
            val webpFile = StickerEngine.saveBitmapAsWebpFile(
                getApplication(),
                bitmap,
                "sticker_${System.currentTimeMillis()}.webp"
            )
            val success = repository.downloadWebpToGallery(webpFile.absolutePath)
            _isProcessing.value = false
            if (success) {
                Toast.makeText(getApplication(), "Figurinha WebP salva em Imagens/WhatsAppStickers!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(getApplication(), "Figurinha WebP pronta nos arquivos!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun downloadCurrentStickerPng() {
        val bitmap = previewStickerBitmap ?: cutoutBitmap ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Baixando PNG com fundo transparente..."
            val tempFile = StickerEngine.saveBitmapToFile(
                getApplication(),
                bitmap,
                "dl_${System.currentTimeMillis()}.png"
            )
            val success = repository.downloadPngToGallery(tempFile.absolutePath)
            _isProcessing.value = false
            if (success) {
                Toast.makeText(getApplication(), "Figurinha PNG salva na sua Galeria!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(getApplication(), "Figurinha pronta nos arquivos!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun shareExistingStickerWhatsApp(sticker: StickerEntity) {
        viewModelScope.launch {
            val srcFile = File(sticker.imagePath)
            if (srcFile.name.endsWith(".webp", ignoreCase = true)) {
                repository.shareStickerWebp(sticker.imagePath, directWhatsApp = true)
            } else {
                val bitmap = BitmapFactory.decodeFile(sticker.imagePath)
                if (bitmap != null) {
                    val webpFile = StickerEngine.saveBitmapAsWebpFile(
                        getApplication(),
                        bitmap,
                        "sticker_${sticker.id}.webp"
                    )
                    repository.shareStickerWebp(webpFile.absolutePath, directWhatsApp = true)
                } else {
                    repository.shareSticker(sticker.imagePath, directWhatsApp = true)
                }
            }
        }
    }

    fun downloadExistingStickerPng(sticker: StickerEntity) {
        viewModelScope.launch {
            val success = repository.downloadPngToGallery(sticker.imagePath)
            if (success) {
                Toast.makeText(getApplication(), "Figurinha salva na Galeria!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun duplicateSticker(sticker: StickerEntity) {
        viewModelScope.launch {
            repository.duplicateSticker(sticker)
            Toast.makeText(getApplication(), "Figurinha duplicada!", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteSticker(id: String) {
        viewModelScope.launch {
            repository.deleteSticker(id)
            if (_selectedSticker.value?.id == id) {
                _selectedSticker.value = null
            }
            Toast.makeText(getApplication(), "Figurinha removida!", Toast.LENGTH_SHORT).show()
        }
    }

    // Subscription & Auth actions
    fun subscribe(method: String = "PIX") {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Ativando plano Figurinhas Pro..."
            val success = repository.subscribePlan(user.id, method)
            _isProcessing.value = false
            if (success) {
                Toast.makeText(getApplication(), "Plano Figurinhas Pro Ativado com Sucesso! 🟢", Toast.LENGTH_LONG).show()
                _currentScreen.value = Screen.DASHBOARD
            }
        }
    }

    fun cancelSubscription() {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.cancelSubscription(user.id)
            Toast.makeText(getApplication(), "Assinatura cancelada.", Toast.LENGTH_SHORT).show()
        }
    }

    fun signUp(name: String, email: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            val res = repository.signUp(name, email)
            _isProcessing.value = false
            if (res.isSuccess) {
                Toast.makeText(getApplication(), "Bem-vindo ao Figurinhas Pro!", Toast.LENGTH_SHORT).show()
                _currentScreen.value = Screen.DASHBOARD
            } else {
                Toast.makeText(getApplication(), "Erro ao criar conta", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun signIn(email: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            val res = repository.signIn(email)
            _isProcessing.value = false
            if (res.isSuccess) {
                Toast.makeText(getApplication(), "Login realizado com sucesso!", Toast.LENGTH_SHORT).show()
                _currentScreen.value = Screen.DASHBOARD
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _currentScreen.value = Screen.LANDING
        }
    }

    // Pack Management
    fun createPack(name: String, description: String = "", onComplete: (() -> Unit)? = null) {
        val user = _currentUser.value ?: return
        if (name.isBlank()) {
            Toast.makeText(getApplication(), "O nome do pacote não pode ser vazio", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            repository.createPack(user.id, name.trim(), description.trim())
            Toast.makeText(getApplication(), "Pacote \"$name\" criado!", Toast.LENGTH_SHORT).show()
            onComplete?.invoke()
        }
    }

    fun renamePack(oldName: String, newName: String, onComplete: (() -> Unit)? = null) {
        val user = _currentUser.value ?: return
        if (newName.isBlank()) {
            Toast.makeText(getApplication(), "O novo nome não pode ser vazio", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            repository.renamePack(user.id, oldName, newName.trim())
            Toast.makeText(getApplication(), "Pacote renomeado para \"$newName\"", Toast.LENGTH_SHORT).show()
            onComplete?.invoke()
        }
    }

    fun deletePack(packName: String, deleteStickers: Boolean, onComplete: (() -> Unit)? = null) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Excluindo pacote..."
            repository.deletePack(user.id, packName, deleteStickers)
            _isProcessing.value = false
            val msg = if (deleteStickers) {
                "Pacote \"$packName\" e suas figurinhas foram excluídos."
            } else {
                "Pacote \"$packName\" excluído. Figurinhas movidas para \"Geral\"."
            }
            Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
            onComplete?.invoke()
        }
    }

    fun moveStickerToPack(sticker: StickerEntity, targetPackName: String, onComplete: (() -> Unit)? = null) {
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            repository.moveStickerToPack(user.id, sticker.id, targetPackName.trim())
            Toast.makeText(getApplication(), "Figurinha movida para \"$targetPackName\"", Toast.LENGTH_SHORT).show()
            onComplete?.invoke()
        }
    }

    fun sharePackStickers(pack: PackWithStickers) {
        if (pack.stickers.isEmpty()) {
            Toast.makeText(getApplication(), "Este pacote ainda não tem figurinhas", Toast.LENGTH_SHORT).show()
            return
        }
        val firstSticker = pack.stickers.firstOrNull()
        if (firstSticker != null) {
            shareExistingStickerWhatsApp(firstSticker)
        }
    }
}
