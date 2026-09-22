package com.example.model

/**
 * Filtros de cor e efeitos visuais aplicáveis às figurinhas
 */
enum class StickerFilter(
    val id: String,
    val displayName: String,
    val subtitle: String,
    val iconEmoji: String
) {
    NONE("NONE", "Original", "Cores naturais", "✨"),
    GRAYSCALE("GRAYSCALE", "Grayscale", "Preto e branco clássico", "⬛"),
    SEPIA("SEPIA", "Sepia", "Tom sépia aconchegante", "📜"),
    COMIC_BOOK("COMIC_BOOK", "Comic Book", "Estilo HQ e quadrinhos", "💥"),
    VINTAGE("VINTAGE", "Vintage", "Visual retrô nostálgico", "📷"),
    CARTOON("CARTOON", "Cartoon", "Traço contrastado pop", "🎨"),
    CINEMATIC("CINEMATIC", "Cinematic", "Cores de cinema (teal & orange)", "🎬"),
    NEON("NEON", "Neon Glow", "Cyberpunk com brilho", "💜"),
    HIGH_CONTRAST("HIGH_CONTRAST", "High Contrast", "Pretos profundos e nítidos", "⚡"),
    VIBRANT("VIBRANT", "Vibrant", "Super saturação e cor", "🌈"),
    GLITCH("GLITCH", "Glitch RGB", "Distorção cromática", "👾"),
    INVERT("INVERT", "Negative", "Inversão de cores / Raio-X", "👁️");

    companion object {
        fun fromId(id: String): StickerFilter {
            return when (id.uppercase()) {
                "NONE", "PADRÃO" -> NONE
                "GRAYSCALE", "PRETO_BRANCO", "PB" -> GRAYSCALE
                "SEPIA" -> SEPIA
                "COMIC_BOOK", "COMIC", "QUADRINHOS" -> COMIC_BOOK
                "VINTAGE" -> VINTAGE
                "CARTOON" -> CARTOON
                "CINEMATIC", "CINEMATICO" -> CINEMATIC
                "NEON", "NEON_GLOW" -> NEON
                "HIGH_CONTRAST", "CONTRASTE_ALTO" -> HIGH_CONTRAST
                "VIBRANT", "VIBRANTE" -> VIBRANT
                "GLITCH" -> GLITCH
                "INVERT", "NEGATIVO" -> INVERT
                else -> NONE
            }
        }
    }
}
