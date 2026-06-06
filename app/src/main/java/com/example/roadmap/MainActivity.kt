package com.example.roadmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.roadmap.ui.catalog.DesignSystemCatalog
import com.example.roadmap.ui.theme.RoadmapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoadmapTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { inner ->
                    DesignSystemCatalog(Modifier.padding(inner))
                }
            }
        }
    }
}
