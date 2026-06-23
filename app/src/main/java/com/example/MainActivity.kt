package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AutoViewModel
import com.example.ui.screens.AutoControlMainScreen
import com.example.ui.theme.AutoControlTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AutoControlTheme {
        val viewModel: AutoViewModel = viewModel()
        AutoControlMainScreen(viewModel = viewModel)
      }
    }
  }
}
