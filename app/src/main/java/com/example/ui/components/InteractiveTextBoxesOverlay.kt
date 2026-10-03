package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StickerFontFamily
import com.example.model.StickerTextAlign
import com.example.model.StickerTextBox
import com.example.model.StickerTextStyle
import com.example.ui.theme.WhatsAppGreenLight
import kotlin.math.roundToInt

/**
 * Overlay interativo que permite arrastar, selecionar, posicionar e gerenciar
 * caixas de texto sobre o canvas da figurinha.
 */
@Composable
fun InteractiveTextBoxesOverlay(
    textBoxes: List<StickerTextBox>,
    selectedId: String?,
    onSelectTextBox: (String) -> Unit,
    onDragStart: ((String) -> Unit)? = null,
    onMoveTextBox: (String, Float, Float) -> Unit,
    onDragEnd: (() -> Unit)? = null,
    onDeleteTextBox: ((String) -> Unit)? = null,
    onDuplicateTextBox: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val parentWidthPx = constraints.maxWidth.toFloat()
        val parentHeightPx = constraints.maxHeight.toFloat()

        if (parentWidthPx <= 0f || parentHeightPx <= 0f) return@BoxWithConstraints

        textBoxes.forEach { box ->
            val isSelected = box.id == selectedId

            // Posição central em pixels dentro do canvas
            val posX = (box.normalizedX * parentWidthPx)
            val posY = (box.normalizedY * parentHeightPx)

            val displayText = if (box.isUppercase) box.text.uppercase() else box.text

            val textAlignment = when (box.textAlign) {
                StickerTextAlign.LEFT -> TextAlign.Start
                StickerTextAlign.RIGHT -> TextAlign.End
                StickerTextAlign.CENTER -> TextAlign.Center
            }

            var boxWidthPx by remember { mutableIntStateOf(160) }
            var boxHeightPx by remember { mutableIntStateOf(44) }

            Box(
                modifier = Modifier
                    .offset {
                        // Centralizar dinamicamente a caixa em (posX, posY)
                        IntOffset(
                            (posX - boxWidthPx / 2f).roundToInt(),
                            (posY - boxHeightPx / 2f).roundToInt()
                        )
                    }
                    .onSizeChanged { size ->
                        boxWidthPx = size.width
                        boxHeightPx = size.height
                    }
                    .rotate(box.rotationDegrees)
                    .pointerInput(box.id) {
                        detectDragGestures(
                            onDragStart = {
                                onSelectTextBox(box.id)
                                onDragStart?.invoke(box.id)
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
                                val newX = (box.normalizedX + deltaNormX).coerceIn(0.1f, 0.9f)
                                val newY = (box.normalizedY + deltaNormY).coerceIn(0.08f, 0.92f)
                                onMoveTextBox(box.id, newX, newY)
                            }
                        )
                    }
                    .clickable { onSelectTextBox(box.id) }
                    .then(
                        if (isSelected) {
                            Modifier
                                .border(
                                    BorderStroke(1.5.dp, WhatsAppGreenLight),
                                    RoundedCornerShape(10.dp)
                                )
                                .background(
                                    Color.Black.copy(alpha = 0.25f),
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        } else {
                            Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        }
                    )
                    .testTag("text_box_${box.id}")
            ) {
                val boxBgColor = when (box.textStyle) {
                    StickerTextStyle.BADGE_BACKGROUND -> Color(box.backgroundColor)
                    else -> Color.Transparent
                }

                Box(
                    modifier = Modifier
                        .background(boxBgColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayText,
                        color = Color(box.textColor),
                        fontSize = (box.fontSize * 0.45f).coerceAtLeast(10f).sp, // Escala para o tamanho visual de tela
                        fontWeight = if (box.isBold) FontWeight.Black else FontWeight.Normal,
                        fontStyle = if (box.isItalic) FontStyle.Italic else FontStyle.Normal,
                        fontFamily = box.fontFamily.fontFamily,
                        textAlign = textAlignment
                    )
                }

                // Handles rápidos quando selecionado (Excluir e Duplicar)
                if (isSelected) {
                    if (onDeleteTextBox != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 6.dp, y = (-6).dp)
                                .size(20.dp)
                                .background(Color.Red, CircleShape)
                                .clickable { onDeleteTextBox(box.id) }
                                .testTag("btn_delete_textbox_handle_${box.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Excluir texto",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    if (onDuplicateTextBox != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = (-6).dp, y = (-6).dp)
                                .size(20.dp)
                                .background(Color(0xFF3B82F6), CircleShape)
                                .clickable { onDuplicateTextBox(box.id) }
                                .testTag("btn_duplicate_textbox_handle_${box.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Duplicar texto",
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
