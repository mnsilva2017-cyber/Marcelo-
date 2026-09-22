package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.StickerEntity
import com.example.data.StickerPackEntity
import com.example.ui.MainViewModel
import com.example.ui.PackWithStickers
import com.example.ui.Screen
import com.example.ui.components.CheckerboardBackground
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ProAccentOrange
import com.example.ui.theme.ProAccentPink
import com.example.ui.theme.ProPurple
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppGreenLight
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PacotesScreen(
    viewModel: MainViewModel,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val packsEntities by viewModel.packs.collectAsState()
    val stickers by viewModel.stickers.collectAsState()

    // Aggregate explicit packs with stickers categories
    val packsWithStickers by remember(packsEntities, stickers) {
        derivedStateOf {
            val packMap = packsEntities.associateBy { it.name }.toMutableMap()
            val stickerGroups = stickers.groupBy { it.category }
            val allNames = (packsEntities.map { it.name } + stickerGroups.keys).distinct()

            allNames.map { name ->
                val entity = packMap[name]
                val packStickers = stickerGroups[name] ?: emptyList()
                PackWithStickers(
                    id = entity?.id ?: name,
                    name = name,
                    description = entity?.description ?: "",
                    stickers = packStickers,
                    createdAt = entity?.createdAt ?: System.currentTimeMillis()
                )
            }.sortedWith(
                compareByDescending<PackWithStickers> { it.stickers.size }
                    .thenByDescending { it.createdAt }
            )
        }
    }

    var selectedFilter by remember { mutableStateOf("Todos") }
    val filteredPacks by remember(packsWithStickers, selectedFilter) {
        derivedStateOf {
            when (selectedFilter) {
                "Com figurinhas" -> packsWithStickers.filter { it.stickers.isNotEmpty() }
                "Vazios" -> packsWithStickers.filter { it.stickers.isEmpty() }
                else -> packsWithStickers
            }
        }
    }

    // Dialog States
    var showCreatePackDialog by remember { mutableStateOf(false) }
    var packToRename by remember { mutableStateOf<PackWithStickers?>(null) }
    var packToDelete by remember { mutableStateOf<PackWithStickers?>(null) }
    var packToOrganize by remember { mutableStateOf<PackWithStickers?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("pacotes_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onNavigate(Screen.DASHBOARD) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Meus Pacotes",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        val totalStickers = stickers.size
                        Text(
                            text = "${packsWithStickers.size} pacotes • $totalStickers figurinhas",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }

                Button(
                    onClick = { showCreatePackDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("btn_create_pack")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Novo Pacote",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val filters = listOf("Todos", "Com figurinhas", "Vazios")
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) WhatsAppGreen.copy(alpha = 0.25f) else DarkSurface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) WhatsAppGreenLight else DarkSurfaceElevated
                        ),
                        modifier = Modifier.clickable { selectedFilter = filter }
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) WhatsAppGreenLight else Color.LightGray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredPacks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("📦", fontSize = 34.sp)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (selectedFilter == "Todos") "Nenhum pacote criado ainda" else "Nenhum pacote neste filtro",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Crie pacotes personalizados para organizar suas figurinhas e enviar no WhatsApp!",
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showCreatePackDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CRIAR MEU PRIMEIRO PACOTE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredPacks, key = { it.id }) { pack ->
                        PackItemCard(
                            pack = pack,
                            onOrganize = { packToOrganize = pack },
                            onRename = { packToRename = pack },
                            onDelete = { packToDelete = pack },
                            onShare = { viewModel.sharePackStickers(pack) },
                            onAddSticker = { onNavigate(Screen.CAMERA) }
                        )
                    }
                }
            }
        }

        // Dialog: Create Pack
        if (showCreatePackDialog) {
            CreatePackDialog(
                onDismiss = { showCreatePackDialog = false },
                onCreate = { name, desc ->
                    viewModel.createPack(name, desc) {
                        showCreatePackDialog = false
                    }
                }
            )
        }

        // Dialog: Rename Pack
        packToRename?.let { pack ->
            RenamePackDialog(
                currentName = pack.name,
                onDismiss = { packToRename = null },
                onRename = { newName ->
                    viewModel.renamePack(pack.name, newName) {
                        packToRename = null
                    }
                }
            )
        }

        // Dialog: Delete Pack (allowing delete pack + stickers or move to Geral)
        packToDelete?.let { pack ->
            DeletePackDialog(
                pack = pack,
                onDismiss = { packToDelete = null },
                onConfirmDelete = { deleteStickers ->
                    viewModel.deletePack(pack.name, deleteStickers) {
                        packToDelete = null
                        if (packToOrganize?.name == pack.name) {
                            packToOrganize = null
                        }
                    }
                }
            )
        }

        // Dialog / Sheet: Organize Pack Stickers
        packToOrganize?.let { pack ->
            // Keep pack data updated with live state
            val livePack = packsWithStickers.find { it.name == pack.name } ?: pack
            OrganizePackDialog(
                pack = livePack,
                allPacks = packsWithStickers.map { it.name },
                onDismiss = { packToOrganize = null },
                onShareSticker = { sticker ->
                    viewModel.shareExistingStickerWhatsApp(sticker)
                },
                onMoveSticker = { sticker, targetPack ->
                    viewModel.moveStickerToPack(sticker, targetPack)
                },
                onAddMore = {
                    packToOrganize = null
                    onNavigate(Screen.CAMERA)
                }
            )
        }
    }
}

