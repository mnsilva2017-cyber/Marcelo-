package com.example

import android.graphics.Bitmap
import android.graphics.Color
import coil.size.Size
import com.example.engine.CutoutAlgorithm
import com.example.engine.StickerCutoutProcessor
import com.example.engine.StickerCutoutTransformation
import com.example.engine.StickerEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class StickerCutoutTest {

    @Test
    fun `test remove background creates transparent alpha channel`() = runBlocking {
        // Create simple 100x100 test bitmap with white background and darker center
        val source = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        for (y in 0 until 100) {
            for (x in 0 until 100) {
                if (x in 30..70 && y in 20..80) {
                    source.setPixel(x, y, Color.rgb(220, 160, 130)) // Skin tone center
                } else {
                    source.setPixel(x, y, Color.WHITE) // White background
                }
            }
        }

        val cutout = StickerCutoutProcessor.removeBackground(
            source = source,
            algorithm = CutoutAlgorithm.ADAPTIVE_CHROMA_DEPTH
        )

        assertNotNull(cutout)
        assertEquals(100, cutout.width)
        assertEquals(100, cutout.height)

        // Corner background should be transparent (alpha == 0)
        val cornerAlpha = Color.alpha(cutout.getPixel(2, 2))
        assertEquals(0, cornerAlpha)

        // Center subject should be preserved (alpha > 0)
        val centerAlpha = Color.alpha(cutout.getPixel(50, 50))
        assertTrue("Center should remain opaque", centerAlpha > 200)
    }

    @Test
    fun `test coil transformation creates sticker effect with outline`() = runBlocking {
        val source = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
        for (y in 0 until 80) {
            for (x in 0 until 80) {
                source.setPixel(x, y, if (x in 25..55 && y in 25..55) Color.rgb(200, 150, 120) else Color.BLUE)
            }
        }

        val transformation = StickerCutoutTransformation(
            outlineColor = Color.WHITE,
            outlineThickness = 6f,
            algorithm = CutoutAlgorithm.PORTRAIT_FOCUS
        )

        val result = transformation.transform(source, Size.ORIGINAL)
        assertNotNull(result)
        assertEquals(80, result.width)
        assertEquals(80, result.height)
    }

    @Test
    fun `test render final sticker with custom text boxes, fonts and styles`() {
        val dummyCutout = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
        dummyCutout.eraseColor(Color.MAGENTA)

        val textBoxes = listOf(
            com.example.model.StickerTextBox(
                text = "TOP MEME",
                fontFamily = com.example.model.StickerFontFamily.SERIF,
                textStyle = com.example.model.StickerTextStyle.MEME_STROKE,
                textColor = Color.YELLOW,
                normalizedY = 0.15f
            ),
            com.example.model.StickerTextBox(
                text = "TAG STYLE",
                fontFamily = com.example.model.StickerFontFamily.MONOSPACE,
                textStyle = com.example.model.StickerTextStyle.BADGE_BACKGROUND,
                textColor = Color.WHITE,
                backgroundColor = Color.RED,
                normalizedY = 0.85f
            )
        )

        val sticker = StickerEngine.renderFinalSticker(
            cutoutBitmap = dummyCutout,
            textBoxes = textBoxes
        )

        assertNotNull(sticker)
        assertEquals(512, sticker.width)
        assertEquals(512, sticker.height)
    }

    @Test
    fun `test gemini fallback suggestions and bitmap encoding`() {
        val dummy = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val base64 = com.example.ai.GeminiMemeSuggester.bitmapToBase64(dummy, maxDim = 100)
        assertNotNull(base64)
        assertTrue(base64.isNotEmpty())

        val fallbacks = com.example.ai.GeminiMemeSuggester.getLocalFallbackSuggestions()
        assertTrue(fallbacks.isNotEmpty())
        assertTrue(fallbacks.any { it.caption.contains("TANKEI") || it.caption.contains("AVISEI") })
    }
}
