package com.example.graphicspoc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.graphicspoc.ui.CaptionScreen
import com.example.graphicspoc.ui.theme.GraphicsPOCTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GraphicsPOCTheme {
                CaptionScreen()
            }
        }
    }
}
