package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SubscriptionEntity
import com.example.ui.Screen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ProAccentPink
import com.example.ui.theme.ProPurple
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppGreenLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppHeader(
    currentScreen: Screen,
    subscription: SubscriptionEntity?,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier.testTag("app_header"),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(WhatsAppGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "FP",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "FIGURINHAS ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = ProAccentPink
                        ) {
                            Text(
                                text = "PRO",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "Para WhatsApp",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }
        },
        actions = {
            if (currentScreen != Screen.LANDING && currentScreen != Screen.AUTH) {
                val isTrial = subscription?.status == "TRIAL"
                val days = subscription?.trialDaysRemaining ?: 5

                Surface(
                    onClick = { onNavigate(Screen.ASSINATURA) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isTrial) Color(0xFFF59E0B).copy(alpha = 0.2f) else WhatsAppGreen.copy(alpha = 0.2f),
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .testTag("subscription_badge")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isTrial) Color(0xFFF59E0B) else WhatsAppGreenLight)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isTrial) "Teste ($days d)" else "PRO Ativo",
                            color = if (isTrial) Color(0xFFFBBF24) else WhatsAppGreenLight,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = DarkBackground
        )
    )
}

@Composable
fun AppBottomNav(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.testTag("bottom_nav_bar"),
        containerColor = DarkSurface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentScreen == Screen.DASHBOARD,
            onClick = { onNavigate(Screen.DASHBOARD) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
            label = { Text("Início", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = WhatsAppGreenLight,
                selectedTextColor = WhatsAppGreenLight,
                indicatorColor = WhatsAppGreen.copy(alpha = 0.2f),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            ),
            modifier = Modifier.testTag("nav_item_dashboard")
        )
        NavigationBarItem(
            selected = currentScreen == Screen.CAMERA,
            onClick = { onNavigate(Screen.CAMERA) },
            icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Câmera") },
            label = { Text("Câmera", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = WhatsAppGreenLight,
                selectedTextColor = WhatsAppGreenLight,
                indicatorColor = WhatsAppGreen.copy(alpha = 0.2f),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            ),
            modifier = Modifier.testTag("nav_item_camera")
        )
        NavigationBarItem(
            selected = currentScreen == Screen.BIBLIOTECA,
            onClick = { onNavigate(Screen.BIBLIOTECA) },
            icon = { Icon(Icons.Default.Collections, contentDescription = "Figurinhas") },
            label = { Text("Figurinhas", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = WhatsAppGreenLight,
                selectedTextColor = WhatsAppGreenLight,
                indicatorColor = WhatsAppGreen.copy(alpha = 0.2f),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            ),
            modifier = Modifier.testTag("nav_item_library")
        )
        NavigationBarItem(
            selected = currentScreen == Screen.PACOTES,
            onClick = { onNavigate(Screen.PACOTES) },
            icon = { Icon(Icons.Default.FolderSpecial, contentDescription = "Pacotes") },
            label = { Text("Pacotes", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = WhatsAppGreenLight,
                selectedTextColor = WhatsAppGreenLight,
                indicatorColor = WhatsAppGreen.copy(alpha = 0.2f),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            ),
            modifier = Modifier.testTag("nav_item_pacotes")
        )
        NavigationBarItem(
            selected = currentScreen == Screen.CONFIGURACOES,
            onClick = { onNavigate(Screen.CONFIGURACOES) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Config") },
            label = { Text("Ajustes", fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = WhatsAppGreenLight,
                selectedTextColor = WhatsAppGreenLight,
                indicatorColor = WhatsAppGreen.copy(alpha = 0.2f),
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            ),
            modifier = Modifier.testTag("nav_item_settings")
        )
    }
}

/**
 * Checkerboard canvas representing transparent sticker background
 */
@Composable
fun CheckerboardBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val squareSize = 16.dp.toPx()
        val numCols = (size.width / squareSize).toInt() + 1
        val numRows = (size.height / squareSize).toInt() + 1

        for (row in 0 until numRows) {
            for (col in 0 until numCols) {
                val isEven = (row + col) % 2 == 0
                val color = if (isEven) Color(0xFF1E293B) else Color(0xFF0F172A)
                drawRect(
                    color = color,
                    topLeft = Offset(col * squareSize, row * squareSize),
                    size = Size(squareSize, squareSize)
                )
            }
        }
    }
}

@Composable
fun ProcessingOverlay(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkSurface,
            tonalElevation = 12.dp,
            modifier = Modifier.padding(32.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp)
            ) {
                CircularProgressIndicator(
                    color = WhatsAppGreenLight,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(44.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}
