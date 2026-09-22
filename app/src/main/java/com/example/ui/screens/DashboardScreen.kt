package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ProAccentOrange
import com.example.ui.theme.ProAccentPink
import com.example.ui.theme.ProAccentYellow
import com.example.ui.theme.ProPurple
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppGreenLight

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val user by viewModel.currentUser.collectAsState()
    val subscription by viewModel.subscription.collectAsState()
    val stickers by viewModel.stickers.collectAsState()

    // Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onPhotoUriSelected(uri)
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("dashboard_screen")
    ) {
        // Subscription Status Bar
        val isTrial = subscription?.status == "TRIAL"
        val trialDays = subscription?.trialDaysRemaining ?: 5

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isTrial) Color(0xFF292015) else Color(0xFF132A22)
            ),
            border = BorderStroke(
                1.dp,
                if (isTrial) Color(0xFFF59E0B).copy(alpha = 0.5f) else WhatsAppGreen.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(Screen.ASSINATURA) }
                .testTag("card_subscription_status")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isTrial) Color(0xFFF59E0B) else WhatsAppGreenLight)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isTrial) "🟡 Teste grátis" else "🟢 Ativo — Plano Figurinhas Pro",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (isTrial) "Seu teste termina em $trialDays dias." else "Acesso premium liberado!",
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = "Ver plano",
                    tint = Color.LightGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Greeting header
        Text(
            text = "Olá${if (user != null) ", ${user?.name}" else ""}! 👋",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF94A3B8),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Vamos criar uma figurinha?",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 26.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Hero Direct Action Banner
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigate(Screen.CAMERA) }
                .testTag("hero_take_photo_banner")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(listOf(WhatsAppGreen, Color(0xFF007A60))),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "RECORTE AUTOMÁTICO",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tirar Foto Agora",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Abre a câmera, recorta o fundo e aplica contorno em segundos!",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "OPÇÕES RÁPIDAS",
            style = MaterialTheme.typography.labelLarge,
            color = WhatsAppGreenLight,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Cards Grid (Tirar foto, Escolher da galeria, Criar Meme, Minhas Figurinhas, Minha Biblioteca, Configurações)
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DashboardActionCard(
                    icon = Icons.Default.CameraAlt,
                    title = "Tirar foto",
                    subtitle = "Câmera do celular",
                    badgeColor = WhatsAppGreen,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("card_take_photo"),
                    onClick = { onNavigate(Screen.CAMERA) }
                )

                DashboardActionCard(
                    icon = Icons.Default.PhotoLibrary,
                    title = "Escolher da galeria",
                    subtitle = "Fotos salvas",
                    badgeColor = ProPurple,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("card_pick_gallery"),
                    onClick = { galleryLauncher.launch("image/*") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DashboardActionCard(
                    icon = Icons.Default.EmojiEmotions,
                    title = "Criar Meme",
                    subtitle = "Frases e reações",
                    badgeColor = ProAccentPink,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("card_create_meme"),
                    onClick = {
                        viewModel.updateMemeCategory("😂 Engraçado", "KKKKKK")
                        onNavigate(Screen.CAMERA)
                    }
                )

                DashboardActionCard(
                    icon = Icons.Default.Star,
                    title = "Minhas Figurinhas",
                    subtitle = "${stickers.size} salvas",
                    badgeColor = ProAccentYellow,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("card_my_stickers"),
                    onClick = { onNavigate(Screen.BIBLIOTECA) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                DashboardActionCard(
                    icon = Icons.Default.FolderSpecial,
                    title = "Meus Pacotes",
                    subtitle = "Organizar e exportar",
                    badgeColor = Color(0xFF0EA5E9),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("card_my_packs"),
                    onClick = { onNavigate(Screen.PACOTES) }
                )

                DashboardActionCard(
                    icon = Icons.Default.Settings,
                    title = "Configurações",
                    subtitle = "Conta e preferências",
                    badgeColor = Color(0xFF64748B),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("card_settings"),
                    onClick = { onNavigate(Screen.CONFIGURACOES) }
                )
            }
        }
    }
}

@Composable
fun DashboardActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, DarkSurfaceElevated),
        modifier = modifier
            .height(115.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(badgeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(20.dp))
            }

            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }
    }
}
