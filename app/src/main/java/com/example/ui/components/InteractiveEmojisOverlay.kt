package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StickerEmojiItem
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppGreenLight
import kotlin.math.roundToInt

/**
 * Overlay interativo sobre a figurinha para manipular emojis:
 * Permite selecionar, arrastar, rotacionar e redimensionar emojis sobrepostos.
 */
@Composable
fun InteractiveEmojisOverlay(
    emojiItems: List<StickerEmojiItem>,
    selectedId: String?,
    onSelectEmoji: (String) -> Unit,
    onDragStart: ((String) -> Unit)? = null,
    onMoveEmoji: (String, Float, Float) -> Unit,
    onDragEnd: (() -> Unit)? = null,
    onTransformEmoji: ((String, Float, Float) -> Unit)? = null, // (id, scaleDelta, rotationDelta)
    onDeleteEmoji: ((String) -> Unit)? = null,
    onDuplicateEmoji: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val parentWidthPx = constraints.maxWidth.toFloat()
        val parentHeightPx = constraints.maxHeight.toFloat()

        if (parentWidthPx <= 0f || parentHeightPx <= 0f) return@BoxWithConstraints

        emojiItems.forEach { item ->
            val isSelected = item.id == selectedId

            // Posição central em pixels
            val posX = item.normalizedX * parentWidthPx
            val posY = item.normalizedY * parentHeightPx

            // Tamanho base da caixa do emoji (aproximadamente 64dp escalado)
            val baseSizeDp = 64.dp

            Box(
                modifier = Modifier
                    .offset {
                        // Centralizar o elemento no ponto (posX, posY)
                        val halfSizePx = (32 * item.scale * density).roundToInt()
                        IntOffset(posX.roundToInt() - halfSizePx, posY.roundToInt() - halfSizePx)
                    }
                    .rotate(item.rotationDegrees)
                    .scale(scaleX = if (item.isFlipped) -item.scale else item.scale, scaleY = item.scale)
                    .pointerInput(item.id) {
                        // Detecta arrasto de posição
                        detectDragGestures(
                            onDragStart = {
                                onSelectEmoji(item.id)
                                onDragStart?.invoke(item.id)
                            },
                            onDragEnd = {
                                onDragEnd?.invoke()
                            },
                            onDragCancel = {
                                onDragEnd?.invoke()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val deltaNormX = dragAmount.x / parentWidthPx
                                val deltaNormY = dragAmount.y / parentHeightPx
                                val newX = (item.normalizedX + deltaNormX).coerceIn(0.05f, 0.95f)
                                val newY = (item.normalizedY + deltaNormY).coerceIn(0.05f, 0.95f)
                                onMoveEmoji(item.id, newX, newY)
                            }
                        )
                    }
                    .clickable { onSelectEmoji(item.id) }
                    .then(
                        if (isSelected) {
                            Modifier
                                .border(
                                    BorderStroke(1.5.dp, WhatsAppGreenLight),
                                    RoundedCornerShape(12.dp)
                                )
                                .background(
                                    Color.Black.copy(alpha = 0.25f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(4.dp)
                        } else {
                            Modifier.padding(4.dp)
                        }
                    )
                    .testTag("emoji_overlay_${item.id}")
            ) {
                // Conteúdo do Emoji
                Text(
                    text = item.emoji,
                    fontSize = 42.sp,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Handles rápidos quando selecionado
                if (isSelected && onDeleteEmoji != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                            .size(20.dp)
                            .background(Color.Red, CircleShape)
                            .clickable { onDeleteEmoji(item.id) }
                            .testTag("btn_delete_emoji_handle_${item.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Excluir emoji",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}
