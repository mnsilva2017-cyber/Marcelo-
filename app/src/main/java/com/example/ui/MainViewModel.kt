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
    val selectedTextBoxId: String? = null
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
        viewModelScope.launch {
            _isProcessing.value = true
            _processingMessage.value = "Carregando foto com alta definição..."
            val bmp = StickerEngine.loadAndResizeBitmap(getApplication(), uri)
            _isProcessing.value = false
            if (bmp != null) {
                rawPhotoBitmap = bmp
                startCutoutProcess(bmp)
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
        _editorState.value = _editorState.value.copy(captionText = text, captionColor = color)
        updateStickerPreview()
    }

    fun updateOutline(color: Int, thickness: Float, style: String = _editorState.value.outlineStyle) {
        _editorState.value = _editorState.value.copy(
            outlineColor = color,
            outlineThickness = thickness,
            outlineStyle = style
        )
        updateStickerPreview()
    }

    fun updateBackground(type: String, customColor: Int = Color.TRANSPARENT) {
        _editorState.value = _editorState.value.copy(backgroundType = type, customBgColor = customColor)
        updateStickerPreview()
    }

    fun updateFilter(filter: String) {
        _editorState.value = _editorState.value.copy(filter = filter)
        updateStickerPreview()
    }

    fun updateAccessory(accessory: String) {
        _editorState.value = _editorState.value.copy(accessory = accessory)
        updateStickerPreview()
    }

    fun updateSpeechBalloon(text: String) {
        _editorState.value = _editorState.value.copy(speechBalloonText = text)
        updateStickerPreview()
    }

    fun updateEmoji(emoji: String) {
        _editorState.value = _editorState.value.copy(emojiText = emoji)
        updateStickerPreview()
    }

    fun updateMemeCategory(category: String, phrase: String = "") {
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
    fun addTextBox(
        initialText: String = "NOVO TEXTO",
        color: Int = Color.WHITE,
        style: StickerTextStyle = StickerTextStyle.MEME_STROKE,
        font: StickerFontFamily = StickerFontFamily.DEFAULT
    ) {
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
        val updatedList = _editorState.value.textBoxes.filter { it.id != selectedId }
        _editorState.value = _editorState.value.copy(
            textBoxes = updatedList,
            selectedTextBoxId = updatedList.firstOrNull()?.id
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
            textBoxes = state.textBoxes
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
