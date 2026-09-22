package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
 * Overlay interativo que permite arrastar, selecionar e posicionar caixas de texto sobre o canvas da figurinha
 */
@Composable
fun InteractiveTextBoxesOverlay(
    textBoxes: List<StickerTextBox>,
    selectedId: String?,
    onSelectTextBox: (String) -> Unit,
    onMoveTextBox: (String, Float, Float) -> Unit,
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

            Box(
                modifier = Modifier
                    .offset {
                        // Offset aproximado para centralizar a caixa
                        IntOffset(posX.roundToInt() - 100, posY.roundToInt() - 25)
                    }
                    .rotate(box.rotationDegrees)
                    .pointerInput(box.id) {
                        detectDragGestures(
                            onDragStart = { onSelectTextBox(box.id) },
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
                            Modifier.border(
                                BorderStroke(1.5.dp, WhatsAppGreenLight),
                                RoundedCornerShape(8.dp)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
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
                        fontSize = (box.fontSize * 0.45f).sp, // Escala para o tamanho visual de tela
                        fontWeight = if (box.isBold) FontWeight.Black else FontWeight.Normal,
                        fontStyle = if (box.isItalic) FontStyle.Italic else FontStyle.Normal,
                        fontFamily = box.fontFamily.fontFamily,
                        textAlign = textAlignment
                    )
                }
            }
        }
    }
}
