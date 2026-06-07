package com.example.roadmap.data

import com.example.roadmap.domain.ImportValidation
import com.example.roadmap.domain.validateImport
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val importJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
}

/** Parse pasted text into a [RoadmapDraft], tolerating ```fences and surrounding prose. */
fun parseRoadmapJson(raw: String): Result<RoadmapDraft> {
    val extracted = extractJsonObject(raw)
        ?: return Result.failure(IllegalArgumentException("No JSON object found"))
    return runCatching { importJson.decodeFromString<RoadmapDraft>(extracted) }
}

/** Strip a fenced code block if present, then take the first '{' .. last '}'. */
private fun extractJsonObject(raw: String): String? {
    var s = raw.trim()
    Regex("```(?:json|JSON)?\\s*([\\s\\S]*?)```").find(s)?.let { s = it.groupValues[1].trim() }
    val start = s.indexOf('{')
    val end = s.lastIndexOf('}')
    if (start == -1 || end == -1 || end < start) return null
    return s.substring(start, end + 1)
}

/** The copyable LLM prompt with the user's goal injected (spec §8). */
fun buildImportPrompt(goal: String): String {
    val g = goal.ifBlank { "<describe your goal>" }
    return """
        You are an expert planner. Create a structured roadmap as JSON for this goal:

        "$g"

        Return ONLY one JSON object — no markdown, no code fences, no commentary before or after.

        Shape:
        {
          "title": "string, 1-200 chars",
          "description": "string, optional",
          "deadline": "YYYY-MM-DD, optional",
          "milestones": [
            {
              "title": "string, 1-200 chars",
              "description": "string, optional",
              "deadline": "YYYY-MM-DD, optional",
              "steps": [
                { "title": "string, 1-200 chars", "completed": false,
                  "links": [ { "url": "https://...", "label": "string, optional" } ] }
              ]
            }
          ]
        }

        Rules:
        - Break the goal into 4-8 milestones, each with 3-8 concrete steps.
        - Every "title" is 1-200 characters.
        - "links" is optional; if present, each "url" MUST start with http:// or https://.
        - Set "completed" to false for every step.
        - Output the JSON object only.
    """.trimIndent()
}

/** Live preview for the dialog note: parse → validate. */
sealed interface ImportPreview {
    data object Empty : ImportPreview
    data class Invalid(val reason: String) : ImportPreview
    data class Valid(val draft: RoadmapDraft, val milestones: Int, val steps: Int) : ImportPreview
}

fun previewImport(raw: String): ImportPreview {
    if (raw.isBlank()) return ImportPreview.Empty
    val draft = parseRoadmapJson(raw).getOrElse {
        return ImportPreview.Invalid("Couldn't read the JSON — paste the whole object the LLM returned.")
    }
    return when (val v = validateImport(draft)) {
        is ImportValidation.Valid -> ImportPreview.Valid(
            draft, draft.milestones.size, draft.milestones.sumOf { it.steps.size },
        )
        is ImportValidation.Invalid -> ImportPreview.Invalid(v.reason)
    }
}
