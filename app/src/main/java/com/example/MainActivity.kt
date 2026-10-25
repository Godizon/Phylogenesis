package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.model.AppScreen
import com.example.ui.PhylogenesisScreen
import com.example.ui.startmenu.StartMenuScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PixelVoid
import com.example.viewmodel.PhylogenesisViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PhylogenesisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PixelVoid
                ) {
                    val currentScreen by viewModel.currentScreen.collectAsState()
                    val settings by viewModel.gameSettings.collectAsState()

                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "screen_routing"
                    ) { screen ->
                        when (screen) {
                            AppScreen.START_MENU -> {
                                StartMenuScreen(
                                    onContinue = { viewModel.navigateToScreen(AppScreen.SIMULATION) },
                                    onStartTutorial = { viewModel.startTutorial() },
                                    onGenerateWorld = { seed, star, preset ->
                                        viewModel.generateNewWorld(seed, star, preset)
                                    },
                                    settings = settings,
                                    onUpdateSettings = { viewModel.updateSettings(it) }
                                )
                            }
                            AppScreen.SIMULATION -> {
                                BackHandler {
                                    viewModel.navigateToScreen(AppScreen.START_MENU)
                                }
                                PhylogenesisScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
