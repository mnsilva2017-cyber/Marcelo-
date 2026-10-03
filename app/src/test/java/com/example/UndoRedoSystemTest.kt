package com.example

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.engine.StickerEngine
import com.example.model.StickerFontFamily
import com.example.ui.MainViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UndoRedoSystemTest {

    private lateinit var application: Application
    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        viewModel = MainViewModel(application)
    }

    @Test
    fun `test sticker engine cropping square and trimming`() {
        // Create a non-square bitmap (100x200)
        val bmp = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.RED)

        val square = StickerEngine.cropSquare(bmp)
        assertEquals(100, square.width)
        assertEquals(100, square.height)

        val rotated = StickerEngine.rotateBitmap(square, 90f)
        assertEquals(100, rotated.width)
        assertEquals(100, rotated.height)

        val flipped = StickerEngine.flipHorizontal(rotated)
        assertEquals(100, flipped.width)
        assertEquals(100, flipped.height)
    }

    @Test
    fun `test undo and redo for text box additions and modifications`() {
        assertFalse(viewModel.canUndo.value)
        assertFalse(viewModel.canRedo.value)

        // Add a text box
        viewModel.addTextBox("TEXTO 1")
        assertTrue(viewModel.canUndo.value)
        assertFalse(viewModel.canRedo.value)
        assertEquals(1, viewModel.editorState.value.textBoxes.size)
        assertEquals("TEXTO 1", viewModel.editorState.value.textBoxes[0].text)

        // Add a second text box
        viewModel.addTextBox("TEXTO 2")
        assertEquals(2, viewModel.editorState.value.textBoxes.size)

        // Perform undo
        val undone = viewModel.undo()
        assertTrue(undone)
        assertEquals(1, viewModel.editorState.value.textBoxes.size)
        assertEquals("TEXTO 1", viewModel.editorState.value.textBoxes[0].text)
        assertTrue(viewModel.canRedo.value)

        // Perform redo
        val redone = viewModel.redo()
        assertTrue(redone)
        assertEquals(2, viewModel.editorState.value.textBoxes.size)
        assertEquals("TEXTO 2", viewModel.editorState.value.textBoxes[1].text)
    }

    @Test
    fun `test undo and redo for filter application`() {
        assertEquals("NONE", viewModel.editorState.value.filter)

        // Apply filter
        viewModel.updateFilter("GRAYSCALE")
        assertEquals("GRAYSCALE", viewModel.editorState.value.filter)
        assertTrue(viewModel.canUndo.value)

        // Undo filter
        viewModel.undo()
        assertEquals("NONE", viewModel.editorState.value.filter)

        // Redo filter
        viewModel.redo()
        assertEquals("GRAYSCALE", viewModel.editorState.value.filter)
    }

    @Test
    fun `test text placement drag undo tracking`() {
        viewModel.addTextBox("ARRASTE ME")
        val boxId = viewModel.editorState.value.textBoxes.first().id
        val initialY = viewModel.editorState.value.textBoxes.first().normalizedY

        // Start drag and move
        viewModel.onStartTextBoxDrag(boxId)
        viewModel.onMoveTextBox(boxId, 0.5f, 0.3f)
        viewModel.onEndTextBoxDrag()

        val movedY = viewModel.editorState.value.textBoxes.first { it.id == boxId }.normalizedY
        assertEquals(0.3f, movedY, 0.01f)

        // Undo move
        viewModel.undo()
        val revertedY = viewModel.editorState.value.textBoxes.first { it.id == boxId }.normalizedY
        assertEquals(initialY, revertedY, 0.01f)

        // Redo move
        viewModel.redo()
        val redoY = viewModel.editorState.value.textBoxes.first { it.id == boxId }.normalizedY
        assertEquals(0.3f, redoY, 0.01f)
    }

    @Test
    fun `test emoji overlay addition, scaling, rotation, and undo-redo`() {
        assertEquals(0, viewModel.editorState.value.emojiItems.size)

        // Add first emoji
        viewModel.addEmojiItem("🔥")
        assertEquals(1, viewModel.editorState.value.emojiItems.size)
        val emoji1 = viewModel.editorState.value.emojiItems[0]
        assertEquals("🔥", emoji1.emoji)
        assertEquals(1.0f, emoji1.scale)
        assertEquals(0f, emoji1.rotationDegrees)
        assertTrue(viewModel.canUndo.value)

        // Scale emoji
        viewModel.updateEmojiScale(emoji1.id, 1.8f)
        val scaled = viewModel.editorState.value.emojiItems.first { it.id == emoji1.id }
        assertEquals(1.8f, scaled.scale)

        // Rotate emoji
        viewModel.updateEmojiRotation(emoji1.id, 45f)
        val rotated = viewModel.editorState.value.emojiItems.first { it.id == emoji1.id }
        assertEquals(45f, rotated.rotationDegrees)

        // Flip emoji
        viewModel.selectEmojiItem(emoji1.id)
        viewModel.flipSelectedEmoji()
        val flipped = viewModel.editorState.value.emojiItems.first { it.id == emoji1.id }
        assertTrue(flipped.isFlipped)

        // Duplicate emoji
        viewModel.duplicateEmoji(emoji1.id)
        assertEquals(2, viewModel.editorState.value.emojiItems.size)
        assertEquals("🔥", viewModel.editorState.value.emojiItems[1].emoji)

        // Undo duplication
        viewModel.undo()
        assertEquals(1, viewModel.editorState.value.emojiItems.size)

        // Redo duplication
        viewModel.redo()
        assertEquals(2, viewModel.editorState.value.emojiItems.size)

        // Remove emoji
        viewModel.removeSelectedEmoji()
        assertEquals(1, viewModel.editorState.value.emojiItems.size)

        // Undo deletion
        viewModel.undo()
        assertEquals(2, viewModel.editorState.value.emojiItems.size)
    }

    @Test
    fun `test customizable text layers font color size and meme template`() {
        // Create Meme Template (Top + Bottom)
        viewModel.addMemeTemplateTextBoxes("TOPO DO MEME", "BASE DO MEME")
        assertEquals(2, viewModel.editorState.value.textBoxes.size)
        val topBox = viewModel.editorState.value.textBoxes[0]
        val bottomBox = viewModel.editorState.value.textBoxes[1]

        assertEquals("TOPO DO MEME", topBox.text)
        assertEquals("BASE DO MEME", bottomBox.text)
        assertEquals(StickerFontFamily.IMPACT_MEME, topBox.fontFamily)
        assertEquals(42f, topBox.fontSize)

        // Customize font of top box to Comic
        viewModel.selectTextBox(topBox.id)
        viewModel.updateSelectedTextBox { it.copy(fontFamily = StickerFontFamily.COMIC) }
        val updatedTop = viewModel.editorState.value.textBoxes.first { it.id == topBox.id }
        assertEquals(StickerFontFamily.COMIC, updatedTop.fontFamily)

        // Customize color
        viewModel.updateSelectedTextBox { it.copy(textColor = Color.YELLOW, strokeColor = Color.BLACK) }
        val coloredTop = viewModel.editorState.value.textBoxes.first { it.id == topBox.id }
        assertEquals(Color.YELLOW, coloredTop.textColor)
        assertEquals(Color.BLACK, coloredTop.strokeColor)

        // Customize font size
        viewModel.updateSelectedTextBox { it.copy(fontSize = 52f) }
        val resizedTop = viewModel.editorState.value.textBoxes.first { it.id == topBox.id }
        assertEquals(52f, resizedTop.fontSize)

        // Customize rotation
        viewModel.updateTextBoxRotation(topBox.id, 15f)
        val rotatedTop = viewModel.editorState.value.textBoxes.first { it.id == topBox.id }
        assertEquals(15f, rotatedTop.rotationDegrees)

        // Duplicate layer
        viewModel.duplicateTextBox(topBox.id)
        assertEquals(3, viewModel.editorState.value.textBoxes.size)

        // Undo duplication
        viewModel.undo()
        assertEquals(2, viewModel.editorState.value.textBoxes.size)
    }

    @Test
    fun `test post-capture image processing layer filters black and white sepia and brightness`() {
        val original = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        original.eraseColor(Color.rgb(200, 100, 50))

        // Neutral returns the same bitmap without allocating redundant output
        val neutral = StickerEngine.processCapturedImage(original, filterId = "NONE", brightnessOffset = 0f)
        assertEquals(original, neutral)

        // Black and white (Grayscale)
        val bw = StickerEngine.applyBlackAndWhite(original)
        assertNotNull(bw)
        assertEquals(100, bw.width)
        assertEquals(100, bw.height)

        // Sepia
        val sepia = StickerEngine.applySepia(original)
        assertNotNull(sepia)
        assertEquals(100, sepia.width)
        assertEquals(100, sepia.height)

        // Brightness adjustment
        val brightened = StickerEngine.adjustBrightness(original, brightnessOffset = 40f)
        assertNotNull(brightened)
        assertEquals(100, brightened.width)
        assertEquals(100, brightened.height)

        // Combined filter + brightness
        val combined = StickerEngine.processCapturedImage(
            source = original,
            filterId = "GRAYSCALE",
            brightnessOffset = 25f,
            contrastFactor = 1.2f
        )
        assertNotNull(combined)
        assertEquals(100, combined.width)
        assertEquals(100, combined.height)
    }
}
