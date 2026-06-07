package com.example.roadmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.roadmap.data.RoadmapGraph
import com.example.roadmap.ui.RoadmapApp
import com.example.roadmap.ui.theme.RoadmapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = RoadmapGraph.repository(applicationContext)
        setContent {
            RoadmapTheme { RoadmapApp(repository, Modifier.fillMaxSize()) }
        }
    }
}
