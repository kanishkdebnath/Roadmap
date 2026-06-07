package com.example.roadmap.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportJsonTest {
    private val sample = """
        {"title":"Learn Rust","milestones":[
          {"title":"Ownership","steps":[
            {"title":"Read Ch.4","links":[{"url":"https://doc.rust-lang.org"}]}]}]}
    """.trimIndent()

    @Test fun parses_clean_object() {
        val d = parseRoadmapJson(sample).getOrThrow()
        assertEquals("Learn Rust", d.title)
        assertEquals(1, d.milestones.size)
        assertEquals("Read Ch.4", d.milestones[0].steps[0].title)
        assertEquals("https://doc.rust-lang.org", d.milestones[0].steps[0].links[0].url)
    }

    @Test fun strips_markdown_fence() {
        val fenced = "```json\n$sample\n```"
        assertEquals("Learn Rust", parseRoadmapJson(fenced).getOrThrow().title)
    }

    @Test fun strips_surrounding_prose() {
        val prose = "Sure! Here is your roadmap:\n$sample\nHope it helps."
        assertEquals("Learn Rust", parseRoadmapJson(prose).getOrThrow().title)
    }

    @Test fun ignores_unknown_keys() {
        val extra = """{"title":"X","extra":42,"milestones":[]}"""
        assertEquals("X", parseRoadmapJson(extra).getOrThrow().title)
    }

    @Test fun applies_completed_default() {
        val d = parseRoadmapJson("""{"title":"X","milestones":[{"title":"M","steps":[{"title":"S"}]}]}""").getOrThrow()
        assertFalse(d.milestones[0].steps[0].completed)
    }

    @Test fun malformed_json_fails() {
        assertTrue(parseRoadmapJson("{ not json ").isFailure)
        assertTrue(parseRoadmapJson("no braces here").isFailure)
        assertTrue(parseRoadmapJson("   ").isFailure)
    }

    @Test fun prompt_contains_goal_and_schema() {
        val p = buildImportPrompt("Learn Rust")
        assertTrue(p.contains("Learn Rust"))
        assertTrue(p.contains("\"milestones\""))
        assertTrue(p.contains("http://"))
    }

    @Test fun prompt_blank_goal_uses_placeholder() {
        assertTrue(buildImportPrompt("   ").contains("<describe your goal>"))
    }

    @Test fun preview_empty_when_blank() {
        assertEquals(ImportPreview.Empty, previewImport("   "))
    }

    @Test fun preview_invalid_on_bad_json() {
        assertTrue(previewImport("not json at all") is ImportPreview.Invalid)
    }

    @Test fun preview_invalid_on_non_http_link() {
        val bad = """{"title":"X","milestones":[{"title":"M","steps":[{"title":"S","links":[{"url":"ftp://x"}]}]}]}"""
        assertTrue(previewImport(bad) is ImportPreview.Invalid)
    }

    @Test fun preview_valid_with_counts() {
        val json = """
            {"title":"G","milestones":[
              {"title":"M1","steps":[{"title":"a"},{"title":"b"}]},
              {"title":"M2","steps":[{"title":"c"}]},
              {"title":"M3","steps":[]}]}
        """.trimIndent()
        val p = previewImport(json) as ImportPreview.Valid
        assertEquals(3, p.milestones)
        assertEquals(3, p.steps)
    }
}
