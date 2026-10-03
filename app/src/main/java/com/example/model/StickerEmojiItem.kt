package com.example.model

import java.util.UUID

/**
 * Representa um emoji sobreposto à figurinha com posicionamento interativo,
 * redimensionamento (escala) e rotação livre.
 */
data class StickerEmojiItem(
    val id: String = UUID.randomUUID().toString(),
    val emoji: String = "😂",
    // Posição central normalizada (0.0f a 1.0f) relativa ao canvas de 512x512
    val normalizedX: Float = 0.5f,
    val normalizedY: Float = 0.5f,
    // Fator de escala / redimensionamento (ex: 0.5f a 3.0f, padrão 1.0f)
    val scale: Float = 1.0f,
    // Ângulo de rotação em graus (-180° a +180°)
    val rotationDegrees: Float = 0f,
    // Espelhamento horizontal
    val isFlipped: Boolean = false
)
