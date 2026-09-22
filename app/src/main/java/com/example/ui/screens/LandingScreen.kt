package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.Screen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ProAccentPink
import com.example.ui.theme.ProAccentYellow
import com.example.ui.theme.ProPurple
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppGreenLight

@Composable
fun LandingScreen(
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(bottom = 40.dp)
            .testTag("landing_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Brand Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(WhatsAppGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "FP",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "FIGURINHAS ",
                    style = MaterialTheme.typography.titleLarge,
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
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            OutlinedButton(
                onClick = { onNavigate(Screen.AUTH) },
                border = BorderStroke(1.dp, WhatsAppGreenLight),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppGreenLight),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("landing_login_button")
            ) {
                Text("Entrar", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Badge Slogan
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = WhatsAppGreen.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.4f)),
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Text(
                text = "✨ “Tire uma foto. Vire figurinha. Compartilhe sua reação.”",
                color = WhatsAppGreenLight,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Headline
        Text(
            text = "Sua foto.\nSua figurinha.\nSeu meme.",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            fontSize = 36.sp,
            lineHeight = 42.sp,
            textAlign = TextAlign.Center,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Subheadline
        Text(
            text = "Transforme suas fotos em figurinhas personalizadas para compartilhar suas melhores reações no WhatsApp.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = Color(0xFFCBD5E1),
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 28.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onNavigate(Screen.CAMERA) },
                colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("btn_create_my_sticker")
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "CRIAR MINHA FIGURINHA",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }

            Button(
                onClick = { onNavigate(Screen.AUTH) },
                colors = ButtonDefaults.buttonColors(containerColor = ProPurple),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_try_free")
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "EXPERIMENTAR GRÁTIS (5 DIAS)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Showcase Cards (Exemplos visuais de figurinhas com pessoas, expressões, textos, emojis e efeitos)
        Text(
            text = "VEJA COMO FICA:",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = WhatsAppGreenLight,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Showcase card 1
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(200.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, ProPurple.copy(alpha = 0.5f))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_sticker_sample1),
                        contentDescription = "Figurinha meme KKKKK",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "😂 Memes & Reações",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }

            // Showcase card 2
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(200.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.5f))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_sticker_sample2),
                        contentDescription = "Figurinha surpresa MEU DEUS",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "😱 Expressões & Balões",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Plan Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.5.dp, WhatsAppGreen)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FIGURINHAS PRO",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = WhatsAppGreen
                    ) {
                        Text(
                            text = "5 DIAS GRÁTIS",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "R$ 15",
                        fontWeight = FontWeight.Black,
                        fontSize = 34.sp,
                        color = WhatsAppGreenLight
                    )
                    Text(
                        text = "/mês",
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = Color.LightGray,
                        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                    )
                }

                Text(
                    text = "Teste grátis de 5 dias. Após o período de teste, cobrança mensal de R$ 15.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Feature checklist
                val features = listOf(
                    "Criar figurinhas ilimitadas",
                    "Usar câmera frontal e traseira",
                    "Enviar fotos da galeria",
                    "Recorte automático da pessoa",
                    "Fundo transparente padrão WhatsApp",
                    "Editor de texto com fontes e cores",
                    "Emojis, efeitos e filtros artísticos",
                    "Molduras e balões de fala",
                    "Biblioteca de figurinhas e histórico",
                    "Download em PNG de alta resolução",
                    "Compartilhamento nativo no WhatsApp"
                )

                features.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(WhatsAppGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = WhatsAppGreenLight,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = feature,
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = { onNavigate(Screen.AUTH) },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_start_free_trial")
                ) {
                    Text(
                        text = "COMEÇAR TESTE GRÁTIS",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
