package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.example.engine.CutoutAlgorithm
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.CheckerboardBackground
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ProAccentPink
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppGreenLight

@Composable
fun RecorteScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentAlgorithm by viewModel.selectedCutoutAlgorithm.collectAsState()
    val currentSensitivity by viewModel.cutoutSensitivity.collectAsState()

    var activeBitmap by remember { mutableStateOf(viewModel.cutoutBitmap) }

    // Sync activeBitmap with ViewModel updates
    LaunchedEffect(viewModel.cutoutBitmap) {
        if (viewModel.cutoutBitmap != null) {
            activeBitmap = viewModel.cutoutBitmap
        }
    }
    var selectedTool by remember { mutableStateOf("VIEW") } // VIEW, ERASE, RESTORE
    var brushRadius by remember { mutableFloatStateOf(28f) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(20.dp)
            .testTag("recorte_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Recorte Automático",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = androidx.compose.ui.graphics.Color.White
        )
        Text(
            text = "Fundo removido automaticamente. Rosto nítido e natural!",
            style = MaterialTheme.typography.bodyMedium,
            color = WhatsAppGreenLight,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Cutout Canvas preview with Checkerboard transparency
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(2.dp, WhatsAppGreen.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .testTag("cutout_canvas_card")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { viewSize = it }
                    .pointerInput(selectedTool, brushRadius) {
                        if (selectedTool == "ERASE" || selectedTool == "RESTORE") {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val currentBmp = activeBitmap ?: return@detectDragGestures
                                val rawBmp = viewModel.rawPhotoBitmap ?: return@detectDragGestures

                                if (viewSize.width > 0 && viewSize.height > 0) {
                                    val scaleX = currentBmp.width.toFloat() / viewSize.width
                                    val scaleY = currentBmp.height.toFloat() / viewSize.height
                                    val bmpX = change.position.x * scaleX
                                    val bmpY = change.position.y * scaleY

                                    val mutable = currentBmp.copy(Bitmap.Config.ARGB_8888, true)
                                    val canvas = Canvas(mutable)

                                    if (selectedTool == "ERASE") {
                                        val erasePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                                        }
                                        canvas.drawCircle(bmpX, bmpY, brushRadius, erasePaint)
                                    } else if (selectedTool == "RESTORE") {
                                        // Sample source pixels back
                                        val restorePaint = Paint(Paint.ANTI_ALIAS_FLAG)
                                        canvas.drawBitmap(rawBmp, 0f, 0f, restorePaint)
                                    }
                                    activeBitmap = mutable
                                    viewModel.manualCutoutUpdate(mutable)
                                }
                            }
                        }
                    }
            ) {
                CheckerboardBackground(modifier = Modifier.fillMaxSize())

                if (activeBitmap != null) {
                    Image(
                        bitmap = activeBitmap!!.asImageBitmap(),
                        contentDescription = "Pessoa recortada",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Surface(
                    color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Fundo Transparente",
                        color = androidx.compose.ui.graphics.Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Algorithm selection card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = WhatsAppGreenLight,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ALGORITMO DE RECORTE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CutoutAlgorithm.values().forEach { algo ->
                        val isSelected = currentAlgorithm == algo
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setCutoutAlgorithm(algo) },
                            label = {
                                Text(
                                    text = when (algo) {
                                        CutoutAlgorithm.ADAPTIVE_CHROMA_DEPTH -> "Inteligente"
                                        CutoutAlgorithm.COLOR_DISTANCE -> "Cor do Fundo"
                                        CutoutAlgorithm.PORTRAIT_FOCUS -> "Foco Retrato"
                                    },
                                    fontSize = 11.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = WhatsAppGreen,
                                selectedLabelColor = androidx.compose.ui.graphics.Color.White,
                                containerColor = DarkSurface,
                                labelColor = androidx.compose.ui.graphics.Color.LightGray
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = androidx.compose.ui.graphics.Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Intensidade: ${(currentSensitivity * 100).toInt()}%",
                        fontSize = 11.sp,
                        color = androidx.compose.ui.graphics.Color.LightGray
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Slider(
                        value = currentSensitivity,
                        onValueChange = { viewModel.setCutoutSensitivity(it) },
                        valueRange = 0.6f..1.4f,
                        colors = SliderDefaults.colors(
                            thumbColor = WhatsAppGreenLight,
                            activeTrackColor = WhatsAppGreen
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Manual touch-up tools
        Text(
            text = "FERRAMENTAS DE AJUSTE MANUAL:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color.LightGray
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilterChip(
                selected = selectedTool == "VIEW",
                onClick = { selectedTool = "VIEW" },
                label = { Text("Visualizar") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WhatsAppGreen,
                    selectedLabelColor = androidx.compose.ui.graphics.Color.White
                ),
                modifier = Modifier.testTag("tool_view")
            )
            FilterChip(
                selected = selectedTool == "ERASE",
                onClick = { selectedTool = "ERASE" },
                label = { Text("Apagar partes") },
                leadingIcon = { Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ProAccentPink,
                    selectedLabelColor = androidx.compose.ui.graphics.Color.White
                ),
                modifier = Modifier.testTag("tool_erase")
            )
            FilterChip(
                selected = selectedTool == "RESTORE",
                onClick = { selectedTool = "RESTORE" },
                label = { Text("Restaurar") },
                leadingIcon = { Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WhatsAppGreenLight,
                    selectedLabelColor = androidx.compose.ui.graphics.Color.Black
                ),
                modifier = Modifier.testTag("tool_restore")
            )
        }

        if (selectedTool == "ERASE" || selectedTool == "RESTORE") {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tamanho do pincel: ${brushRadius.toInt()}px",
                    color = androidx.compose.ui.graphics.Color.LightGray,
                    fontSize = 12.sp,
                    modifier = Modifier.width(150.dp)
                )
                Slider(
                    value = brushRadius,
                    onValueChange = { brushRadius = it },
                    valueRange = 10f..80f,
                    colors = SliderDefaults.colors(
                        thumbColor = WhatsAppGreenLight,
                        activeTrackColor = WhatsAppGreen
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Confirm button: Avançar para o Editor
        Button(
            onClick = {
                viewModel.proceedToEditor()
            },
            colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("btn_proceed_to_editor")
        ) {
            Text(
                text = "AVANÇAR PARA O EDITOR",
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                color = androidx.compose.ui.graphics.Color.White
            )
            Spacer(modifier = Modifier.width(10.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = androidx.compose.ui.graphics.Color.White)
        }
    }
}
