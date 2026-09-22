package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.components.AppBottomNav
import com.example.ui.components.AppHeader
import com.example.ui.components.ProcessingOverlay
import com.example.ui.screens.AssinaturaScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BibliotecaScreen
import com.example.ui.screens.CameraScreen
import com.example.ui.screens.ConfiguracoesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.LandingScreen
import com.example.ui.screens.PacotesScreen
import com.example.ui.screens.RecorteScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FigurinhasProApp()
            }
        }
    }
}

@Composable
fun FigurinhasProApp(viewModel: MainViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val subscription by viewModel.subscription.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val processingMessage by viewModel.processingMessage.collectAsState()

    // Back button behavior
    BackHandler(enabled = currentScreen != Screen.LANDING && currentScreen != Screen.DASHBOARD) {
        when (currentScreen) {
            Screen.EDITOR -> viewModel.navigateTo(Screen.RECORTE)
            Screen.RECORTE -> viewModel.navigateTo(Screen.CAMERA)
            Screen.CAMERA, Screen.BIBLIOTECA, Screen.PACOTES, Screen.ASSINATURA, Screen.CONFIGURACOES -> {
                viewModel.navigateTo(Screen.DASHBOARD)
            }
            Screen.AUTH -> viewModel.navigateTo(Screen.LANDING)
            else -> {}
        }
    }

    val showBars = currentScreen != Screen.LANDING && currentScreen != Screen.AUTH

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        Scaffold(
            topBar = {
                if (showBars) {
                    AppHeader(
                        currentScreen = currentScreen,
                        subscription = subscription,
                        onNavigate = { viewModel.navigateTo(it) }
                    )
                }
            },
            bottomBar = {
                if (showBars && currentScreen != Screen.RECORTE && currentScreen != Screen.EDITOR) {
                    AppBottomNav(
                        currentScreen = currentScreen,
                        onNavigate = { viewModel.navigateTo(it) }
                    )
                }
            },
            containerColor = DarkBackground
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    Screen.LANDING -> LandingScreen(onNavigate = { viewModel.navigateTo(it) })
                    Screen.AUTH -> AuthScreen(viewModel = viewModel, onNavigate = { viewModel.navigateTo(it) })
                    Screen.DASHBOARD -> DashboardScreen(viewModel = viewModel, onNavigate = { viewModel.navigateTo(it) })
                    Screen.CAMERA -> CameraScreen(viewModel = viewModel, onNavigate = { viewModel.navigateTo(it) })
                    Screen.RECORTE -> RecorteScreen(viewModel = viewModel)
                    Screen.EDITOR -> EditorScreen(viewModel = viewModel, onNavigate = { viewModel.navigateTo(it) })
                    Screen.BIBLIOTECA -> BibliotecaScreen(viewModel = viewModel, onNavigate = { viewModel.navigateTo(it) })
                    Screen.PACOTES -> PacotesScreen(viewModel = viewModel, onNavigate = { viewModel.navigateTo(it) })
                    Screen.ASSINATURA -> AssinaturaScreen(viewModel = viewModel, onNavigate = { viewModel.navigateTo(it) })
                    Screen.CONFIGURACOES -> ConfiguracoesScreen(viewModel = viewModel, onNavigate = { viewModel.navigateTo(it) })
                }
            }
        }

        if (isProcessing) {
            ProcessingOverlay(message = processingMessage)
        }
    }
}

