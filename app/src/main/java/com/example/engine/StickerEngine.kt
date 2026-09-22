package com.example.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import com.example.model.StickerFontFamily
import com.example.model.StickerTextAlign
import com.example.model.StickerTextBox
import com.example.model.StickerTextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object StickerEngine {

    suspend fun loadAndResizeBitmap(context: Context, uri: Uri, targetSize: Int = 720): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()

                var inSampleSize = 1
                val maxDim = max(options.outWidth, options.outHeight)
                while (maxDim / (inSampleSize * 2) >= targetSize) {
                    inSampleSize *= 2
                }

                val decodeStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                val actualOptions = BitmapFactory.Options().apply {
                    this.inSampleSize = inSampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val original = BitmapFactory.decodeStream(decodeStream, null, actualOptions)
                decodeStream.close()
                original
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    /**
     * Automatic person cutout segmentation.
     * Delegates to the dedicated StickerCutoutProcessor using the chosen algorithm.
     */
    suspend fun autoCutoutPerson(
        source: Bitmap,
        algorithm: CutoutAlgorithm = CutoutAlgorithm.ADAPTIVE_CHROMA_DEPTH,
        sensitivity: Float = 1.0f
    ): Bitmap {
        return StickerCutoutProcessor.removeBackground(source, algorithm, sensitivity)
    }

    /**
     * Create customizable sticker outline around transparent cutout bitmap.
     * Essential for classic WhatsApp sticker aesthetic!
     * Supports:
     * - "SOLID": Classic crisp opaque WhatsApp sticker border
     * - "GLOW": Soft vibrant halo around the subject
     * - "DOUBLE": Double outline (custom color interior + crisp classic white outer border)
     * - "SHADOW": 3D Die-cut sticker drop shadow + border
     */
    fun applyStickerOutline(
        source: Bitmap,
        outlineColor: Int = Color.WHITE,
        outlineThickness: Float = 14f,
        outlineStyle: String = "SOLID"
    ): Bitmap {
        if (outlineThickness <= 0f || outlineColor == Color.TRANSPARENT) return source

        val width = source.width
        val height = source.height
        val t = outlineThickness.toInt().coerceAtLeast(1)
        val pad = if (outlineStyle == "DOUBLE") (t * 2.2f).toInt() + 12 else (t * 1.5f).toInt() + 8
        val outWidth = width + pad * 2
        val outHeight = height + pad * 2

        val output = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Generate solid color silhouette from source
        val silhouette = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val silCanvas = Canvas(silhouette)
        val silPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = PorterDuffColorFilter(outlineColor, PorterDuff.Mode.SRC_IN)
        }
        silCanvas.drawBitmap(source, 0f, 0f, silPaint)

        when (outlineStyle) {
            "GLOW" -> {
                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = outlineColor
                    maskFilter = BlurMaskFilter(outlineThickness * 1.2f, BlurMaskFilter.Blur.NORMAL)
                }
                val alphaMask = source.extractAlpha(glowPaint, null)
                canvas.drawBitmap(alphaMask, pad.toFloat(), pad.toFloat(), glowPaint)
                alphaMask.recycle()
                drawDilatedSilhouette(canvas, silhouette, pad, (t * 0.5f).toInt().coerceAtLeast(1))
            }
            "SHADOW" -> {
                val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.argb(120, 0, 0, 0)
                    maskFilter = BlurMaskFilter(t * 0.8f, BlurMaskFilter.Blur.NORMAL)
                }
                val shadowAlpha = source.extractAlpha(shadowPaint, null)
                canvas.drawBitmap(shadowAlpha, pad.toFloat() + 4f, pad.toFloat() + 7f, shadowPaint)
                shadowAlpha.recycle()
                drawDilatedSilhouette(canvas, silhouette, pad, t)
            }
            "DOUBLE" -> {
                val outerSil = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val outerSilCanvas = Canvas(outerSil)
                outerSilCanvas.drawBitmap(source, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
                })
                val outerT = (t * 1.6f).toInt()
                drawDilatedSilhouette(canvas, outerSil, pad, outerT)
                outerSil.recycle()
                drawDilatedSilhouette(canvas, silhouette, pad, t)
            }
            else -> {
                // Classic WhatsApp Solid Border
                drawDilatedSilhouette(canvas, silhouette, pad, t)
            }
        }

        // Draw crisp original subject over outline
        canvas.drawBitmap(source, pad.toFloat(), pad.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        silhouette.recycle()

        return output
    }

    private fun drawDilatedSilhouette(canvas: Canvas, silhouette: Bitmap, pad: Int, radius: Int) {
        val numAngles = when {
            radius <= 4 -> 16
            radius <= 10 -> 24
            radius <= 18 -> 32
            else -> 48
        }
        val stepR = if (radius <= 6) 1 else 2
        canvas.drawBitmap(silhouette, pad.toFloat(), pad.toFloat(), null)

        var r = 1
        while (r <= radius) {
            for (i in 0 until numAngles) {
                val angleRad = (i * 2.0 * Math.PI) / numAngles
                val dx = (Math.cos(angleRad) * r).toFloat()
                val dy = (Math.sin(angleRad) * r).toFloat()
                canvas.drawBitmap(silhouette, pad + dx, pad + dy, null)
            }
            r += stepR
        }
        for (i in 0 until numAngles) {
            val angleRad = (i * 2.0 * Math.PI) / numAngles
            val dx = (Math.cos(angleRad) * radius).toFloat()
            val dy = (Math.sin(angleRad) * radius).toFloat()
            canvas.drawBitmap(silhouette, pad + dx, pad + dy, null)
        }
    }

    /**
     * Apply optional artistic filter without degrading realism unless chosen.
     */
    /**
     * Aplica filtros artísticos de cor à imagem da pessoa recortada.
     */
    fun applyFilter(source: Bitmap, filterName: String): Bitmap {
        if (filterName.equals("NONE", ignoreCase = true) || filterName.equals("PADRÃO", ignoreCase = true)) return source

        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (filterName.uppercase()) {
            "GRAYSCALE", "PRETO_BRANCO", "PB" -> {
                // Classic crisp Black & White / Grayscale with balanced tonal range
                val matrix = ColorMatrix().apply {
                    setSaturation(0f)
                    val contrast = ColorMatrix(
                        floatArrayOf(
                            1.15f, 0f, 0f, 0f, -10f,
                            0f, 1.15f, 0f, 0f, -10f,
                            0f, 0f, 1.15f, 0f, -10f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                    postConcat(contrast)
                }
                paint.colorFilter = ColorMatrixColorFilter(matrix)
                canvas.drawBitmap(source, 0f, 0f, paint)
            }
            "SEPIA" -> {
                // Classic rich warm Sepia tone
                val sepia = ColorMatrix(
                    floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                paint.colorFilter = ColorMatrixColorFilter(sepia)
                canvas.drawBitmap(source, 0f, 0f, paint)
            }
            "COMIC_BOOK", "COMIC", "QUADRINHOS" -> {
                // Stylized Comic Book / Pop-Art graphic novel print effect
                return applyComicBookFilter(source)
            }
            "VINTAGE" -> {
                val vintage = ColorMatrix(
                    floatArrayOf(
                        0.95f, 0.05f, 0f, 0f, 15f,
                        0f, 0.85f, 0.05f, 0f, 10f,
                        0f, 0f, 0.65f, 0f, -5f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                paint.colorFilter = ColorMatrixColorFilter(vintage)
                canvas.drawBitmap(source, 0f, 0f, paint)
            }
            "CONTRASTE_ALTO", "HIGH_CONTRAST" -> {
                // High contrast filter: scale > 1 and negative offset to keep midtones punchy
                val highContrast = ColorMatrix(
                    floatArrayOf(
                        1.7f, 0f, 0f, 0f, -50f,
                        0f, 1.7f, 0f, 0f, -50f,
                        0f, 0f, 1.7f, 0f, -50f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                paint.colorFilter = ColorMatrixColorFilter(highContrast)
                canvas.drawBitmap(source, 0f, 0f, paint)
            }
            "CINEMATICO", "CINEMATIC" -> {
                // Dramatic cool shadows, warm highlights (teal & orange)
                val cinematic = ColorMatrix(
                    floatArrayOf(
                        1.2f, 0f, 0f, 0f, 10f,
                        0f, 1.1f, 0f, 0f, 0f,
                        0f, 0f, 1.4f, 0f, 20f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                paint.colorFilter = ColorMatrixColorFilter(cinematic)
                canvas.drawBitmap(source, 0f, 0f, paint)
            }
            "VIBRANTE", "VIBRANT" -> {
                // High saturation and color pop
                val satMatrix = ColorMatrix().apply { setSaturation(1.7f) }
                paint.colorFilter = ColorMatrixColorFilter(satMatrix)
                canvas.drawBitmap(source, 0f, 0f, paint)
            }
            "BRILHO", "BRIGHT" -> {
                val brightness = ColorMatrix(
                    floatArrayOf(
                        1.2f, 0f, 0f, 0f, 25f,
                        0f, 1.2f, 0f, 0f, 25f,
                        0f, 0f, 1.2f, 0f, 25f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                paint.colorFilter = ColorMatrixColorFilter(brightness)
                canvas.drawBitmap(source, 0f, 0f, paint)
            }
            "NEON", "NEON_GLOW" -> {
                val neon = ColorMatrix(
                    floatArrayOf(
                        1.5f, 0f, 0f, 0f, 10f,
                        0f, 1.3f, 0f, 0f, 30f,
                        0f, 0f, 1.8f, 0f, 50f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                paint.colorFilter = ColorMatrixColorFilter(neon)
                canvas.drawBitmap(source, 0f, 0f, paint)
            }
            "GLITCH" -> {
                canvas.drawBitmap(source, -6f, 0f, Paint().apply {
                    colorFilter = ColorMatrixColorFilter(
                        floatArrayOf(
                            1f, 0f, 0f, 0f, 0f,
                            0f, 0f, 0f, 0f, 0f,
                            0f, 0f, 0f, 0f, 0f,
                            0f, 0f, 0f, 0.7f, 0f
                        )
                    )
                })
                canvas.drawBitmap(source, 6f, 0f, Paint().apply {
                    colorFilter = ColorMatrixColorFilter(
                        floatArrayOf(
                            0f, 0f, 0f, 0f, 0f,
                            0f, 1f, 0f, 0f, 0f,
                            0f, 0f, 1f, 0f, 0f,
                            0f, 0f, 0f, 0.7f, 0f
                        )
                    )
                })
                canvas.drawBitmap(source, 0f, 0f, null)
            }
            "CARTOON" -> {
                val contrast = ColorMatrix(
                    floatArrayOf(
                        1.6f, 0f, 0f, 0f, -40f,
                        0f, 1.6f, 0f, 0f, -40f,
                        0f, 0f, 1.6f, 0f, -40f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                paint.colorFilter = ColorMatrixColorFilter(contrast)
                canvas.drawBitmap(source, 0f, 0f, paint)
            }
            "INVERT", "NEGATIVO" -> {
                val invert = ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                paint.colorFilter = ColorMatrixColorFilter(invert)
                canvas.drawBitmap(source, 0f, 0f, paint)
            }
            else -> {
                canvas.drawBitmap(source, 0f, 0f, null)
            }
        }

        return output
    }

    /**
     * Efeito Comic Book (Gibi / Quadrinhos Pop-Art):
     * Realce de saturação, contraste elevado e posterização em faixas de cor com sombras em tinta preta.
     */
    fun applyComicBookFilter(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Pass 1: Render with boosted saturation and strong contrast
        val boosted = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val boostedCanvas = Canvas(boosted)
        val boostedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            val matrix = ColorMatrix().apply {
                val contrast = ColorMatrix(
                    floatArrayOf(
                        1.5f, 0f, 0f, 0f, -25f,
                        0f, 1.5f, 0f, 0f, -25f,
                        0f, 0f, 1.5f, 0f, -25f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                val sat = ColorMatrix().apply { setSaturation(1.8f) }
                postConcat(contrast)
                postConcat(sat)
            }
            colorFilter = ColorMatrixColorFilter(matrix)
        }
        boostedCanvas.drawBitmap(source, 0f, 0f, boostedPaint)

        // Pass 2: Comic posterization & ink black shadows
        val pixels = IntArray(width * height)
        boosted.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val color = pixels[i]
            val a = (color shr 24) and 0xFF
            if (a < 20) continue

            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF

            // Perceived luminance
            val lum = (r * 299 + g * 587 + b * 114) / 1000

            if (lum < 55) {
                // Inked comic black shadow line
                pixels[i] = (a shl 24) or (15 shl 16) or (15 shl 8) or 20
            } else {
                // Stepped 4-level color bands (comic print screen)
                val newR = ((r / 64) * 64 + 32).coerceIn(0, 255)
                val newG = ((g / 64) * 64 + 32).coerceIn(0, 255)
                val newB = ((b / 64) * 64 + 32).coerceIn(0, 255)
                pixels[i] = (a shl 24) or (newR shl 16) or (newG shl 8) or newB
            }
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        boosted.recycle()
        return output
    }

    /**
     * Renders final standard 512x512 WhatsApp Sticker PNG
     */
    fun renderFinalSticker(
        cutoutBitmap: Bitmap,
        outlineColor: Int = Color.WHITE,
        outlineThickness: Float = 14f,
        outlineStyle: String = "SOLID",
        backgroundType: String = "TRANSPARENT",
        customBgColor: Int = Color.TRANSPARENT,
        filter: String = "NONE",
        captionText: String = "",
        captionColor: Int = Color.WHITE,
        speechBalloonText: String = "",
        emojiText: String = "",
        accessory: String = "NONE",
        textBoxes: List<StickerTextBox> = emptyList()
    ): Bitmap {
        val size = 512
        val finalBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(finalBitmap)

        // 1. Background
        when (backgroundType) {
            "WHITE" -> canvas.drawColor(Color.WHITE)
            "BLACK" -> canvas.drawColor(Color.BLACK)
            "CUSTOM" -> canvas.drawColor(customBgColor)
            "GRADIENT" -> {
                val gradientPaint = Paint().apply {
                    shader = android.graphics.LinearGradient(
                        0f, 0f, size.toFloat(), size.toFloat(),
                        Color.parseColor("#6C5CE7"), Color.parseColor("#00A884"),
                        android.graphics.Shader.TileMode.CLAMP
                    )
                }
                canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), gradientPaint)
            }
            else -> {
                // Transparent: do nothing
            }
        }

        // 2. Filter on cutout
        val filtered = applyFilter(cutoutBitmap, filter)

        // 3. Outline around cutout
        val outlined = applyStickerOutline(filtered, outlineColor, outlineThickness, outlineStyle)

        // Fit into sticker canvas maintaining aspect ratio
        val scale = min((size * 0.82f) / outlined.width, (size * 0.82f) / outlined.height)
        val destWidth = (outlined.width * scale).toInt()
        val destHeight = (outlined.height * scale).toInt()
        val left = (size - destWidth) / 2
        val top = (size - destHeight) / 2 + 10

        val destRect = Rect(left, top, left + destWidth, top + destHeight)
        canvas.drawBitmap(outlined, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))

        // 4. Draw accessory prop if any
        if (accessory != "NONE") {
            drawAccessory(canvas, accessory, left.toFloat(), top.toFloat(), destWidth.toFloat(), destHeight.toFloat())
        }

        // 5. Speech balloon if any
        if (speechBalloonText.isNotBlank()) {
            drawSpeechBalloon(canvas, speechBalloonText, size)
        }

        // 6. Emoji if any
        if (emojiText.isNotBlank()) {
            val emojiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 64f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(emojiText, size * 0.85f, size * 0.25f, emojiPaint)
        }

        // 7. Custom text boxes (with fonts, styles, colors and positions)
        if (textBoxes.isNotEmpty()) {
            for (box in textBoxes) {
                if (box.text.isNotBlank()) {
                    drawTextBox(canvas, box, size)
                }
            }
        } else if (captionText.isNotBlank()) {
            // Legacy single caption fallback
            drawCaptionText(canvas, captionText, captionColor, size)
        }

        return finalBitmap
    }

    private fun drawAccessory(
        canvas: Canvas,
        accessory: String,
        bodyLeft: Float,
        bodyTop: Float,
        bodyWidth: Float,
        bodyHeight: Float
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 58f
            textAlign = Paint.Align.CENTER
        }
        val headX = bodyLeft + bodyWidth * 0.5f
        val headY = bodyTop + bodyHeight * 0.22f

        when (accessory) {
            "OCULOS", "Óculos" -> {
                canvas.drawText("🕶️", headX, headY + 15f, paint)
            }
            "CHAPEU", "Chapéu" -> {
                canvas.drawText("🎩", headX, bodyTop + 20f, paint)
            }
            "BIGODE", "Bigode" -> {
                canvas.drawText("🥸", headX, headY + 50f, paint)
            }
            "FOGO", "Fogo" -> {
                canvas.drawText("🔥", headX - 80f, bodyTop + 50f, paint)
                canvas.drawText("🔥", headX + 80f, bodyTop + 50f, paint)
            }
            "CORACOES", "Corações" -> {
                canvas.drawText("💖", headX - 70f, headY, paint)
                canvas.drawText("😍", headX + 70f, headY, paint)
            }
            "ESTRELAS", "Estrelas" -> {
                canvas.drawText("✨", headX - 80f, headY - 20f, paint)
                canvas.drawText("⭐", headX + 80f, headY - 20f, paint)
            }
            "CONFETE", "Confete" -> {
                canvas.drawText("🎉", headX, bodyTop + 30f, paint)
            }
            "CHORANDO", "Lágrimas" -> {
                canvas.drawText("😭", headX + 75f, headY + 30f, paint)
            }
            "EXPLOSAO", "Explosão" -> {
                canvas.drawText("💥", headX, bodyTop + 20f, paint)
            }
        }
    }

    private fun drawSpeechBalloon(canvas: Canvas, text: String, canvasSize: Int) {
        val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 28f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        val bubbleRect = RectF(40f, 25f, canvasSize - 40f, 95f)
        canvas.drawRoundRect(bubbleRect, 24f, 24f, bubblePaint)
        canvas.drawRoundRect(bubbleRect, 24f, 24f, strokePaint)

        // Little speech pointer
        val pointer = Path().apply {
            moveTo(canvasSize * 0.45f, 95f)
            lineTo(canvasSize * 0.48f, 118f)
            lineTo(canvasSize * 0.52f, 95f)
            close()
        }
        canvas.drawPath(pointer, bubblePaint)
        canvas.drawPath(pointer, strokePaint)

        canvas.drawText(text, canvasSize / 2f, 68f, textPaint)
    }

    private fun drawCaptionText(canvas: Canvas, text: String, textColor: Int, canvasSize: Int) {
        val textSize = when {
            text.length > 20 -> 34f
            text.length > 12 -> 42f
            else -> 48f
        }

        // Stroke paint for meme style text
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            this.textSize = textSize
            style = Paint.Style.STROKE
            strokeWidth = 8f
            strokeJoin = Paint.Join.ROUND
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            this.textSize = textSize
            style = Paint.Style.FILL
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            setShadowLayer(6f, 2f, 2f, Color.argb(160, 0, 0, 0))
        }

        val y = canvasSize - 32f
        val x = canvasSize / 2f

        canvas.drawText(text, x, y, strokePaint)
        canvas.drawText(text, x, y, fillPaint)
    }

    private fun drawTextBox(canvas: Canvas, box: StickerTextBox, canvasSize: Int) {
        val posX = box.normalizedX * canvasSize
        val posY = box.normalizedY * canvasSize

        val displayText = if (box.isUppercase) box.text.uppercase() else box.text

        // Build Typeface
        val baseTypeface = when (box.fontFamily) {
            StickerFontFamily.DEFAULT -> Typeface.DEFAULT
            StickerFontFamily.SANS_SERIF -> Typeface.SANS_SERIF
            StickerFontFamily.SERIF -> Typeface.SERIF
            StickerFontFamily.MONOSPACE -> Typeface.MONOSPACE
            StickerFontFamily.CURSIVE -> Typeface.create("cursive", Typeface.NORMAL)
        }

        val styleFlag = when {
            box.isBold && box.isItalic -> Typeface.BOLD_ITALIC
            box.isBold -> Typeface.BOLD
            box.isItalic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        val typeface = Typeface.create(baseTypeface, styleFlag)

        val align = when (box.textAlign) {
            StickerTextAlign.LEFT -> Paint.Align.LEFT
            StickerTextAlign.RIGHT -> Paint.Align.RIGHT
            StickerTextAlign.CENTER -> Paint.Align.CENTER
        }

        canvas.save()
        if (box.rotationDegrees != 0f) {
            canvas.rotate(box.rotationDegrees, posX, posY)
        }

        when (box.textStyle) {
            StickerTextStyle.BADGE_BACKGROUND -> {
                // Calculate text bounds to draw rounded background badge
                val measurePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.typeface = typeface
                    this.textSize = box.fontSize
                    this.textAlign = align
                }
                val bounds = Rect()
                measurePaint.getTextBounds(displayText, 0, displayText.length, bounds)

                val paddingX = 18f
                val paddingY = 12f
                val left = when (align) {
                    Paint.Align.LEFT -> posX - paddingX
                    Paint.Align.RIGHT -> posX - bounds.width() - paddingX
                    Paint.Align.CENTER -> posX - (bounds.width() / 2f) - paddingX
                }
                val top = posY - bounds.height() - paddingY
                val right = left + bounds.width() + (paddingX * 2)
                val bottom = posY + paddingY

                val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = box.backgroundColor
                    this.style = Paint.Style.FILL
                }
                canvas.drawRoundRect(RectF(left, top, right, bottom), 16f, 16f, badgePaint)

                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.typeface = typeface
                    this.textSize = box.fontSize
                    this.color = box.textColor
                    this.textAlign = align
                }
                canvas.drawText(displayText, posX, posY, textPaint)
            }

            StickerTextStyle.MEME_STROKE -> {
                val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.typeface = typeface
                    this.textSize = box.fontSize
                    this.color = box.strokeColor
                    this.style = Paint.Style.STROKE
                    this.strokeWidth = max(5f, box.fontSize * 0.18f)
                    this.strokeJoin = Paint.Join.ROUND
                    this.strokeCap = Paint.Cap.ROUND
                    this.textAlign = align
                }
                val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.typeface = typeface
                    this.textSize = box.fontSize
                    this.color = box.textColor
                    this.style = Paint.Style.FILL
                    this.textAlign = align
                }
                canvas.drawText(displayText, posX, posY, strokePaint)
                canvas.drawText(displayText, posX, posY, fillPaint)
            }

            StickerTextStyle.NEON_GLOW -> {
                val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.typeface = typeface
                    this.textSize = box.fontSize
                    this.color = box.textColor
                    this.style = Paint.Style.STROKE
                    this.strokeWidth = 10f
                    this.textAlign = align
                    this.setShadowLayer(16f, 0f, 0f, box.textColor)
                }
                val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.typeface = typeface
                    this.textSize = box.fontSize
                    this.color = Color.WHITE
                    this.style = Paint.Style.FILL
                    this.textAlign = align
                }
                canvas.drawText(displayText, posX, posY, glowPaint)
                canvas.drawText(displayText, posX, posY, corePaint)
            }

            StickerTextStyle.MINIMAL_SHADOW -> {
                val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.typeface = typeface
                    this.textSize = box.fontSize
                    this.color = box.textColor
                    this.style = Paint.Style.FILL
                    this.textAlign = align
                    this.setShadowLayer(8f, 4f, 4f, Color.argb(180, 0, 0, 0))
                }
                canvas.drawText(displayText, posX, posY, shadowPaint)
            }

            StickerTextStyle.SOLID -> {
                val solidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.typeface = typeface
                    this.textSize = box.fontSize
                    this.color = box.textColor
                    this.style = Paint.Style.FILL
                    this.textAlign = align
                }
                canvas.drawText(displayText, posX, posY, solidPaint)
            }
        }

        canvas.restore()
    }

    suspend fun saveBitmapToFile(context: Context, bitmap: Bitmap, fileName: String): File =
        withContext(Dispatchers.IO) {
            val stickersDir = File(context.filesDir, "stickers").apply { if (!exists()) mkdirs() }
            val file = File(stickersDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file
        }

    /**
     * Formats and exports the sticker bitmap as a transparent WebP file.
     * Complies with the WhatsApp Sticker specification:
     * - Exactly 512x512 pixels
     * - Full alpha transparency channel
     * - Lossless WebP compression
     */
    suspend fun saveBitmapAsWebpFile(
        context: Context,
        bitmap: Bitmap,
        fileName: String = "sticker_${System.currentTimeMillis()}.webp"
    ): File = withContext(Dispatchers.IO) {
        val stickersDir = File(context.filesDir, "stickers").apply { if (!exists()) mkdirs() }
        val cleanName = if (fileName.endsWith(".webp", ignoreCase = true)) fileName else "$fileName.webp"
        val file = File(stickersDir, cleanName)

        // WhatsApp stickers MUST be exactly 512x512
        val finalStickerBitmap = if (bitmap.width == 512 && bitmap.height == 512) {
            bitmap
        } else {
            val scaled = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(scaled)
            val scale = min(512f / bitmap.width, 512f / bitmap.height)
            val destW = (bitmap.width * scale).toInt()
            val destH = (bitmap.height * scale).toInt()
            val left = (512 - destW) / 2
            val top = (512 - destH) / 2
            canvas.drawBitmap(bitmap, null, Rect(left, top, left + destW, top + destH), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
            scaled
        }

        FileOutputStream(file).use { out ->
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            finalStickerBitmap.compress(format, 100, out)
        }

        file
    }
}
