package com.example

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.engine.StickerEngine
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
}
