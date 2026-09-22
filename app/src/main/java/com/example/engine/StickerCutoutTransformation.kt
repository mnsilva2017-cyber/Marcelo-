package com.example.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import coil.size.Size
import coil.transform.Transformation

/**
 * Coil Transformation customizada para remoção de fundo e criação do efeito sticker.
 *
 * Pode ser usada diretamente em ImageRequest do Coil:
 * ImageRequest.Builder(context)
 *     .data(bitmapOuUri)
 *     .transformations(StickerCutoutTransformation(outlineColor = Color.WHITE, outlineThickness = 14f))
 *     .target { drawable -> ... }
 *     .build()
 */
class StickerCutoutTransformation(
    private val outlineColor: Int = android.graphics.Color.WHITE,
    private val outlineThickness: Float = 14f,
    private val algorithm: CutoutAlgorithm = CutoutAlgorithm.ADAPTIVE_CHROMA_DEPTH
) : Transformation {

    override val cacheKey: String
        get() = "StickerCutoutTransformation_${outlineColor}_${outlineThickness}_${algorithm.name}"

    override suspend fun transform(input: Bitmap, size: Size): Bitmap {
        // 1. Executa o algoritmo de remoção de fundo
        val cutout = StickerCutoutProcessor.removeBackground(input, algorithm)

        // 2. Se houver contorno configurado, aplica a borda clássica de sticker
        return if (outlineThickness > 0f) {
            StickerEngine.applyStickerOutline(cutout, outlineColor, outlineThickness)
        } else {
            cutout
        }
    }
}
