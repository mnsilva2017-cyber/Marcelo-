package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LineWeight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.StickerFilter
import com.example.model.StickerFontFamily
import com.example.model.StickerTextAlign
import com.example.model.StickerTextBox
import com.example.model.StickerTextStyle
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.CheckerboardBackground
import com.example.ui.components.InteractiveTextBoxesOverlay
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ProAccentPink
import com.example.ui.theme.ProAccentYellow
import com.example.ui.theme.ProPurple
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppGreenLight

@Composable
fun EditorScreen(
    viewModel: MainViewModel,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val editorState by viewModel.editorState.collectAsState()
    val previewBitmapFromFlow by viewModel.previewStickerBitmapFlow.collectAsState()
    val previewBitmap = previewBitmapFromFlow ?: viewModel.previewStickerBitmap ?: viewModel.cutoutBitmap
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val lastActionMessage by viewModel.lastActionMessage.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showWhatsAppExportDialog by remember { mutableStateOf(false) }
    val tabs = listOf(
        "TEXTO",
        "CORTAR",
        "MEME",
        "FILTROS",
        "CONTORNO",
        "FUNDO",
        "ADESIVOS",
        "BALÕES",
        "EMOJIS"
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
            .testTag("editor_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top action bar inside editor
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Editor de Figurinha",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Desfazer (Undo)
                IconButton(
                    onClick = { viewModel.undo() },
                    enabled = canUndo,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.White,
                        disabledContentColor = Color.Gray.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.testTag("btn_editor_undo")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Desfazer ação de edição",
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Refazer (Redo)
                IconButton(
                    onClick = { viewModel.redo() },
                    enabled = canRedo,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.White,
                        disabledContentColor = Color.Gray.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.testTag("btn_editor_redo")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Refazer ação de edição",
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Button(
                    onClick = { viewModel.saveCurrentSticker() },
                    colors = ButtonDefaults.buttonColors(containerColor = ProPurple),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("btn_save_sticker")
                ) {
                    Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Salvar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Action Feedback Toast / Pill
        AnimatedVisibility(
            visible = lastActionMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.dp, WhatsAppGreenLight.copy(alpha = 0.5f)),
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { viewModel.dismissLastActionMessage() }
                    .testTag("undo_redo_feedback_pill")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = WhatsAppGreenLight,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = lastActionMessage.orEmpty(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }

        // Live Sticker Canvas (512x512 standard format)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(2.dp, WhatsAppGreenLight.copy(alpha = 0.5f)),
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .testTag("live_sticker_canvas")
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (editorState.backgroundType == "TRANSPARENT") {
                    CheckerboardBackground(modifier = Modifier.fillMaxSize())
                }

                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap.asImageBitmap(),
                        contentDescription = "Figurinha editada",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Interactive Text Box Drag & Positioning Layer
                if (editorState.textBoxes.isNotEmpty()) {
                    InteractiveTextBoxesOverlay(
                        textBoxes = editorState.textBoxes,
                        selectedId = editorState.selectedTextBoxId,
                        onSelectTextBox = { id -> viewModel.selectTextBox(id) },
                        onDragStart = { id -> viewModel.onStartTextBoxDrag(id) },
                        onMoveTextBox = { id, newX, newY -> viewModel.onMoveTextBox(id, newX, newY) },
                        onDragEnd = { viewModel.onEndTextBoxDrag() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // WhatsApp 512x512 sticker badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "512x512 WhatsApp",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Color Filters Carousel (Grayscale, Sepia, Comic Book, etc.) directly under canvas
        QuickColorFiltersBar(
            currentFilter = editorState.filter,
            onSelectFilter = { filterId -> viewModel.updateFilter(filterId) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Big Primary Export Buttons: "EXPORTAR (WHATSAPP)" & "BAIXAR WEBP"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { showWhatsAppExportDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.3f)
                    .height(48.dp)
                    .testTag("btn_share_whatsapp")
            ) {
                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "WHATSAPP (WEBP)",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }

            Button(
                onClick = { viewModel.downloadCurrentStickerWebp() },
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("btn_download_png")
            ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "BAIXAR WEBP",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Tool Tabs (Horizontal Scrollable)
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = DarkSurface,
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = WhatsAppGreenLight
                )
            },
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            color = if (selectedTabIndex == index) WhatsAppGreenLight else Color.Gray
                        )
                    },
                    modifier = Modifier
                        .testTag("tab_tool_$title")
                        .then(if (title == "FILTROS") Modifier.testTag("tab_tool_EFEITOS") else Modifier)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Tool Settings Panels
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                when (selectedTabIndex) {
                    0 -> TextToolPanel(viewModel, editorState)
                    1 -> CropToolPanel(viewModel)
                    2 -> MemeToolPanel(viewModel, editorState.captionText)
                    3 -> FiltersToolPanel(viewModel, editorState.filter)
                    4 -> OutlineToolPanel(viewModel, editorState.outlineColor, editorState.outlineThickness, editorState.outlineStyle)
                    5 -> BackgroundToolPanel(viewModel, editorState.backgroundType)
                    6 -> AccessoriesToolPanel(viewModel, editorState.accessory)
                    7 -> SpeechBalloonPanel(viewModel, editorState.speechBalloonText)
                    8 -> EmojiToolPanel(viewModel, editorState.emojiText)
                }
            }
        }

        if (showWhatsAppExportDialog) {
            val currentBitmap = previewBitmap ?: viewModel.cutoutBitmap
            if (currentBitmap != null) {
                WhatsAppExportDialog(
                    bitmap = currentBitmap,
                    viewModel = viewModel,
                    onDismiss = { showWhatsAppExportDialog = false }
                )
            }
        }
    }
}

@Composable
fun CropToolPanel(viewModel: MainViewModel) {
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CORTAR E ENQUADRAR",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 13.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { viewModel.undo() },
                    enabled = canUndo,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.White,
                        disabledContentColor = Color.Gray.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.size(32.dp).testTag("btn_crop_panel_undo")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Desfazer corte",
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { viewModel.redo() },
                    enabled = canRedo,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.White,
                        disabledContentColor = Color.Gray.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.size(32.dp).testTag("btn_crop_panel_redo")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Refazer corte",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Text(
            text = "Ajuste a imagem para caber perfeitamente no formato 512x512 do WhatsApp.",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        // Actions Grid / Rows
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Enquadrar Quadrado 1:1
            Button(
                onClick = { viewModel.cropCutoutSquare() },
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("btn_crop_square_tool")
            ) {
                Icon(Icons.Default.Crop, contentDescription = null, tint = WhatsAppGreenLight, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Cortar 1:1", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            // Auto-Ajustar Bordas Transparentes
            Button(
                onClick = { viewModel.trimCutoutBorders() },
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .testTag("btn_trim_borders_tool")
            ) {
                Icon(Icons.Default.AspectRatio, contentDescription = null, tint = WhatsAppGreenLight, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ajustar Bordas", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Girar 90° Horário
            OutlinedButton(
                onClick = { viewModel.rotateCutout(90f) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("btn_rotate_clockwise")
            ) {
                Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Girar 90°", fontSize = 11.sp)
            }

            // Girar -90° Anti-Horário
            OutlinedButton(
                onClick = { viewModel.rotateCutout(-90f) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("btn_rotate_counter_clockwise")
            ) {
                Icon(Icons.Default.RotateLeft, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Girar -90°", fontSize = 11.sp)
            }

            // Espelhar Imagem
            OutlinedButton(
                onClick = { viewModel.flipCutoutHorizontal() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .testTag("btn_flip_horizontal")
            ) {
                Icon(Icons.Default.Flip, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Espelhar", fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun TextToolPanel(viewModel: MainViewModel, editorState: com.example.ui.EditorState) {
    val selectedBox = editorState.textBoxes.find { it.id == editorState.selectedTextBoxId }
        ?: editorState.textBoxes.firstOrNull()

    val quickPhrases = listOf(
        "KKKKKK", "EU AVISEI", "NÃO ACREDITO!", "BOM DIA",
        "BOA NOITE", "TÔ CHEGANDO", "DEPOIS EU VEJO", "MEU DEUS", "OLHA ISSO"
    )

    val textColors = listOf(
        AndroidColor.WHITE to "Branco",
        AndroidColor.YELLOW to "Amarelo",
        AndroidColor.RED to "Vermelho",
        AndroidColor.GREEN to "Verde",
        AndroidColor.CYAN to "Ciano",
        AndroidColor.MAGENTA to "Rosa",
        AndroidColor.BLACK to "Preto",
        AndroidColor.parseColor("#FFA500") to "Laranja",
        AndroidColor.parseColor("#9C27B0") to "Roxo",
        AndroidColor.parseColor("#00E676") to "Verde Neon"
    )

    Column {
        // Header: Add Text Box button and title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Caixas de Texto (${editorState.textBoxes.size})",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )

            Button(
                onClick = { viewModel.addTextBox() },
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_add_text_box")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nova Caixa", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selector for active text box if multiple exist
        if (editorState.textBoxes.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(editorState.textBoxes.size) { index ->
                    val box = editorState.textBoxes[index]
                    val isSelected = box.id == (selectedBox?.id)
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectTextBox(box.id) },
                        label = {
                            Text(
                                text = "Texto ${index + 1}: ${box.text.take(10)}${if (box.text.length > 10) "..." else ""}",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WhatsAppGreen,
                            selectedLabelColor = Color.White,
                            containerColor = DarkSurfaceElevated,
                            labelColor = Color.LightGray
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Active text content editor
        val activeText = selectedBox?.text ?: editorState.captionText
        val isGeneratingAi by viewModel.isGeneratingMemeAi.collectAsState()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = activeText,
                onValueChange = { newText ->
                    if (selectedBox != null) {
                        viewModel.updateSelectedTextBox { it.copy(text = newText) }
                    } else {
                        viewModel.addTextBox(initialText = newText)
                    }
                    viewModel.updateCaption(newText)
                },
                placeholder = { Text("Digite o texto aqui...", color = Color.Gray) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = WhatsAppGreenLight,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_caption_text")
            )

            // Quick Gemini AI suggestion trigger
            IconButton(
                onClick = { viewModel.loadGeminiMemeSuggestions() },
                enabled = !isGeneratingAi,
                modifier = Modifier
                    .size(44.dp)
                    .background(ProPurple.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .testTag("btn_text_ai_magic")
            ) {
                if (isGeneratingAi) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = WhatsAppGreenLight
                    )
                } else {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "Sugerir com Gemini AI",
                        tint = ProAccentPink
                    )
                }
            }

            if (selectedBox != null) {
                IconButton(
                    onClick = { viewModel.removeSelectedTextBox() },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFEF4444).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .testTag("btn_remove_text_box")
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Excluir caixa de texto",
                        tint = Color(0xFFEF4444)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1. FONT FAMILIES
        Text("Tipografia / Fonte:", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(StickerFontFamily.values()) { font ->
                val isSelected = selectedBox?.fontFamily == font
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) WhatsAppGreen else DarkSurfaceElevated,
                    border = BorderStroke(1.dp, if (isSelected) WhatsAppGreenLight else Color.Transparent),
                    modifier = Modifier.clickable {
                        if (selectedBox != null) {
                            viewModel.updateSelectedTextBox { it.copy(fontFamily = font) }
                        } else {
                            viewModel.addTextBox(font = font)
                        }
                    }
                ) {
                    Text(
                        text = font.displayName,
                        fontFamily = font.fontFamily,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. TEXT STYLES (Meme stroke, Solid, Badge, Neon, Shadow)
        Text("Estilo Visual do Texto:", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(StickerTextStyle.values()) { style ->
                val isSelected = selectedBox?.textStyle == style
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) ProPurple else DarkSurfaceElevated,
                    border = BorderStroke(1.dp, if (isSelected) ProAccentPink else Color.Transparent),
                    modifier = Modifier.clickable {
                        if (selectedBox != null) {
                            viewModel.updateSelectedTextBox { it.copy(textStyle = style) }
                        } else {
                            viewModel.addTextBox(style = style)
                        }
                    }
                ) {
                    Text(
                        text = style.displayName,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. COLOR PALETTE
        val currentColor = selectedBox?.textColor ?: editorState.captionColor
        Text("Cor do Texto:", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(textColors) { (colorVal, name) ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(colorVal))
                        .clickable {
                            if (selectedBox != null) {
                                viewModel.updateSelectedTextBox { it.copy(textColor = colorVal) }
                            }
                            viewModel.updateCaption(activeText, colorVal)
                        }
                        .then(
                            if (currentColor == colorVal) {
                                Modifier.border(2.5.dp, Color.White, CircleShape)
                            } else {
                                Modifier
                            }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. FORMAT CONTROLS (Bold, Italic, Uppercase, Alignments)
        Text("Formatação & Alinhamento:", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Bold button
            val isBold = selectedBox?.isBold ?: true
            IconButton(
                onClick = {
                    if (selectedBox != null) {
                        viewModel.updateSelectedTextBox { it.copy(isBold = !isBold) }
                    }
                },
                modifier = Modifier
                    .background(
                        if (isBold) WhatsAppGreen else DarkSurfaceElevated,
                        RoundedCornerShape(8.dp)
                    )
                    .size(40.dp)
            ) {
                Icon(Icons.Default.FormatBold, contentDescription = "Negrito", tint = Color.White)
            }

            // Italic button
            val isItalic = selectedBox?.isItalic ?: false
            IconButton(
                onClick = {
                    if (selectedBox != null) {
                        viewModel.updateSelectedTextBox { it.copy(isItalic = !isItalic) }
                    }
                },
                modifier = Modifier
                    .background(
                        if (isItalic) WhatsAppGreen else DarkSurfaceElevated,
                        RoundedCornerShape(8.dp)
                    )
                    .size(40.dp)
            ) {
                Icon(Icons.Default.FormatItalic, contentDescription = "Itálico", tint = Color.White)
            }

            // Uppercase toggle
            val isUpper = selectedBox?.isUppercase ?: true
            IconButton(
                onClick = {
                    if (selectedBox != null) {
                        viewModel.updateSelectedTextBox { it.copy(isUppercase = !isUpper) }
                    }
                },
                modifier = Modifier
                    .background(
                        if (isUpper) WhatsAppGreen else DarkSurfaceElevated,
                        RoundedCornerShape(8.dp)
                    )
                    .size(40.dp)
            ) {
                Icon(Icons.Default.TextFields, contentDescription = "Maiúsculas", tint = Color.White)
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Align Left
            val currentAlign = selectedBox?.textAlign ?: StickerTextAlign.CENTER
            IconButton(
                onClick = {
                    if (selectedBox != null) {
                        viewModel.updateSelectedTextBox { it.copy(textAlign = StickerTextAlign.LEFT) }
                    }
                },
                modifier = Modifier
                    .background(
                        if (currentAlign == StickerTextAlign.LEFT) WhatsAppGreen else DarkSurfaceElevated,
                        RoundedCornerShape(8.dp)
                    )
                    .size(40.dp)
            ) {
                Icon(Icons.Default.FormatAlignLeft, contentDescription = "Alinhar à Esquerda", tint = Color.White)
            }

            // Align Center
            IconButton(
                onClick = {
                    if (selectedBox != null) {
                        viewModel.updateSelectedTextBox { it.copy(textAlign = StickerTextAlign.CENTER) }
                    }
                },
                modifier = Modifier
                    .background(
                        if (currentAlign == StickerTextAlign.CENTER) WhatsAppGreen else DarkSurfaceElevated,
                        RoundedCornerShape(8.dp)
                    )
                    .size(40.dp)
            ) {
                Icon(Icons.Default.FormatAlignCenter, contentDescription = "Centralizar", tint = Color.White)
            }

            // Align Right
            IconButton(
                onClick = {
                    if (selectedBox != null) {
                        viewModel.updateSelectedTextBox { it.copy(textAlign = StickerTextAlign.RIGHT) }
                    }
                },
                modifier = Modifier
                    .background(
                        if (currentAlign == StickerTextAlign.RIGHT) WhatsAppGreen else DarkSurfaceElevated,
                        RoundedCornerShape(8.dp)
                    )
                    .size(40.dp)
            ) {
                Icon(Icons.Default.FormatAlignRight, contentDescription = "Alinhar à Direita", tint = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. FONT SIZE SLIDER
        val currentSize = selectedBox?.fontSize ?: 36f
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tamanho da Fonte:", color = Color.LightGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("${currentSize.toInt()} pt", color = WhatsAppGreenLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = currentSize,
            onValueChange = { newSize ->
                if (selectedBox != null) {
                    viewModel.updateSelectedTextBox { it.copy(fontSize = newSize) }
                }
            },
            valueRange = 18f..72f,
            colors = SliderDefaults.colors(
                thumbColor = WhatsAppGreenLight,
                activeTrackColor = WhatsAppGreen
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 6. POPULAR & GEMINI QUICK PHRASES
        val geminiList by viewModel.geminiSuggestions.collectAsState()
        val displayPhrases = if (geminiList.isNotEmpty()) {
            geminiList.map { "${it.emoji} ${it.caption}" } + quickPhrases
        } else {
            quickPhrases
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (geminiList.isNotEmpty()) "✨ Sugestões Gemini & Populares:" else "Frases Rápidas Populares:",
                color = if (geminiList.isNotEmpty()) WhatsAppGreenLight else Color.LightGray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(displayPhrases) { phraseWithEmoji ->
                val cleanPhrase = phraseWithEmoji.replace(Regex("""^[^\w\s]+\s*"""), "").trim()
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(1.dp, if (phraseWithEmoji.startsWith("✨") || geminiList.any { phraseWithEmoji.contains(it.caption) }) ProPurple.copy(alpha = 0.5f) else Color.Transparent),
                    modifier = Modifier.clickable {
                        val textToUse = if (cleanPhrase.isNotBlank()) cleanPhrase else phraseWithEmoji
                        if (selectedBox != null) {
                            viewModel.updateSelectedTextBox { it.copy(text = textToUse) }
                        } else {
                            viewModel.addTextBox(initialText = textToUse)
                        }
                        viewModel.updateCaption(textToUse)
                    }
                ) {
                    Text(
                        text = phraseWithEmoji,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MemeToolPanel(viewModel: MainViewModel, currentText: String) {
    val isGeneratingAi by viewModel.isGeneratingMemeAi.collectAsState()
    val geminiSuggestions by viewModel.geminiSuggestions.collectAsState()

    val categories = listOf(
        "😂 Engraçado" to listOf("Rindo até 2030", "Não tankei", "Socorro kkkkk", "KKKKKK"),
        "😱 Surpresa" to listOf("Passada!", "Como assim?!", "Mentira!", "MEU DEUS!"),
        "😡 Raiva" to listOf("Tô por um fio", "Nem fala comigo", "Calma respira", "EU AVISEI"),
        "😍 Amor" to listOf("Gatinho(a)", "Amei muito!", "Meu dengo", "Apaixonei"),
        "🤔 Pensando" to listOf("Será?!", "Refletindo...", "Hmm suspeito", "DEPOIS EU VEJO"),
        "🤣 Risada" to listOf("Tô chorando de rir", "Perdi tudo", "Rindo de nervoso"),
        "😎 Confiante" to listOf("O patrão tá on", "Apenas chique", "Respeita a história"),
        "😭 Triste" to listOf("Só dor e sofrimento", "Chorando no banho", "Cadê o pix?"),
        "🔥 Reação" to listOf("Habla mesmo!", "Eita atrás de eita", "Fogo no parquinho"),
        "💰 Dinheiro" to listOf("Caiu o pix!", "Pobre porém limpinho", "Bora pagar boleto"),
        "❤️ Romântico" to listOf("Te amo meu bem", "Amor da minha vida", "Meu coração todinho")
    )

    var selectedCat by remember { mutableIntStateOf(0) }

    Column {
        // --- SEÇÃO DE IA GEMINI: SUGESTÕES BASEADAS NA FOTO ---
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = ProPurple.copy(alpha = 0.25f)),
            border = BorderStroke(1.dp, ProPurple.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("gemini_meme_card")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(ProPurple),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Gemini AI: Criador de Memes",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Frases inteligentes baseadas na foto capturada",
                                color = Color.LightGray,
                                fontSize = 10.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.loadGeminiMemeSuggestions() },
                        enabled = !isGeneratingAi,
                        modifier = Modifier
                            .size(36.dp)
                            .background(DarkSurfaceElevated, CircleShape)
                            .testTag("btn_regenerate_gemini_memes")
                    ) {
                        if (isGeneratingAi) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = WhatsAppGreenLight
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Regerar memes com Gemini",
                                tint = WhatsAppGreenLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isGeneratingAi && geminiSuggestions.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = ProAccentPink
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Gemini está analisando a imagem e criando memes...",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (geminiSuggestions.isNotEmpty()) {
                    Text(
                        "Sugestões geradas para sua foto (toque para aplicar):",
                        color = Color(0xFFE2E8F0),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        geminiSuggestions.forEach { item ->
                            val isCurrent = currentText == item.caption
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isCurrent) WhatsAppGreen.copy(alpha = 0.25f) else DarkSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (isCurrent) WhatsAppGreenLight else ProPurple.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateMemeCategory(item.category, item.caption)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = item.emoji,
                                            fontSize = 18.sp,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Column {
                                            Text(
                                                text = item.caption,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = item.category,
                                                color = WhatsAppGreenLight,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isCurrent) WhatsAppGreen else ProPurple.copy(alpha = 0.4f),
                                        modifier = Modifier.padding(start = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isCurrent) "USANDO" else "USAR",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SEÇÃO DE CATEGORIAS CLÁSSICAS ---
        Text("Ou escolha por Categoria de Reação:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories.size) { idx ->
                val (catName, _) = categories[idx]
                FilterChip(
                    selected = selectedCat == idx,
                    onClick = {
                        selectedCat = idx
                        val phrase = categories[idx].second.first()
                        viewModel.updateMemeCategory(catName, phrase)
                    },
                    label = { Text(catName, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WhatsAppGreen,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Frases prontas para ${categories[selectedCat].first}:", color = Color.LightGray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            categories[selectedCat].second.forEach { phrase ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (currentText == phrase) WhatsAppGreen.copy(alpha = 0.2f) else DarkSurfaceElevated,
                    border = BorderStroke(
                        1.dp,
                        if (currentText == phrase) WhatsAppGreenLight else Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateCaption(phrase) }
                ) {
                    Text(
                        text = "“$phrase”",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OutlineToolPanel(
    viewModel: MainViewModel,
    currentColor: Int,
    currentThickness: Float,
    currentStyle: String = "SOLID"
) {
    val isNoOutline = currentThickness <= 0f || currentColor == AndroidColor.TRANSPARENT
    val isClassicWhatsApp = !isNoOutline && currentColor == AndroidColor.WHITE && currentThickness.toInt() in 12..16 && currentStyle == "SOLID"

    val outlineColors = listOf(
        AndroidColor.WHITE to "Branco WhatsApp",
        AndroidColor.BLACK to "Preto",
        AndroidColor.parseColor("#00A884") to "Verde WhatsApp",
        AndroidColor.parseColor("#25D366") to "Verde Claro",
        AndroidColor.parseColor("#FDCB6E") to "Amarelo Sol",
        AndroidColor.parseColor("#E84393") to "Rosa Neon",
        AndroidColor.parseColor("#00CEC9") to "Ciano",
        AndroidColor.parseColor("#6C5CE7") to "Roxo Pro",
        AndroidColor.parseColor("#FF5252") to "Vermelho",
        AndroidColor.parseColor("#FF7675") to "Laranja",
        AndroidColor.parseColor("#DFE6E9") to "Prata"
    )

    val thicknessPresets = listOf(
        0f to "Sem borda",
        6f to "Fina (6px)",
        14f to "Padrão WhatsApp (14px)",
        22f to "Grossa (22px)",
        30f to "Extra (30px)"
    )

    val styleOptions = listOf(
        "SOLID" to "Sólida (WhatsApp)",
        "DOUBLE" to "Dupla Borda",
        "GLOW" to "Brilho Neon",
        "SHADOW" to "Sombra 3D"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Borda do Sticker (Padrão WhatsApp)",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "Adicione a clássica borda branca recortada ou escolha cores personalizadas.",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }
            if (isClassicWhatsApp) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = WhatsAppGreen.copy(alpha = 0.25f),
                    border = BorderStroke(1.dp, WhatsAppGreenLight)
                ) {
                    Text(
                        text = "⭐ PADRÃO OFICIAL",
                        color = WhatsAppGreenLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // WhatsApp Official Preset Hero Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (isClassicWhatsApp) WhatsAppGreen.copy(alpha = 0.15f) else DarkSurfaceElevated,
            border = BorderStroke(
                1.5.dp,
                if (isClassicWhatsApp) WhatsAppGreenLight else WhatsAppGreen.copy(alpha = 0.4f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    viewModel.updateOutline(AndroidColor.WHITE, 14f, "SOLID")
                }
                .testTag("btn_preset_whatsapp_outline")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color.White, CircleShape)
                            .border(2.dp, WhatsAppGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isClassicWhatsApp) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = DarkBackground,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Borda Branca Clássica do WhatsApp",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Borda branca nítida de 14px estilo sticker oficial",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                }
                Button(
                    onClick = { viewModel.updateOutline(AndroidColor.WHITE, 14f, "SOLID") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isClassicWhatsApp) WhatsAppGreen else WhatsAppGreen.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isClassicWhatsApp) "Ativa" else "Aplicar",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Border Style Selector
        Text(
            text = "Estilo do Contorno:",
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            styleOptions.forEach { (sKey, sLabel) ->
                val isSelected = currentStyle == sKey && !isNoOutline
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) WhatsAppGreen.copy(alpha = 0.25f) else DarkSurfaceElevated,
                    border = BorderStroke(
                        1.5.dp,
                        if (isSelected) WhatsAppGreenLight else Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            val targetColor = if (currentColor == AndroidColor.TRANSPARENT) AndroidColor.WHITE else currentColor
                            val targetThickness = if (currentThickness <= 0f) 14f else currentThickness
                            viewModel.updateOutline(targetColor, targetThickness, sKey)
                        }
                        .testTag("outline_style_$sKey")
                ) {
                    Text(
                        text = sLabel,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Color.LightGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Color Palette
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cor da Borda Personalizável:",
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontSize = 12.sp
            )
            val selectedName = outlineColors.find { it.first == currentColor }?.second ?: if (isNoOutline) "Nenhuma" else "Personalizada"
            Text(
                text = selectedName,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WhatsAppGreenLight
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // "Sem Borda" option
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isNoOutline) WhatsAppGreen.copy(alpha = 0.3f) else DarkSurfaceElevated,
                    border = BorderStroke(
                        1.5.dp,
                        if (isNoOutline) WhatsAppGreenLight else Color.Transparent
                    ),
                    modifier = Modifier
                        .clickable { viewModel.updateOutline(AndroidColor.TRANSPARENT, 0f, currentStyle) }
                        .testTag("outline_color_none")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(DarkSurface, CircleShape)
                                .border(1.5.dp, Color.Gray, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Sem Borda", color = Color.LightGray, fontSize = 10.sp)
                    }
                }
            }

            // Available Colors
            items(outlineColors) { (colorVal, name) ->
                val isSelected = currentColor == colorVal && !isNoOutline
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) WhatsAppGreen.copy(alpha = 0.25f) else DarkSurfaceElevated,
                    border = BorderStroke(
                        1.5.dp,
                        if (isSelected) WhatsAppGreenLight else Color.Transparent
                    ),
                    modifier = Modifier
                        .clickable {
                            val targetThickness = if (currentThickness <= 0f) 14f else currentThickness
                            viewModel.updateOutline(colorVal, targetThickness, currentStyle)
                        }
                        .testTag("outline_color_${name.lowercase().replace(" ", "_")}")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .border(
                                    1.dp,
                                    if (colorVal == AndroidColor.WHITE) Color.LightGray else Color.Transparent,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                val checkTint = if (colorVal == AndroidColor.WHITE || colorVal == AndroidColor.parseColor("#FDCB6E") || colorVal == AndroidColor.parseColor("#DFE6E9")) Color.Black else Color.White
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = checkTint,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = name.split(" ").first(),
                            color = if (isSelected) Color.White else Color.LightGray,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Thickness Slider & Presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Espessura da Borda:",
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                fontSize = 12.sp
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = DarkSurfaceElevated
            ) {
                Text(
                    text = "${currentThickness.toInt()} px",
                    color = WhatsAppGreenLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Slider(
            value = currentThickness,
            onValueChange = {
                val targetColor = if (currentColor == AndroidColor.TRANSPARENT && it > 0f) AndroidColor.WHITE else currentColor
                viewModel.updateOutline(targetColor, it, currentStyle)
            },
            valueRange = 0f..32f,
            colors = SliderDefaults.colors(
                thumbColor = WhatsAppGreenLight,
                activeTrackColor = WhatsAppGreen,
                inactiveTrackColor = DarkSurfaceElevated
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("slider_outline_thickness")
        )

        // Quick Thickness Presets
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(thicknessPresets) { (presetValue, presetLabel) ->
                val isSelected = currentThickness.toInt() == presetValue.toInt()
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        val targetColor = if (presetValue <= 0f) AndroidColor.TRANSPARENT else if (currentColor == AndroidColor.TRANSPARENT) AndroidColor.WHITE else currentColor
                        viewModel.updateOutline(targetColor, presetValue, currentStyle)
                    },
                    label = { Text(presetLabel, fontSize = 10.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WhatsAppGreen,
                        selectedLabelColor = Color.White,
                        containerColor = DarkSurfaceElevated,
                        labelColor = Color.LightGray
                    ),
                    modifier = Modifier.testTag("chip_thickness_${presetValue.toInt()}")
                )
            }
        }
    }
}

@Composable
fun BackgroundToolPanel(viewModel: MainViewModel, currentType: String) {
    val bgTypes = listOf(
        "TRANSPARENT" to "Transparente (WhatsApp)",
        "WHITE" to "Branco",
        "BLACK" to "Preto",
        "GRADIENT" to "Gradiente Moderno"
    )

    Column {
        Text("Fundo da Figurinha:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
        Text(
            "Recomendado: Transparente para o visual clássico de sticker do WhatsApp.",
            color = WhatsAppGreenLight,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            bgTypes.forEach { (typeKey, label) ->
                FilterChip(
                    selected = currentType == typeKey,
                    onClick = { viewModel.updateBackground(typeKey) },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WhatsAppGreen,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun QuickColorFiltersBar(
    currentFilter: String,
    onSelectFilter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.ColorLens,
                    contentDescription = null,
                    tint = WhatsAppGreenLight,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Filtros de Cor:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Grayscale, Sepia, Comic Book...",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
            }
            if (currentFilter != "NONE") {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DarkSurfaceElevated,
                    modifier = Modifier.clickable { onSelectFilter("NONE") }
                ) {
                    Text(
                        text = "Resetar",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WhatsAppGreenLight,
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("btn_reset_filter")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(StickerFilter.values()) { filter ->
                val isSelected = currentFilter.equals(filter.id, ignoreCase = true) ||
                        (filter == StickerFilter.GRAYSCALE && currentFilter.equals("PRETO_BRANCO", ignoreCase = true)) ||
                        (filter == StickerFilter.COMIC_BOOK && currentFilter.equals("CARTOON", ignoreCase = true))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) WhatsAppGreen.copy(alpha = 0.25f) else DarkSurfaceElevated,
                    border = BorderStroke(
                        1.5.dp,
                        if (isSelected) WhatsAppGreenLight else Color.Transparent
                    ),
                    modifier = Modifier
                        .clickable { onSelectFilter(filter.id) }
                        .testTag("quick_filter_${filter.id.lowercase()}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(text = filter.iconEmoji, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = filter.displayName,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color.LightGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FiltersToolPanel(viewModel: MainViewModel, currentFilter: String) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Filtros de Cor da Figurinha",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "Personalize o recorte com filtros como Grayscale, Sepia, Comic Book e mais.",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }
            if (currentFilter != "NONE") {
                OutlinedButton(
                    onClick = { viewModel.updateFilter("NONE") },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, WhatsAppGreenLight.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppGreenLight),
                    modifier = Modifier.testTag("btn_reset_filter_panel")
                ) {
                    Text("Original", fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Comprehensive filter selector with descriptions and badges
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StickerFilter.values().forEach { filter ->
                val isSelected = currentFilter.equals(filter.id, ignoreCase = true) ||
                        (filter == StickerFilter.GRAYSCALE && currentFilter.equals("PRETO_BRANCO", ignoreCase = true)) ||
                        (filter == StickerFilter.COMIC_BOOK && currentFilter.equals("CARTOON", ignoreCase = true))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) WhatsAppGreen.copy(alpha = 0.2f) else DarkSurfaceElevated,
                    border = BorderStroke(
                        1.5.dp,
                        if (isSelected) WhatsAppGreenLight else Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateFilter(filter.id) }
                        .testTag("filter_chip_${filter.id.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (isSelected) WhatsAppGreen else DarkSurface,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(filter.iconEmoji, fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = filter.displayName,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = filter.subtitle,
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (isSelected) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = WhatsAppGreen,
                                modifier = Modifier.padding(start = 6.dp)
                            ) {
                                Text(
                                    text = "ATIVO",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EffectsToolPanel(viewModel: MainViewModel, currentFilter: String) {
    FiltersToolPanel(viewModel = viewModel, currentFilter = currentFilter)
}

@Composable
fun AccessoriesToolPanel(viewModel: MainViewModel, currentAcc: String) {
    val accessories = listOf(
        "NONE" to "Nenhum",
        "OCULOS" to "🕶️ Óculos",
        "CHAPEU" to "🎩 Chapéu",
        "BIGODE" to "🥸 Bigode",
        "FOGO" to "🔥 Fogo",
        "CORACOES" to "💖 Corações",
        "ESTRELAS" to "✨ Estrelas",
        "CONFETE" to "🎉 Confete",
        "CHORANDO" to "😭 Lágrimas",
        "EXPLOSAO" to "💥 Explosão"
    )

    Column {
        Text("Acessórios e Efeitos Visuais:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(accessories) { (accKey, accName) ->
                FilterChip(
                    selected = currentAcc == accKey,
                    onClick = { viewModel.updateAccessory(accKey) },
                    label = { Text(accName, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ProPurple,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }
    }
}

@Composable
fun SpeechBalloonPanel(viewModel: MainViewModel, currentBalloonText: String) {
    val suggestions = listOf("HABLA MESMO", "EITA ATRÁS DE EITA", "SOCORRO!", "CALMA RESPIRA", "O PATRÃO TÁ ON")

    Column {
        Text("Balão de Fala Comic:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = currentBalloonText,
            onValueChange = { viewModel.updateSpeechBalloon(it) },
            placeholder = { Text("Texto dentro do balão...", color = Color.Gray) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = WhatsAppGreenLight,
                unfocusedBorderColor = Color.Gray,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))
        Text("Ideias de frases de balão:", color = Color.LightGray, fontSize = 11.sp)
        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(suggestions) { sug ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceElevated,
                    modifier = Modifier.clickable { viewModel.updateSpeechBalloon(sug) }
                ) {
                    Text(
                        sug,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }

        if (currentBalloonText.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { viewModel.updateSpeechBalloon("") },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Remover balão de fala", fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun EmojiToolPanel(viewModel: MainViewModel, currentEmoji: String) {
    val emojis = listOf(
        "😂", "😱", "🔥", "🚀", "❤️", "🤡", "💸", "👀",
        "⚡", "🕶️", "👑", "🍕", "🐶", "💪", "🤫", "🎯"
    )

    Column {
        Text("Adicionar Emoji de Reação:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(emojis) { emoji ->
                Surface(
                    shape = CircleShape,
                    color = if (currentEmoji == emoji) WhatsAppGreen else DarkSurfaceElevated,
                    modifier = Modifier
                        .size(42.dp)
                        .clickable {
                            if (currentEmoji == emoji) viewModel.updateEmoji("") else viewModel.updateEmoji(emoji)
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(emoji, fontSize = 20.sp)
                    }
                }
            }
        }

        if (currentEmoji.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            TextButton(onClick = { viewModel.updateEmoji("") }) {
                Text("Remover emoji", color = Color.Gray, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun WhatsAppExportDialog(
    bitmap: Bitmap,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var packName by remember { mutableStateOf("😂 Memes WhatsApp") }
    val existingPacks by viewModel.packs.collectAsState()
    val quickPacks = remember(existingPacks) {
        (existingPacks.map { it.name } + listOf("😂 Memes WhatsApp", "🔥 Reações", "💬 Conversas", "❤️ Favoritas")).distinct()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.5.dp, WhatsAppGreenLight),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .testTag("dialog_whatsapp_export")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(WhatsAppGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💬", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Exportar para o WhatsApp",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Sticker WebP Transparente 512x512",
                                color = WhatsAppGreenLight,
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sticker Preview Card with Checkerboard
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                    modifier = Modifier
                        .size(190.dp)
                        .aspectRatio(1f)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CheckerboardBackground(modifier = Modifier.fillMaxSize())
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Figurinha WebP",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.75f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "WEBP • 512×512",
                                color = WhatsAppGreenLight,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Pack Name Input
                Text(
                    text = "Pacote da Figurinha:",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = packName,
                    onValueChange = { packName = it },
                    placeholder = { Text("Nome do pacote (ex: Memes da Galera)", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = WhatsAppGreen,
                        unfocusedBorderColor = Color.Gray,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_pack_name")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick pack chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickPacks) { qp ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (packName == qp) WhatsAppGreen.copy(alpha = 0.3f) else DarkSurfaceElevated,
                            border = BorderStroke(1.dp, if (packName == qp) WhatsAppGreenLight else Color.Transparent),
                            modifier = Modifier.clickable { packName = qp }
                        ) {
                            Text(
                                text = qp,
                                color = if (packName == qp) Color.White else Color.LightGray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Instructions Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = WhatsAppGreen.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("💡", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ao compartilhar no WhatsApp, envie a figurinha em qualquer conversa. Toque nela e selecione 'Adicionar às Favoritas' para salvá-la no seu teclado de stickers!",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action 1: Enviar para o WhatsApp
                Button(
                    onClick = {
                        viewModel.exportCurrentStickerWhatsAppWebp(packName) {
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_export_dialog_whatsapp")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "COMPARTILHAR NO WHATSAPP",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action 2 & 3 in a Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.downloadCurrentStickerWebp()
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.Gray),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_export_dialog_download_webp")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salvar WebP", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.shareCurrentStickerWebpOtherApps()
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color.Gray),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("btn_export_dialog_share_other")
                    ) {
                        Text("Outros Apps", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
