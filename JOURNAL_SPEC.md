# Journal - Sub-feature Spec (Roadmap Android app)

**Status:** Draft v1 - 2026-06-09
**Provenance:** Extracted from the pathforge web journal module, adapted as a module inside the local-only Roadmap Android app.
**Parent:** [PRODUCT_SPEC.md](./PRODUCT_SPEC.md) - inherits local-only/offline, no userId, Long/local-TZ, Kotlin + Compose + Material 3 + Room, reactive Flow, shared design tokens. This doc covers only what Journal adds.

## 1. What it is

One entry per calendar day, blending three uses in one record:
- **Reflection** - mood (1-5 + up to 3 tags) and an optional one-line summary.
- **Event log** - timestamped moments within the day, each starrable as important.
- **Goal-linked log** - references to the roadmaps/milestones the day is about.

It belongs in the Roadmap app because both live in the same on-device DB: a day can point at the very roadmap/milestone it describes, with no network. Journal is a second top-level destination. Linking is optional and one-directional (roadmaps hold no back-pointer in v1).

## 2. Non-goals (v1, faithful to pathforge)

No search, no charts/analytics/streaks, no attachments, no custom mood tags, no per-event editing (whole-day save only), no auto-save, no `job` references (this app has no jobs entity), no sync/accounts/network.

## 3. Features & requirements

| # | Feature | Rules |
|---|---------|-------|
| J1 | **Month heatmap** | Calendar grid; cells tinted by mood (1 red, 2 amber, 3 neutral, 4 emerald, 5 sky); month nav; tap a day to open (empty template if none). |
| J2 | **Day editor** | Loads the day or an empty draft; hosts J3-J7; one **Save** (enabled only when dirty) + **Delete** (confirm). Mood required to save. |
| J3 | **Mood** | `scale` 1-5 (required, emoji); `tags` up to 3 of {focused, tired, anxious, grateful, restless, excited, low, calm}. |
| J4 | **Summary** | Optional text, max 500. |
| J5 | **Events** | 0-20, ordered; each `text` (1-500), `important` flag, optional `time` free-text (max 20, e.g. "10am"). |
| J6 | **Links** | 0-10, ordered; `url` (http/https only) + optional `label`; tap opens browser. Shares the roadmap step-link model. |
| J7 | **References** | 0-10, ordered; roadmap or milestone, added via a searchable picker over this app's roadmaps/milestones; chips removable; deleted targets render "(deleted)"; tapping a live chip opens that roadmap. |
| J8 | **Nav entry** | Top-level destination; opens to today. |

## 4. Data model (Room additions)

Embedded Mongo arrays become relational child tables with `position` ordering and `ON DELETE CASCADE`. No userId. One entry per date.

**journal_day** - `id` PK; `date` (ISO YYYY-MM-DD, UNIQUE); `moodScale` (Int 1-5); `moodTags` (JSON list, <=3 enum); `summary` (String?, <=500); `createdAt`/`updatedAt` (Long).

**journal_event** - `id` PK; `dayId` FK->journal_day CASCADE; `text` (1-500); `important` (Bool); `time` (String?, <=20); `position` (Int). Cap 20.

**journal_link** - `id` PK; `dayId` FK CASCADE; `url` (http/https); `label` (String?); `position`. Cap 10.

**journal_reference** - `id` PK; `dayId` FK CASCADE; `type` (roadmap|milestone); `roadmapId` (Long, plain id - NOT a cascading FK); `milestoneId` (Long?, when milestone); `position`. Cap 10. Index `(roadmapId)`, `(milestoneId)` for future back-reference lookups.

Enums: `MoodTag` (8 values above); `RefType` (roadmap, milestone).

## 5. Logic

- **Mood color:** client maps scale 1-5 to overdue/amber/neutral/done/brand tokens.
- **Reference labels:** resolved at render by joining against local roadmap/milestone tables; missing target -> "(deleted)". No denormalized label stored.
- **Full-day upsert (atomic):** Save = one Room @Transaction: upsert journal_day by `date` (preserve createdAt, bump updatedAt), then delete+reinsert its events/links/references from the draft, assigning `position` by index. Mirrors pathforge's PUT-replace; no per-child mutations.
- **Dirty-check** drives the Save button. Reuse parent local-TZ date helpers (todayLocal, monthOf, month-grid, labels, shiftMonth).

## 6. Integration with the Roadmap app

- Added to top-level nav (Roadmaps, Journal); opens to today; month heatmap + day editor, responsive (stacked phone / side-by-side tablet) like roadmap detail.
- References picker queries the same Room `roadmap`/`milestone` tables - this is why journal lives in this app, not standalone.
- `journal_reference` ids are plain columns (no cascade), so deleting a roadmap/milestone leaves the row and the chip resolves to "(deleted)" - faithful to pathforge.
- Reuses parent link validation, design tokens, dark mode, and Flow-based UI.

## 7. Stack delta (over parent)

Month grid via Compose custom calendar; moodTags as a JSON column (kotlinx.serialization); references via a Room DAO over existing roadmap/milestone tables; reuse parent `java.time` LocalDate helpers. New packages: `data` (journal entities/DAOs/repo, full-day upsert), `ui.journal` (month grid, day editor, mood/event/link/reference editors), reusing shared link chips.

## 8. Adaptations (pathforge -> this module)

Shared adaptations carry over from the parent (no userId, relational tables + position, REST->repository ops, no auth, Flow). Journal-specific:

1. References roadmap|milestone only (dropped pathforge's `job` type - no jobs here).
2. Embedded day doc -> relational child tables + full-day upsert (preserves PUT-replace semantics).
3. References sourced from local Room tables (no server ownership check).
4. Deleted target -> "(deleted)" chip via non-cascading id columns + render-time join.
5. **[added]** Tapping a reference chip navigates to the roadmap/milestone (web chips don't) - easy to cut.
6. **[deferred]** pathforge's dashboard "today" quick-capture widget - the app has no dashboard yet.

## 9. Deferred

Back-reference view ("N entries reference this milestone"; indexes already support it); search; mood trends/charts/streaks; tag & reference filters (promote moodTags to a child table if added); reminders (WorkManager); attachments; JSON export/import (matching roadmap import/export); custom tags; "today" quick-capture if an overview screen is added.

## Appendix - JournalDay logical shape

```jsonc
{
  "date": "YYYY-MM-DD",                                   // unique, one per day
  "mood": { "scale": 1-5, "tags": ["focused","grateful"] }, // <=3 tags
  "summary": "optional, <=500",
  "events":     [ { "text": "1-500", "important": false, "time": "optional <=20" } ], // <=20
  "links":      [ { "url": "http/https", "label": "optional" } ],                     // <=10
  "references": [ { "type": "roadmap", "roadmapId": 12 },
                  { "type": "milestone", "roadmapId": 12, "milestoneId": 34 } ]       // <=10, no jobs
}
```
