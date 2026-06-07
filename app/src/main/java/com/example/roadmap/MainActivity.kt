package com.example.roadmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.roadmap.data.RoadmapGraph
import com.example.roadmap.data.ThemeMode
import com.example.roadmap.ui.RoadmapApp
import com.example.roadmap.ui.theme.RoadmapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = RoadmapGraph.repository(applicationContext)
        val themeStore = RoadmapGraph.themeStore(applicationContext)
        setContent {
            val mode by themeStore.mode.collectAsState()
            val dark = when (mode) {
                ThemeMode.System -> isSystemInDarkTheme()
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            RoadmapTheme(darkTheme = dark) {
                RoadmapApp(
                    repository = repository,
                    themeMode = mode,
                    onSetThemeMode = themeStore::setMode,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
