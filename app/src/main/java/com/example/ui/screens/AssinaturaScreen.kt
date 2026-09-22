package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ProAccentPink
import com.example.ui.theme.ProAccentYellow
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.WhatsAppGreenLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AssinaturaScreen(
    viewModel: MainViewModel,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val subscription by viewModel.subscription.collectAsState()
    var showCheckoutModal by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }

    val isTrial = subscription?.status == "TRIAL"
    val isCanceled = subscription?.status == "CANCELED"
    val trialDays = subscription?.trialDaysRemaining ?: 5
    val nextBillingDate = subscription?.nextBillingDate ?: (System.currentTimeMillis() + 5L * 86400000L)
    val formattedDate = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("pt", "BR")).format(Date(nextBillingDate))

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(20.dp)
            .testTag("assinatura_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Seu Acesso",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Text(
            text = "Gerenciamento do plano e status da assinatura",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Subscription Status Card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(
                1.5.dp,
                if (isTrial) Color(0xFFF59E0B) else if (isCanceled) Color.Gray else WhatsAppGreen
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
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
                        shape = RoundedCornerShape(20.dp),
                        color = when {
                            isCanceled -> Color.Gray.copy(alpha = 0.2f)
                            isTrial -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                            else -> WhatsAppGreen.copy(alpha = 0.2f)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isCanceled -> Color.Gray
                                            isTrial -> Color(0xFFF59E0B)
                                            else -> WhatsAppGreenLight
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isCanceled -> "Cancelado"
                                    isTrial -> "🟡 Teste grátis"
                                    else -> "🟢 Ativo"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = when {
                                    isCanceled -> Color.LightGray
                                    isTrial -> Color(0xFFFBBF24)
                                    else -> WhatsAppGreenLight
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "R$ 15",
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        color = WhatsAppGreenLight
                    )
                    Text(
                        text = "/mês",
                        color = Color.LightGray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isTrial) {
                    Text(
                        text = "Seu teste termina em $trialDays dias.",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFBBF24),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Após o período de teste, cobrança mensal de R$ 15.",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                } else if (isCanceled) {
                    Text(
                        text = "Sua assinatura está cancelada. Assine novamente para continuar criando.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                } else {
                    Text(
                        text = "Próxima cobrança: $formattedDate",
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                androidx.compose.material3.HorizontalDivider(color = DarkSurfaceElevated)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Recursos inclusos no seu plano:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))

                val benefits = listOf(
                    "Criação ilimitada de figurinhas",
                    "Recorte automático e preservação de nitidez",
                    "Fundo transparente padrão WhatsApp",
                    "Exportação direta para o WhatsApp",
                    "Download de PNG em alta resolução",
                    "Sem anúncios e sem marcas d'água"
                )

                benefits.forEach { b ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 3.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = WhatsAppGreenLight, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(b, color = Color(0xFFCBD5E1), fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isTrial || isCanceled) {
                    Button(
                        onClick = { showCheckoutModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_subscribe_now")
                    ) {
                        Text(
                            text = "ASSINAR AGORA (R$ 15/mês)",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = { showCancelDialog = true },
                        border = BorderStroke(1.dp, Color.Gray),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancelar assinatura", color = Color.LightGray, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Checkout Modal (PIX / Cartão)
    if (showCheckoutModal) {
        var paymentTab by remember { mutableIntStateOf(0) } // 0 = PIX, 1 = Cartão
        val pixCopyPaste = "00020126580014br.gov.bcb.pix0136figurinhaspro@pix.com.br520400005303986540515.005802BR5915FIGURINHAS PRO6009SAO PAULO62070503***6304E8A2"

        AlertDialog(
            onDismissRequest = { showCheckoutModal = false },
            title = {
                Text(
                    text = "Assinar FIGURINHAS PRO",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text("Valor: R$ 15,00 por mês", color = WhatsAppGreenLight, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(14.dp))

                    TabRow(
                        selectedTabIndex = paymentTab,
                        containerColor = DarkSurfaceElevated,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[paymentTab]),
                                color = WhatsAppGreenLight
                            )
                        }
                    ) {
                        Tab(
                            selected = paymentTab == 0,
                            onClick = { paymentTab = 0 },
                            text = { Text("PIX", fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = paymentTab == 1,
                            onClick = { paymentTab = 1 },
                            text = { Text("Cartão", fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (paymentTab == 0) {
                        // PIX payment
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.QrCode,
                                    contentDescription = "QR Code Pix",
                                    tint = Color.Black,
                                    modifier = Modifier.size(80.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Aprovação instantânea via PIX!",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Chave PIX Copia e Cola", pixCopyPaste)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Código PIX Copia e Cola copiado!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copiar código PIX", fontSize = 12.sp)
                            }
                        }
                    } else {
                        // Credit card simulation
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CreditCard, contentDescription = null, tint = WhatsAppGreenLight)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cartão de Crédito ou Débito", color = Color.White, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Cobrança recorrente mensal de R$ 15,00. Cancele quando quiser diretamente no aplicativo.",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCheckoutModal = false
                        viewModel.subscribe(if (paymentTab == 0) "PIX" else "CARTAO")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppGreen)
                ) {
                    Text("Confirmar Assinatura", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckoutModal = false }) {
                    Text("Voltar")
                }
            }
        )
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancelar assinatura?") },
            text = {
                Text("Você continuará tendo acesso até o final do período vigente. Deseja realmente cancelar?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelDialog = false
                        viewModel.cancelSubscription()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ProAccentPink)
                ) {
                    Text("Sim, cancelar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Manter plano")
                }
            }
        )
    }
}
