package com.example.roadmap.ui.detail

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

/** Write the JSON to a cache file and launch the Android share sheet (F9). */
fun shareRoadmapJson(context: Context, roadmapTitle: String, json: String) {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    val slug = roadmapTitle.lowercase()
        .replace(Regex("[^a-z0-9]+"), "-")
        .trim('-')
        .ifBlank { "roadmap" }
    val file = File(dir, "$slug.json").apply { writeText(json) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Export roadmap"))
}
