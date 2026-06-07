package com.example.roadmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.roadmap.data.RoadmapGraph
import com.example.roadmap.ui.list.RoadmapListRoute
import com.example.roadmap.ui.list.RoadmapListViewModel
import com.example.roadmap.ui.list.RoadmapListViewModelFactory
import com.example.roadmap.ui.theme.RoadmapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = RoadmapGraph.repository(applicationContext)
        setContent {
            RoadmapTheme {
                val vm: RoadmapListViewModel = viewModel(factory = RoadmapListViewModelFactory(repository))
                RoadmapListRoute(
                    viewModel = vm,
                    onOpenRoadmap = { /* TODO Phase 5: navigate to detail */ },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