@Composable
fun PackItemCard(
    pack: PackWithStickers,
    onOrganize: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onAddSticker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkSurfaceElevated),
        modifier = modifier
            .fillMaxWidth()
            .testTag("pack_card_${pack.name}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Pack Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(WhatsAppGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderSpecial,
                            contentDescription = null,
                            tint = WhatsAppGreenLight,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = pack.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${pack.stickers.size} figurinha${if (pack.stickers.size != 1) "s" else ""}",
                                color = WhatsAppGreenLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (pack.description.isNotBlank()) {
                                Text(
                                    text = " • ${pack.description}",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Header Action icons: Edit / Delete
                Row {
                    IconButton(
                        onClick = onRename,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Renomear",
                            tint = Color.LightGray,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Excluir Pacote",
                            tint = ProAccentPink,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stickers Preview Grid / Row
            if (pack.stickers.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceElevated,
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onAddSticker)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = WhatsAppGreenLight,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pacote vazio. Toque para criar a primeira figurinha!",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val previewCount = 4
                    val previewStickers = pack.stickers.take(previewCount)
                    val remainingCount = pack.stickers.size - previewCount

                    for (sticker in previewStickers) {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                CheckerboardBackground(modifier = Modifier.fillMaxSize())
                                AsyncImage(
                                    model = File(sticker.imagePath),
                                    contentDescription = sticker.title,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp)
                                )
                            }
                        }
                    }

                    // Extra placeholder if more stickers exist
                    if (remainingCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurfaceElevated,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clickable(onClick = onOrganize)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+$remainingCount",
                                    color = WhatsAppGreenLight,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else if (pack.stickers.size < previewCount) {
                        // Empty slot to encourage adding
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurfaceElevated.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clickable(onClick = onAddSticker)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Adicionar",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // WhatsApp recommendation helper
            val count = pack.stickers.size
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (count >= 3) {
                    Text(
                        text = "✓ Padrão WhatsApp atendido ($count figurinhas)",
                        color = WhatsAppGreenLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        text = "Dica: WhatsApp recomenda mín. 3 figurinhas por pacote (${3 - count} restante${if (3 - count > 1) "s" else ""})",
                        color = ProAccentOrange,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Organizar & Enviar WhatsApp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOrganize,
                    border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("btn_organize_${pack.name}")
                ) {
                    Icon(
                        Icons.Default.DriveFileMove,
                        contentDescription = null,
                        tint = WhatsAppGreenLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Organizar",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = onShare,
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                    shape = RoundedCornerShape(10.dp),
                    enabled = pack.stickers.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("btn_share_pack_${pack.name}")
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WhatsApp",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CreatePackDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, desc: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    val quickNames = listOf(
        "😂 Memes Zueira",
        "🔥 Reações de Grupo",
        "💬 Família",
        "🐶 Bichinhos",
        "💼 Trabalho",
        "⭐ Favoritas"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().testTag("dialog_create_pack")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📦", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Novo Pacote",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Nome do Pacote:",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Ex: Memes da Galera", color = Color.Gray) },
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
                    modifier = Modifier.fillMaxWidth().testTag("input_new_pack_name")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick suggestions
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(quickNames) { qn ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (name == qn) WhatsAppGreen.copy(alpha = 0.3f) else DarkSurfaceElevated,
                            border = BorderStroke(1.dp, if (name == qn) WhatsAppGreenLight else Color.Transparent),
                            modifier = Modifier.clickable { name = qn }
                        ) {
                            Text(
                                text = qn,
                                color = if (name == qn) Color.White else Color.LightGray,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Descrição (opcional):",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Ex: Figurinhas engraçadas para conversas", color = Color.Gray) },
                    maxLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = WhatsAppGreen,
                        unfocusedBorderColor = Color.Gray,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onCreate(name.trim(), description.trim())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                    shape = RoundedCornerShape(12.dp),
                    enabled = name.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_confirm_create_pack")
                ) {
                    Text("CRIAR PACOTE", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun RenamePackDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onRename: (newName: String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().testTag("dialog_rename_pack")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Renomear Pacote",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
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
                    modifier = Modifier.fillMaxWidth().testTag("input_rename_pack")
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar", color = Color.LightGray)
                    }

                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                onRename(newName.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                        shape = RoundedCornerShape(10.dp),
                        enabled = newName.isNotBlank() && newName != currentName,
                        modifier = Modifier.weight(1f).testTag("btn_confirm_rename")
                    ) {
                        Text("SALVAR", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun DeletePackDialog(
    pack: PackWithStickers,
    onDismiss: () -> Unit,
    onConfirmDelete: (deleteStickers: Boolean) -> Unit
) {
    var deleteStickersOption by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.5.dp, ProAccentPink.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth().testTag("dialog_delete_pack")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(ProAccentPink.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = ProAccentPink,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Excluir Pacote",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Tem certeza que deseja excluir o pacote \"${pack.name}\"?",
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 18.sp
                )

                if (pack.stickers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Este pacote contém ${pack.stickers.size} figurinha(s). Escolha o que fazer com elas:",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Option 1: Keep stickers (Move to Geral)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (!deleteStickersOption) WhatsAppGreen.copy(alpha = 0.15f) else DarkSurfaceElevated,
                        border = BorderStroke(
                            1.dp,
                            if (!deleteStickersOption) WhatsAppGreenLight else Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { deleteStickersOption = false }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = !deleteStickersOption,
                                onClick = { deleteStickersOption = false },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = WhatsAppGreenLight,
                                    unselectedColor = Color.Gray
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Manter figurinhas (Recomendado)",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Move as figurinhas para o pacote \"Geral\"",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Option 2: Delete stickers too
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (deleteStickersOption) ProAccentPink.copy(alpha = 0.15f) else DarkSurfaceElevated,
                        border = BorderStroke(
                            1.dp,
                            if (deleteStickersOption) ProAccentPink else Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { deleteStickersOption = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = deleteStickersOption,
                                onClick = { deleteStickersOption = true },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = ProAccentPink,
                                    unselectedColor = Color.Gray
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "Apagar pacote E as figurinhas",
                                    color = ProAccentPink,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "As figurinhas serão excluídas permanentemente",
                                    color = Color.Gray,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar", color = Color.LightGray)
                    }

                    Button(
                        onClick = { onConfirmDelete(deleteStickersOption) },
                        colors = ButtonDefaults.buttonColors(containerColor = ProAccentPink),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("btn_confirm_delete_pack")
                    ) {
                        Text("EXCLUIR", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun OrganizePackDialog(
    pack: PackWithStickers,
    allPacks: List<String>,
    onDismiss: () -> Unit,
    onShareSticker: (StickerEntity) -> Unit,
    onMoveSticker: (StickerEntity, targetPack: String) -> Unit,
    onAddMore: () -> Unit
) {
    var stickerToMove by remember { mutableStateOf<StickerEntity?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dialog_organize_pack")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Organizar: ${pack.name}",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${pack.stickers.size} figurinhas no pacote",
                            color = WhatsAppGreenLight,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (pack.stickers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📦", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Nenhuma figurinha neste pacote",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onAddMore,
                                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Criar Figurinha", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp)
                    ) {
                        items(pack.stickers, key = { it.id }) { sticker ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                                border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                    ) {
                                        CheckerboardBackground(modifier = Modifier.fillMaxSize())
                                        AsyncImage(
                                            model = File(sticker.imagePath),
                                            contentDescription = sticker.title,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = sticker.title,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Move button
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = ProPurple.copy(alpha = 0.25f),
                                            modifier = Modifier.clickable {
                                                stickerToMove = sticker
                                            }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.DriveFileMove,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Mover", color = Color.White, fontSize = 10.sp)
                                            }
                                        }

                                        // Share button
                                        IconButton(
                                            onClick = { onShareSticker(sticker) },
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(WhatsAppGreen, CircleShape)
                                        ) {
                                            Icon(
                                                Icons.Default.Share,
                                                contentDescription = "Enviar",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("FECHAR", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Sub-dialog: Move sticker to another pack
    stickerToMove?.let { sticker ->
        MoveStickerDialog(
            sticker = sticker,
            currentPack = pack.name,
            allPacks = allPacks,
            onDismiss = { stickerToMove = null },
            onMove = { targetPack ->
                onMoveSticker(sticker, targetPack)
                stickerToMove = null
            }
        )
    }
}

@Composable
fun MoveStickerDialog(
    sticker: StickerEntity,
    currentPack: String,
    allPacks: List<String>,
    onDismiss: () -> Unit,
    onMove: (targetPack: String) -> Unit
) {
    var newPackName by remember { mutableStateOf("") }
    var selectedExistingPack by remember { mutableStateOf<String?>(null) }
    val availablePacks = allPacks.filter { it != currentPack }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().testTag("dialog_move_sticker")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Mover Figurinha",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
                Text(
                    text = "Mover \"${sticker.title}\" para outro pacote:",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (availablePacks.isNotEmpty()) {
                    Text(
                        text = "Pacotes Existentes:",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (p in availablePacks) {
                            val isSelected = selectedExistingPack == p
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) WhatsAppGreen.copy(alpha = 0.25f) else DarkSurfaceElevated,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) WhatsAppGreenLight else Color.Transparent
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedExistingPack = p
                                        newPackName = ""
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = if (isSelected) WhatsAppGreenLight else Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = p,
                                        color = if (isSelected) Color.White else Color.LightGray,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("ou crie um novo:", color = Color.Gray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                }

                OutlinedTextField(
                    value = newPackName,
                    onValueChange = {
                        newPackName = it
                        if (it.isNotBlank()) selectedExistingPack = null
                    },
                    placeholder = { Text("Nome do novo pacote", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = WhatsAppGreen,
                        unfocusedBorderColor = Color.Gray,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().testTag("input_move_new_pack")
                )

                Spacer(modifier = Modifier.height(18.dp))

                val chosen = selectedExistingPack ?: newPackName.trim()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar", color = Color.LightGray)
                    }

                    Button(
                        onClick = {
                            if (chosen.isNotBlank()) {
                                onMove(chosen)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                        shape = RoundedCornerShape(10.dp),
                        enabled = chosen.isNotBlank(),
                        modifier = Modifier.weight(1f).testTag("btn_confirm_move")
                    ) {
                        Text("MOVER", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
