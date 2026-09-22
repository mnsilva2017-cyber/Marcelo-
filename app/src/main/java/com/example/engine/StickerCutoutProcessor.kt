package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Algoritmos disponíveis para segmentação e remoção de fundo
 */
enum class CutoutAlgorithm(val displayName: String, val description: String) {
    ADAPTIVE_CHROMA_DEPTH(
        "Automático Inteligente",
        "Combina tom de pele, gradiente de profundidade focal e amostragem de bordas"
    ),
    COLOR_DISTANCE(
        "Remoção por Cor de Fundo",
        "Detecta a paleta dominante nas bordas e remove pixels semelhantes"
    ),
    PORTRAIT_FOCUS(
        "Foco em Retrato/Selfie",
        "Prioriza área oval central de rostos e expressões com bordas suavizadas"
    )
}

/**
 * Motor dedicado de processamento digital de imagem para remoção de fundo e criação do efeito sticker.
 */
object StickerCutoutProcessor {

    /**
     * Remove o plano de fundo de uma imagem bitmap retornando um novo bitmap com canal alfa (transparente).
     */
    suspend fun removeBackground(
        source: Bitmap,
        algorithm: CutoutAlgorithm = CutoutAlgorithm.ADAPTIVE_CHROMA_DEPTH,
        sensitivity: Float = 1.0f // 0.5f (menos agressivo) a 1.5f (mais agressivo)
    ): Bitmap = withContext(Dispatchers.Default) {
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val mask = when (algorithm) {
            CutoutAlgorithm.ADAPTIVE_CHROMA_DEPTH -> processAdaptiveChromaDepth(pixels, width, height, sensitivity)
            CutoutAlgorithm.COLOR_DISTANCE -> processColorDistance(pixels, width, height, sensitivity)
            CutoutAlgorithm.PORTRAIT_FOCUS -> processPortraitFocus(pixels, width, height, sensitivity)
        }

        // Suavização das bordas (anti-aliasing / feathering)
        val outPixels = IntArray(width * height)
        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                val idx = rowOffset + x
                val mVal = mask[idx].toInt() and 0xFF

                if (mVal > 0) {
                    var neighborCount = 0
                    for (ny in max(0, y - 1)..min(height - 1, y + 1)) {
                        for (nx in max(0, x - 1)..min(width - 1, x + 1)) {
                            if ((mask[ny * width + nx].toInt() and 0xFF) > 0) {
                                neighborCount++
                            }
                        }
                    }
                    val edgeAlpha = if (neighborCount >= 8) 255 else (neighborCount * 255 / 9)
                    val finalAlpha = (mVal * edgeAlpha) / 255
                    val orig = pixels[idx]
                    outPixels[idx] = Color.argb(
                        finalAlpha,
                        Color.red(orig),
                        Color.green(orig),
                        Color.blue(orig)
                    )
                } else {
                    outPixels[idx] = Color.TRANSPARENT
                }
            }
        }

        output.setPixels(outPixels, 0, width, 0, 0, width, height)
        output
    }

    /**
     * Algoritmo 1: Adaptativo por croma, tom de pele e profundidade central.
     * Preserva o rosto humano com alta fidelidade e elimina cantos e fundos distantes.
     */
    private fun processAdaptiveChromaDepth(
        pixels: IntArray,
        width: Int,
        height: Int,
        sensitivity: Float
    ): ByteArray {
        val mask = ByteArray(width * height)

        // 1. Amostragem dos cantos externos para detectar as cores de fundo
        val cornerColors = extractCornerPalette(pixels, width, height)

        val centerX = width / 2f
        val centerY = height * 0.48f
        val rx = width * 0.45f
        val ry = height * 0.48f

        for (y in 0 until height) {
            val dy = (y - centerY) / ry
            val dy2 = dy * dy
            val rowOffset = y * width

            for (x in 0 until width) {
                val dx = (x - centerX) / rx
                val distFromCenter = dx * dx + dy2

                val color = pixels[rowOffset + x]
                val r = Color.red(color)
                val g = Color.green(color)
                val b = Color.blue(color)

                // Detecção de tons de pele (HSV)
                val hsv = FloatArray(3)
                Color.RGBToHSV(r, g, b, hsv)
                val hue = hsv[0]
                val sat = hsv[1]
                val valB = hsv[2]

                val isSkinTone = (hue in 0f..55f || hue in 340f..360f) && sat in 0.10f..0.90f && valB > 0.20f
                val isHairOrDark = valB < 0.18f && distFromCenter < 0.85f

                var minDiff = 255
                for (bg in cornerColors) {
                    val bgR = Color.red(bg)
                    val bgG = Color.green(bg)
                    val bgB = Color.blue(bg)
                    val diff = (abs(r - bgR) + abs(g - bgG) + abs(b - bgB)) / 3
                    if (diff < minDiff) minDiff = diff
                }

                val threshold = (32f * sensitivity).toInt()

                val keep = when {
                    // Centro e face são preservados
                    isSkinTone && distFromCenter < 0.80f -> true
                    isHairOrDark && distFromCenter < 0.80f -> true
                    distFromCenter < 0.50f && minDiff > (threshold * 0.6f) -> true
                    distFromCenter < 0.75f && minDiff > threshold -> true
                    // Fora do elipsoide da pessoa
                    distFromCenter > 1.05f -> false
                    minDiff < (threshold * 0.7f) -> false
                    else -> distFromCenter < 0.65f
                }

                mask[rowOffset + x] = if (keep) 255.toByte() else 0.toByte()
            }
        }
        return mask
    }

    /**
     * Algoritmo 2: Remoção baseada em distância de cor em relação à borda da imagem.
     */
    private fun processColorDistance(
        pixels: IntArray,
        width: Int,
        height: Int,
        sensitivity: Float
    ): ByteArray {
        val mask = ByteArray(width * height)
        val cornerColors = extractCornerPalette(pixels, width, height)
        val threshold = (38f * sensitivity).toInt()

        for (y in 0 until height) {
            val rowOffset = y * width
            for (x in 0 until width) {
                val color = pixels[rowOffset + x]
                val r = Color.red(color)
                val g = Color.green(color)
                val b = Color.blue(color)

                var minDiff = 255
                for (bg in cornerColors) {
                    val bgR = Color.red(bg)
                    val bgG = Color.green(bg)
                    val bgB = Color.blue(bg)
                    val diff = (abs(r - bgR) + abs(g - bgG) + abs(b - bgB)) / 3
                    if (diff < minDiff) minDiff = diff
                }

                mask[rowOffset + x] = if (minDiff >= threshold) 255.toByte() else 0.toByte()
            }
        }
        return mask
    }

    /**
     * Algoritmo 3: Foco em Retrato com decaimento suave para bordas.
     */
    private fun processPortraitFocus(
        pixels: IntArray,
        width: Int,
        height: Int,
        sensitivity: Float
    ): ByteArray {
        val mask = ByteArray(width * height)
        val centerX = width / 2f
        val centerY = height * 0.45f
        val radiusX = (width * 0.40f) * sensitivity
        val radiusY = (height * 0.45f) * sensitivity

        for (y in 0 until height) {
            val dy = (y - centerY) / radiusY
            val dy2 = dy * dy
            val rowOffset = y * width

            for (x in 0 until width) {
                val dx = (x - centerX) / radiusX
                val dist = sqrt((dx * dx + dy2).toDouble()).toFloat()

                val alphaVal = when {
                    dist <= 0.85f -> 255
                    dist <= 1.0f -> ((1.0f - (dist - 0.85f) / 0.15f) * 255).toInt().coerceIn(0, 255)
                    else -> 0
                }

                mask[rowOffset + x] = alphaVal.toByte()
            }
        }
        return mask
    }

    /**
     * Extrai cores dominantes das 4 bordas para identificar a cor do fundo.
     */
    private fun extractCornerPalette(pixels: IntArray, width: Int, height: Int): List<Int> {
        val list = mutableListOf<Int>()
        val margin = max(4, width / 24)

        // Topo e base
        for (x in 0 until width step 8) {
            for (y in 0 until margin step 2) {
                list.add(pixels[y * width + x])
            }
            for (y in (height - margin) until height step 2) {
                list.add(pixels[y * width + x])
            }
        }

        // Laterais esquerda e direita
        for (y in 0 until height step 8) {
            for (x in 0 until margin step 2) {
                list.add(pixels[y * width + x])
            }
            for (x in (width - margin) until width step 2) {
                list.add(pixels[y * width + x])
            }
        }

        return list.take(64)
    }
}
