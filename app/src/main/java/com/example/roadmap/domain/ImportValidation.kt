package com.example.roadmap.domain

import com.example.roadmap.data.RoadmapDraft

/** Result of validating a pasted import payload against the bulk schema (spec §8). */
sealed interface ImportValidation {
    data object Valid : ImportValidation
    data class Invalid(val reason: String) : ImportValidation
}

private const val MAX_TITLE = 200
private const val MAX_DESCRIPTION = 2000
private const val MAX_MILESTONES = 100
private const val MAX_STEPS = 200
private const val MAX_LINKS = 20

private fun titleValid(t: String) = t.length in 1..MAX_TITLE
private fun descriptionValid(d: String?) = d == null || d.length <= MAX_DESCRIPTION
private fun urlValid(u: String) = u.startsWith("http://") || u.startsWith("https://")

fun validateImport(draft: RoadmapDraft): ImportValidation {
    if (!titleValid(draft.title)) return ImportValidation.Invalid("Roadmap title must be 1–$MAX_TITLE characters.")
    if (!descriptionValid(draft.description)) return ImportValidation.Invalid("Roadmap description must be ≤$MAX_DESCRIPTION characters.")
    if (draft.milestones.size > MAX_MILESTONES) return ImportValidation.Invalid("A roadmap may have at most $MAX_MILESTONES milestones.")

    draft.milestones.forEachIndexed { mi, m ->
        val mLabel = "Milestone ${mi + 1}"
        if (!titleValid(m.title)) return ImportValidation.Invalid("$mLabel title must be 1–$MAX_TITLE characters.")
        if (!descriptionValid(m.description)) return ImportValidation.Invalid("$mLabel description must be ≤$MAX_DESCRIPTION characters.")
        if (m.steps.size > MAX_STEPS) return ImportValidation.Invalid("$mLabel may have at most $MAX_STEPS steps.")

        m.steps.forEachIndexed { si, s ->
            val sLabel = "Step ${si + 1} in $mLabel"
            if (!titleValid(s.title)) return ImportValidation.Invalid("$sLabel title must be 1–$MAX_TITLE characters.")
            if (s.links.size > MAX_LINKS) return ImportValidation.Invalid("$sLabel may have at most $MAX_LINKS links.")
            s.links.forEach { l ->
                if (!urlValid(l.url)) return ImportValidation.Invalid("$sLabel has a link that is not http:// or https://: '${l.url}'.")
            }
        }
    }
    return ImportValidation.Valid
}
