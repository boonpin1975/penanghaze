package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.HazeViewModel
import com.example.ui.MainScreen
import com.example.ui.theme.PenangHazeTheme

class MainActivity : ComponentActivity() {

    private val viewModel: HazeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            PenangHazeTheme(darkTheme = state.isDarkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
