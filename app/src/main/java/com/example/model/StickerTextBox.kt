package com.example.model

import android.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import java.util.UUID

/**
 * Famílias de fontes disponíveis no editor de texto
 */
enum class StickerFontFamily(val displayName: String, val fontFamily: FontFamily) {
    DEFAULT("Padrão", FontFamily.Default),
    SANS_SERIF("Sans-Serif", FontFamily.SansSerif),
    SERIF("Serif Clássico", FontFamily.Serif),
    MONOSPACE("Monospace (Código)", FontFamily.Monospace),
    CURSIVE("Cursiva Elegante", FontFamily.Cursive)
}

/**
 * Estilos visuais de renderização da caixa de texto
 */
enum class StickerTextStyle(val displayName: String) {
    MEME_STROKE("Borda Meme (Clássico)"),
    SOLID("Texto Sólido"),
    BADGE_BACKGROUND("Fundo Tarja / Tag"),
    NEON_GLOW("Brilho Neon"),
    MINIMAL_SHADOW("Sombra Suave")
}

/**
 * Alinhamento horizontal do texto dentro da caixa
 */
enum class StickerTextAlign(val displayName: String) {
    CENTER("Centro"),
    LEFT("Esquerda"),
    RIGHT("Direita")
}

/**
 * Modelo de dados para representar uma caixa de texto independente sobre a figurinha
 */
data class StickerTextBox(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "SEU TEXTO AQUI",
    val textColor: Int = Color.WHITE,
    val backgroundColor: Int = Color.argb(190, 0, 0, 0),
    val strokeColor: Int = Color.BLACK,
    val fontSize: Float = 36f, // Sp / Pt relativo ao tamanho 512
    val fontFamily: StickerFontFamily = StickerFontFamily.DEFAULT,
    val textStyle: StickerTextStyle = StickerTextStyle.MEME_STROKE,
    val isBold: Boolean = true,
    val isItalic: Boolean = false,
    val isUppercase: Boolean = true,
    val textAlign: StickerTextAlign = StickerTextAlign.CENTER,
    // Posição normalizada (0.0f a 1.0f) relativa à tela do sticker (512x512)
    val normalizedX: Float = 0.5f,
    val normalizedY: Float = 0.85f,
    val rotationDegrees: Float = 0f
)
