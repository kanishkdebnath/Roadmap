# Journal — PR 1: Data Layer — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the complete, JVM-tested journal data layer (4 Room tables, converters, additive migration, repository with full-day atomic upsert, mood color tokens, dirty-check) with **no UI** — the foundation for PRs 2–4.

**Architecture:** Mirrors the existing roadmap data layer. Journal code lives under `data/journal/` (entities, DAOs, read models, repository, enums, converters, draft DTO); the pure dirty-check goes in `domain/`; mood color tokens extend `ui/theme/`. `RoadmapDatabase` goes v1→v2 via an **additive** `Migration(1,2)` that preserves existing roadmaps. Wired through `RoadmapGraph` (no Hilt).

**Tech Stack:** Kotlin · Room 2.8.4 (KSP) · kotlinx.serialization · Coroutines/Flow · JUnit + Robolectric (`@Config(sdk=[34])`, in-memory Room). `java.time` for dates.

**Spec:** `docs/superpowers/specs/2026-06-09-journal-integration-design.md` (§3 data model, §4 repository, §5 domain, §7 tokens, §9 testing).

**Branch:** `feat/journal` (already created off `main`). This PR is the first of four; commit frequently, push and open a PR against `main` at the end.

**Build/test commands** (JAVA_HOME is set in `~/.zshenv` → Android Studio JBR; a fresh shell already has it):
- Compile: `./gradlew :app:compileDebugKotlin`
- All unit tests: `./gradlew :app:testDebugUnitTest`
- Single class: `./gradlew :app:testDebugUnitTest --tests "*JournalConvertersTest"`

---

## File map

**Create:**
- `data/journal/MoodTag.kt` — `@Serializable enum` (8 values)
- `data/journal/RefType.kt` — `enum { Roadmap, Milestone }`
- `data/journal/JournalConverters.kt` — Room `TypeConverter`s (moodTags JSON, RefType)
- `data/journal/entity/JournalDayEntity.kt`, `JournalEventEntity.kt`, `JournalLinkEntity.kt`, `JournalReferenceEntity.kt`
- `data/journal/relation/JournalDayWithChildren.kt` (+ `sorted()`), `DayMoodCell.kt`, `ResolvedReference.kt`, `RefTarget.kt`
- `data/journal/dao/JournalDayDao.kt`, `JournalEventDao.kt`, `JournalLinkDao.kt`, `JournalReferenceDao.kt`
- `data/journal/JournalDraft.kt` — `JournalDraft`, `EventDraft`, `ReferenceDraft`
- `data/journal/JournalRepository.kt`, `RoomJournalRepository.kt`
- `data/Migrations.kt` — `MIGRATION_1_2`
- `domain/JournalDirty.kt` — `normalized()`, `canSaveJournal()`
- Tests: `data/JournalConvertersTest.kt`, `data/JournalMigrationTest.kt`, `data/JournalDaoTest.kt`, `data/RoomJournalRepositoryTest.kt`, `domain/JournalDirtyTest.kt`, `ui/theme/MoodColorTest.kt`

**Modify:**
- `data/RoadmapDatabase.kt` — register 4 entities + 4 DAOs, `version = 2`, `exportSchema = true`, `@TypeConverters`
- `data/RoadmapGraph.kt` — `.addMigrations(MIGRATION_1_2)` + `journalRepository(context)` factory
- `app/build.gradle.kts` — `ksp { arg("room.schemaLocation", ...) }`
- `ui/theme/Color.kt` — amber/neutral/sky tokens (light + dark)
- `ui/theme/RoadmapColors.kt` — token fields + `moodAccent()`/`moodTint()`

---

## Task 1: Mood/Ref enums + Room converters

**Files:**
- Create: `app/src/main/java/com/example/roadmap/data/journal/MoodTag.kt`
- Create: `app/src/main/java/com/example/roadmap/data/journal/RefType.kt`
- Create: `app/src/main/java/com/example/roadmap/data/journal/JournalConverters.kt`
- Test: `app/src/test/java/com/example/roadmap/data/JournalConvertersTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.example.roadmap.data

import com.example.roadmap.data.journal.JournalConverters
import com.example.roadmap.data.journal.MoodTag
import com.example.roadmap.data.journal.RefType
import org.junit.Assert.assertEquals
import org.junit.Test

class JournalConvertersTest {
    private val c = JournalConverters()

    @Test fun moodTags_round_trip() {
        val tags = listOf(MoodTag.Focused, MoodTag.Grateful)
        assertEquals(tags, c.toMoodTags(c.fromMoodTags(tags)))
    }

    @Test fun empty_moodTags_round_trips_to_empty_list() {
        assertEquals(emptyList<MoodTag>(), c.toMoodTags(c.fromMoodTags(emptyList())))
    }

    @Test fun blank_json_decodes_to_empty_list() {
        assertEquals(emptyList<MoodTag>(), c.toMoodTags(""))
    }

    @Test fun refType_round_trip() {
        assertEquals(RefType.Milestone, c.toRefType(c.fromRefType(RefType.Milestone)))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*JournalConvertersTest"`
Expected: FAIL — unresolved references `JournalConverters`, `MoodTag`, `RefType`.

- [ ] **Step 3: Write minimal implementation**

`MoodTag.kt`:
```kotlin
package com.example.roadmap.data.journal

import kotlinx.serialization.Serializable

/** Fixed mood tag vocabulary (spec J3). Display labels are lowercased in the UI. */
@Serializable
enum class MoodTag { Focused, Tired, Anxious, Grateful, Restless, Excited, Low, Calm }
```

`RefType.kt`:
```kotlin
package com.example.roadmap.data.journal

/** A journal reference points at a roadmap or one of its milestones (spec J7). */
enum class RefType { Roadmap, Milestone }
```

`JournalConverters.kt`:
```kotlin
package com.example.roadmap.data.journal

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Room converters: moodTags as a JSON array, RefType as its name. */
class JournalConverters {
    @TypeConverter fun fromMoodTags(tags: List<MoodTag>): String = Json.encodeToString(tags)
    @TypeConverter fun toMoodTags(json: String): List<MoodTag> =
        if (json.isBlank()) emptyList() else Json.decodeFromString(json)

    @TypeConverter fun fromRefType(type: RefType): String = type.name
    @TypeConverter fun toRefType(value: String): RefType = RefType.valueOf(value)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*JournalConvertersTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/roadmap/data/journal/MoodTag.kt \
        app/src/main/java/com/example/roadmap/data/journal/RefType.kt \
        app/src/main/java/com/example/roadmap/data/journal/JournalConverters.kt \
        app/src/test/java/com/example/roadmap/data/JournalConvertersTest.kt
git commit -m "data: journal MoodTag/RefType enums + Room converters"
```

---

## Task 2: Journal entities

No behavior yet — entities are validated by the DAO/migration tests in later tasks. This task creates the four tables and verifies they compile.

**Files:**
- Create: `app/src/main/java/com/example/roadmap/data/journal/entity/JournalDayEntity.kt`
- Create: `app/src/main/java/com/example/roadmap/data/journal/entity/JournalEventEntity.kt`
- Create: `app/src/main/java/com/example/roadmap/data/journal/entity/JournalLinkEntity.kt`
- Create: `app/src/main/java/com/example/roadmap/data/journal/entity/JournalReferenceEntity.kt`

- [ ] **Step 1: Create `JournalDayEntity.kt`**

```kotlin
package com.example.roadmap.data.journal.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.roadmap.data.journal.MoodTag

@Entity(tableName = "journal_day", indices = [Index(value = ["date"], unique = true)])
data class JournalDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,                       // ISO YYYY-MM-DD, unique (one entry per day)
    val moodScale: Int,                     // 1..5
    val moodTags: List<MoodTag> = emptyList(),   // JSON column via converter, <= 3
    val summary: String? = null,            // <= 500
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
```

- [ ] **Step 2: Create `JournalEventEntity.kt`**

```kotlin
package com.example.roadmap.data.journal.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "journal_event",
    foreignKeys = [ForeignKey(
        entity = JournalDayEntity::class,
        parentColumns = ["id"], childColumns = ["dayId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("dayId", "position")],
)
data class JournalEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: Long,
    val text: String,                       // 1..500
    val important: Boolean = false,
    val time: String? = null,               // optional free text, <= 20
    val position: Int,
)
```

- [ ] **Step 3: Create `JournalLinkEntity.kt`**

```kotlin
package com.example.roadmap.data.journal.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "journal_link",
    foreignKeys = [ForeignKey(
        entity = JournalDayEntity::class,
        parentColumns = ["id"], childColumns = ["dayId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("dayId", "position")],
)
data class JournalLinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: Long,
    val url: String,                        // http/https only (validated in UI layer)
    val label: String? = null,
    val position: Int,
)
```

- [ ] **Step 4: Create `JournalReferenceEntity.kt`**

```kotlin
package com.example.roadmap.data.journal.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.roadmap.data.journal.RefType

/**
 * roadmapId / milestoneId are PLAIN columns (not FKs) so deleting a roadmap leaves the row;
 * the chip then resolves to "(deleted)" at render (spec §3).
 */
@Entity(
    tableName = "journal_reference",
    foreignKeys = [ForeignKey(
        entity = JournalDayEntity::class,
        parentColumns = ["id"], childColumns = ["dayId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("dayId"), Index("roadmapId"), Index("milestoneId")],
)
data class JournalReferenceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: Long,
    val type: RefType,
    val roadmapId: Long,
    val milestoneId: Long? = null,          // set when type == Milestone
    val position: Int,
)
```

- [ ] **Step 5: Verify compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: SUCCESS (entities not yet registered in the DB — they compile standalone).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/roadmap/data/journal/entity/
git commit -m "data: journal_day/event/link/reference entities"
```

---

## Task 3: Register entities + enable schema export (DB → v2)

**Files:**
- Modify: `app/src/main/java/com/example/roadmap/data/RoadmapDatabase.kt`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add the schema-export KSP arg to `app/build.gradle.kts`**

Add this top-level block immediately **after** the closing brace of the `android { … }` block (before `dependencies { … }`):

```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

- [ ] **Step 2: Update `RoadmapDatabase.kt` to register the journal tables, bump version, export schema, add converters**

Replace the file with:

```kotlin
package com.example.roadmap.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.roadmap.data.dao.LinkDao
import com.example.roadmap.data.dao.MilestoneDao
import com.example.roadmap.data.dao.RoadmapDao
import com.example.roadmap.data.dao.StepDao
import com.example.roadmap.data.entity.LinkEntity
import com.example.roadmap.data.entity.MilestoneEntity
import com.example.roadmap.data.entity.RoadmapEntity
import com.example.roadmap.data.entity.StepEntity
import com.example.roadmap.data.journal.JournalConverters
import com.example.roadmap.data.journal.entity.JournalDayEntity
import com.example.roadmap.data.journal.entity.JournalEventEntity
import com.example.roadmap.data.journal.entity.JournalLinkEntity
import com.example.roadmap.data.journal.entity.JournalReferenceEntity

@Database(
    entities = [
        RoadmapEntity::class, MilestoneEntity::class, StepEntity::class, LinkEntity::class,
        JournalDayEntity::class, JournalEventEntity::class, JournalLinkEntity::class, JournalReferenceEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(JournalConverters::class)
abstract class RoadmapDatabase : RoomDatabase() {
    abstract fun roadmapDao(): RoadmapDao
    abstract fun milestoneDao(): MilestoneDao
    abstract fun stepDao(): StepDao
    abstract fun linkDao(): LinkDao
    // journal DAO accessor methods are added in Task 5 (once those DAOs exist)
}
```

> The four journal entities are registered now, but their DAO accessor methods are added in Task 5. Room is happy to manage an entity's table without a corresponding DAO accessor, so the DB compiles and creates all eight tables. This keeps every task's build green.

- [ ] **Step 3: Verify compile (entities registered, journal DAOs not yet exposed)**

Run: `./gradlew :app:compileDebugKotlin`
Expected: SUCCESS. KSP generates `app/schemas/com.example.roadmap.data.RoadmapDatabase/2.json`.

- [ ] **Step 4: Confirm the generated schema contains the journal tables**

Run: `grep -o '"tableName": "journal_[a-z]*"' app/schemas/com.example.roadmap.data.RoadmapDatabase/2.json | sort -u`
Expected output (4 lines):
```
"tableName": "journal_day"
"tableName": "journal_event"
"tableName": "journal_link"
"tableName": "journal_reference"
```

- [ ] **Step 5: Commit**

```bash
git add app/build.gradle.kts \
        app/src/main/java/com/example/roadmap/data/RoadmapDatabase.kt \
        app/schemas/
git commit -m "data: register journal tables, bump Room to v2, export schema"
```

---

## Task 4: Additive Migration(1→2)

**Files:**
- Create: `app/src/main/java/com/example/roadmap/data/Migrations.kt`
- Modify: `app/src/main/java/com/example/roadmap/data/RoadmapGraph.kt`
- Test: `app/src/test/java/com/example/roadmap/data/JournalMigrationTest.kt`

> The CREATE statements below match Room's generated DDL for the Task 2 entities. **Verify them against the `createSql` fields in `app/schemas/.../2.json`** (Task 3 Step 4); if Room formatted anything differently, copy its exact strings. An exact match is required or the app crashes when it opens the migrated DB at v2.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.example.roadmap.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JournalMigrationTest {

    /** Opens an in-memory SQLite at v1 with just the roadmap table + one row, then runs the migration. */
    private fun v1WithOneRoadmap(): SupportSQLiteDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(null) // in-memory
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS `roadmap` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                                "`title` TEXT NOT NULL, `description` TEXT, `deadline` TEXT, `archived` INTEGER NOT NULL, " +
                                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
                        )
                        db.execSQL("INSERT INTO `roadmap` (`title`,`archived`,`createdAt`,`updatedAt`) VALUES ('Existing goal', 0, 1, 1)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        return helper.writableDatabase
    }

    @Test fun migration_1_2_adds_journal_tables_and_preserves_roadmaps() {
        val db = v1WithOneRoadmap()
        assertEquals(1, db.version)

        MIGRATION_1_2.migrate(db)

        // journal_day is usable
        db.execSQL("INSERT INTO `journal_day` (`date`,`moodScale`,`moodTags`,`createdAt`,`updatedAt`) VALUES ('2026-06-09', 4, '[]', 1, 1)")
        db.query("SELECT COUNT(*) FROM journal_day").use {
            it.moveToFirst(); assertEquals(1, it.getInt(0))
        }
        // a child table with the FK is usable
        db.execSQL("INSERT INTO `journal_event` (`dayId`,`text`,`important`,`position`) VALUES (1, 'hi', 0, 0)")
        db.query("SELECT COUNT(*) FROM journal_event").use {
            it.moveToFirst(); assertEquals(1, it.getInt(0))
        }
        // existing roadmap row preserved
        db.query("SELECT title FROM roadmap").use {
            it.moveToFirst(); assertEquals("Existing goal", it.getString(0))
        }
        db.close()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*JournalMigrationTest"`
Expected: FAIL — unresolved reference `MIGRATION_1_2`.

- [ ] **Step 3: Write `Migrations.kt`**

```kotlin
package com.example.roadmap.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Additive: creates the four journal tables. Existing roadmap data is untouched. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `journal_day` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`date` TEXT NOT NULL, `moodScale` INTEGER NOT NULL, `moodTags` TEXT NOT NULL, " +
                "`summary` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_journal_day_date` ON `journal_day` (`date`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `journal_event` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`dayId` INTEGER NOT NULL, `text` TEXT NOT NULL, `important` INTEGER NOT NULL, " +
                "`time` TEXT, `position` INTEGER NOT NULL, " +
                "FOREIGN KEY(`dayId`) REFERENCES `journal_day`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_event_dayId_position` ON `journal_event` (`dayId`, `position`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `journal_link` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`dayId` INTEGER NOT NULL, `url` TEXT NOT NULL, `label` TEXT, `position` INTEGER NOT NULL, " +
                "FOREIGN KEY(`dayId`) REFERENCES `journal_day`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_link_dayId_position` ON `journal_link` (`dayId`, `position`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `journal_reference` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`dayId` INTEGER NOT NULL, `type` TEXT NOT NULL, `roadmapId` INTEGER NOT NULL, " +
                "`milestoneId` INTEGER, `position` INTEGER NOT NULL, " +
                "FOREIGN KEY(`dayId`) REFERENCES `journal_day`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_reference_dayId` ON `journal_reference` (`dayId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_reference_roadmapId` ON `journal_reference` (`roadmapId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_journal_reference_milestoneId` ON `journal_reference` (`milestoneId`)")
    }
}
```

- [ ] **Step 4: Register the migration on the real DB builder in `RoadmapGraph.kt`**

In `RoadmapGraph.database(...)`, add `.addMigrations(MIGRATION_1_2)` to the builder chain:

```kotlin
            database ?: Room.databaseBuilder(
                context.applicationContext, RoadmapDatabase::class.java, "roadmap.db"
            ).addMigrations(MIGRATION_1_2).build().also { database = it }
```

- [ ] **Step 5: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*JournalMigrationTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/roadmap/data/Migrations.kt \
        app/src/main/java/com/example/roadmap/data/RoadmapGraph.kt \
        app/src/test/java/com/example/roadmap/data/JournalMigrationTest.kt
git commit -m "data: additive Migration(1->2) for journal tables + migration test"
```

---

## Task 5: DAOs + read models

**Files:**
- Create: `app/src/main/java/com/example/roadmap/data/journal/relation/JournalDayWithChildren.kt`
- Create: `app/src/main/java/com/example/roadmap/data/journal/relation/DayMoodCell.kt`
- Create: `app/src/main/java/com/example/roadmap/data/journal/relation/ResolvedReference.kt`
- Create: `app/src/main/java/com/example/roadmap/data/journal/relation/RefTarget.kt`
- Create: `app/src/main/java/com/example/roadmap/data/journal/dao/JournalDayDao.kt`
- Create: `app/src/main/java/com/example/roadmap/data/journal/dao/JournalEventDao.kt`
- Create: `app/src/main/java/com/example/roadmap/data/journal/dao/JournalLinkDao.kt`
- Create: `app/src/main/java/com/example/roadmap/data/journal/dao/JournalReferenceDao.kt`
- Modify: `app/src/main/java/com/example/roadmap/data/RoadmapDatabase.kt` (uncomment the journal DAO methods/imports from Task 3)
- Test: `app/src/test/java/com/example/roadmap/data/JournalDaoTest.kt`

- [ ] **Step 1: Create the read models**

`JournalDayWithChildren.kt`:
```kotlin
package com.example.roadmap.data.journal.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.roadmap.data.journal.entity.JournalDayEntity
import com.example.roadmap.data.journal.entity.JournalEventEntity
import com.example.roadmap.data.journal.entity.JournalLinkEntity
import com.example.roadmap.data.journal.entity.JournalReferenceEntity

data class JournalDayWithChildren(
    @Embedded val day: JournalDayEntity,
    @Relation(parentColumn = "id", entityColumn = "dayId") val events: List<JournalEventEntity>,
    @Relation(parentColumn = "id", entityColumn = "dayId") val links: List<JournalLinkEntity>,
    @Relation(parentColumn = "id", entityColumn = "dayId") val references: List<JournalReferenceEntity>,
)

/** Returns a copy with each child list ordered by its `position` (Room doesn't order @Relation lists). */
fun JournalDayWithChildren.sorted(): JournalDayWithChildren = copy(
    events = events.sortedBy { it.position },
    links = links.sortedBy { it.position },
    references = references.sortedBy { it.position },
)
```

`DayMoodCell.kt`:
```kotlin
package com.example.roadmap.data.journal.relation

/** Lightweight heatmap projection: which days have an entry, and their mood. */
data class DayMoodCell(val date: String, val moodScale: Int)
```

`ResolvedReference.kt`:
```kotlin
package com.example.roadmap.data.journal.relation

import androidx.room.Embedded
import com.example.roadmap.data.journal.entity.JournalReferenceEntity

/** A reference joined to its target's current title; null title => target was deleted. */
data class ResolvedReference(
    @Embedded val reference: JournalReferenceEntity,
    val resolvedTitle: String?,
)
```

`RefTarget.kt`:
```kotlin
package com.example.roadmap.data.journal.relation

import com.example.roadmap.data.journal.RefType

/** A pickable reference target (a roadmap or one of its milestones). */
data class RefTarget(
    val type: RefType,
    val roadmapId: Long,
    val milestoneId: Long?,
    val title: String,
)
```

- [ ] **Step 2: Create `JournalDayDao.kt`**

```kotlin
package com.example.roadmap.data.journal.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.roadmap.data.journal.entity.JournalDayEntity
import com.example.roadmap.data.journal.relation.DayMoodCell
import com.example.roadmap.data.journal.relation.JournalDayWithChildren
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDayDao {
    @Insert suspend fun insert(day: JournalDayEntity): Long
    @Update suspend fun update(day: JournalDayEntity)

    @Query("SELECT * FROM journal_day WHERE date = :date")
    suspend fun getByDate(date: String): JournalDayEntity?

    @Transaction
    @Query("SELECT * FROM journal_day WHERE date = :date")
    fun observeWithChildren(date: String): Flow<JournalDayWithChildren?>

    @Query("SELECT date, moodScale FROM journal_day WHERE date >= :start AND date < :end ORDER BY date ASC")
    fun observeMonth(start: String, end: String): Flow<List<DayMoodCell>>

    @Query("DELETE FROM journal_day WHERE date = :date")
    suspend fun deleteByDate(date: String)
}
```

- [ ] **Step 3: Create `JournalEventDao.kt` and `JournalLinkDao.kt`**

`JournalEventDao.kt`:
```kotlin
package com.example.roadmap.data.journal.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.roadmap.data.journal.entity.JournalEventEntity

@Dao
interface JournalEventDao {
    @Insert suspend fun insertAll(events: List<JournalEventEntity>)

    @Query("SELECT * FROM journal_event WHERE dayId = :dayId ORDER BY position ASC")
    suspend fun getByDay(dayId: Long): List<JournalEventEntity>

    @Query("DELETE FROM journal_event WHERE dayId = :dayId")
    suspend fun deleteByDay(dayId: Long)
}
```

`JournalLinkDao.kt`:
```kotlin
package com.example.roadmap.data.journal.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.roadmap.data.journal.entity.JournalLinkEntity

@Dao
interface JournalLinkDao {
    @Insert suspend fun insertAll(links: List<JournalLinkEntity>)

    @Query("SELECT * FROM journal_link WHERE dayId = :dayId ORDER BY position ASC")
    suspend fun getByDay(dayId: Long): List<JournalLinkEntity>

    @Query("DELETE FROM journal_link WHERE dayId = :dayId")
    suspend fun deleteByDay(dayId: Long)
}
```

- [ ] **Step 4: Create `JournalReferenceDao.kt`**

```kotlin
package com.example.roadmap.data.journal.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.roadmap.data.journal.entity.JournalReferenceEntity
import com.example.roadmap.data.journal.relation.RefTarget
import com.example.roadmap.data.journal.relation.ResolvedReference
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalReferenceDao {
    @Insert suspend fun insertAll(references: List<JournalReferenceEntity>)

    @Query("SELECT * FROM journal_reference WHERE dayId = :dayId ORDER BY position ASC")
    suspend fun getByDay(dayId: Long): List<JournalReferenceEntity>

    @Query("DELETE FROM journal_reference WHERE dayId = :dayId")
    suspend fun deleteByDay(dayId: Long)

    /** Resolve each reference's current target title (null => deleted). */
    @Query(
        """
        SELECT ref.*,
          CASE ref.type
            WHEN 'Roadmap'  THEN (SELECT title FROM roadmap   WHERE id = ref.roadmapId)
            WHEN 'Milestone' THEN (SELECT title FROM milestone WHERE id = ref.milestoneId)
          END AS resolvedTitle
        FROM journal_reference ref
        WHERE ref.dayId = :dayId
        ORDER BY ref.position ASC
        """
    )
    fun observeResolved(dayId: Long): Flow<List<ResolvedReference>>

    /** Searchable picker over the app's roadmaps + milestones. */
    @Query(
        """
        SELECT 'Roadmap' AS type, r.id AS roadmapId, NULL AS milestoneId, r.title AS title
          FROM roadmap r WHERE (:q = '' OR r.title LIKE '%' || :q || '%')
        UNION ALL
        SELECT 'Milestone' AS type, m.roadmapId AS roadmapId, m.id AS milestoneId, m.title AS title
          FROM milestone m WHERE (:q = '' OR m.title LIKE '%' || :q || '%')
        ORDER BY title ASC
        """
    )
    fun searchTargets(q: String): Flow<List<RefTarget>>
}
```

- [ ] **Step 5: Add the journal DAO accessor methods to `RoadmapDatabase.kt`**

Add the four imports alongside the existing journal imports:
```kotlin
import com.example.roadmap.data.journal.dao.JournalDayDao
import com.example.roadmap.data.journal.dao.JournalEventDao
import com.example.roadmap.data.journal.dao.JournalLinkDao
import com.example.roadmap.data.journal.dao.JournalReferenceDao
```

Replace the `// journal DAO accessor methods are added in Task 5` comment with the four abstract methods:
```kotlin
    abstract fun journalDayDao(): JournalDayDao
    abstract fun journalEventDao(): JournalEventDao
    abstract fun journalLinkDao(): JournalLinkDao
    abstract fun journalReferenceDao(): JournalReferenceDao
```

- [ ] **Step 6: Write the failing DAO test**

```kotlin
package com.example.roadmap.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.roadmap.data.entity.MilestoneEntity
import com.example.roadmap.data.entity.RoadmapEntity
import com.example.roadmap.data.journal.RefType
import com.example.roadmap.data.journal.entity.JournalDayEntity
import com.example.roadmap.data.journal.entity.JournalEventEntity
import com.example.roadmap.data.journal.entity.JournalReferenceEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class JournalDaoTest {
    private lateinit var db: RoadmapDatabase
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RoadmapDatabase::class.java
        ).build()
    }
    @After fun teardown() = db.close()

    @Test fun deleting_day_cascades_to_events() = runTest {
        val dayId = db.journalDayDao().insert(JournalDayEntity(date = "2026-06-09", moodScale = 4))
        db.journalEventDao().insertAll(listOf(JournalEventEntity(dayId = dayId, text = "e", position = 0)))
        db.journalDayDao().deleteByDate("2026-06-09")
        assertEquals(0, db.journalEventDao().getByDay(dayId).size)
    }

    @Test fun observeMonth_returns_only_days_in_range() = runTest {
        db.journalDayDao().insert(JournalDayEntity(date = "2026-05-31", moodScale = 1))
        db.journalDayDao().insert(JournalDayEntity(date = "2026-06-09", moodScale = 4))
        db.journalDayDao().insert(JournalDayEntity(date = "2026-07-01", moodScale = 5))
        val cells = db.journalDayDao().observeMonth("2026-06-01", "2026-07-01").first()
        assertEquals(listOf("2026-06-09"), cells.map { it.date })
        assertEquals(4, cells[0].moodScale)
    }

    @Test fun observeResolved_gives_title_then_null_after_target_deleted() = runTest {
        val rid = db.roadmapDao().insert(RoadmapEntity(title = "Kotlin"))
        val dayId = db.journalDayDao().insert(JournalDayEntity(date = "2026-06-09", moodScale = 4))
        db.journalReferenceDao().insertAll(
            listOf(JournalReferenceEntity(dayId = dayId, type = RefType.Roadmap, roadmapId = rid, position = 0))
        )
        assertEquals("Kotlin", db.journalReferenceDao().observeResolved(dayId).first()[0].resolvedTitle)

        db.roadmapDao().deleteById(rid)
        assertNull(db.journalReferenceDao().observeResolved(dayId).first()[0].resolvedTitle)
    }

    @Test fun searchTargets_matches_roadmaps_and_milestones() = runTest {
        val rid = db.roadmapDao().insert(RoadmapEntity(title = "Learn Compose"))
        db.milestoneDao().insert(MilestoneEntity(roadmapId = rid, title = "Compose Basics", position = 0))
        val titles = db.journalReferenceDao().searchTargets("Compose").first().map { it.title }
        assertEquals(listOf("Compose Basics", "Learn Compose"), titles)  // ORDER BY title ASC
    }
}
```

- [ ] **Step 7: Run test to verify it fails, then passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*JournalDaoTest"`
Expected: FAIL before Steps 1–5 are complete; PASS after.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/example/roadmap/data/journal/relation/ \
        app/src/main/java/com/example/roadmap/data/journal/dao/ \
        app/src/main/java/com/example/roadmap/data/RoadmapDatabase.kt \
        app/src/test/java/com/example/roadmap/data/JournalDaoTest.kt
git commit -m "data: journal DAOs + read models (relations, month, resolution, picker)"
```

---

## Task 6: JournalDraft DTO + repository (full-day upsert)

**Files:**
- Create: `app/src/main/java/com/example/roadmap/data/journal/JournalDraft.kt`
- Create: `app/src/main/java/com/example/roadmap/data/journal/JournalRepository.kt`
- Create: `app/src/main/java/com/example/roadmap/data/journal/RoomJournalRepository.kt`
- Test: `app/src/test/java/com/example/roadmap/data/RoomJournalRepositoryTest.kt`

- [ ] **Step 1: Create `JournalDraft.kt`**

```kotlin
package com.example.roadmap.data.journal

import com.example.roadmap.data.LinkDraft
import java.time.LocalDate

/** Editable shape of one day — the repository's saveDay input. Reuses LinkDraft from the roadmap layer. */
data class JournalDraft(
    val date: LocalDate,
    val moodScale: Int = 0,                 // 0 = unset; 1..5 once chosen
    val moodTags: List<MoodTag> = emptyList(),
    val summary: String? = null,
    val events: List<EventDraft> = emptyList(),
    val links: List<LinkDraft> = emptyList(),
    val references: List<ReferenceDraft> = emptyList(),
)

data class EventDraft(val text: String, val important: Boolean = false, val time: String? = null)

data class ReferenceDraft(val type: RefType, val roadmapId: Long, val milestoneId: Long? = null)
```

- [ ] **Step 2: Create `JournalRepository.kt` (interface)**

```kotlin
package com.example.roadmap.data.journal

import com.example.roadmap.data.journal.relation.DayMoodCell
import com.example.roadmap.data.journal.relation.JournalDayWithChildren
import com.example.roadmap.data.journal.relation.RefTarget
import com.example.roadmap.data.journal.relation.ResolvedReference
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

interface JournalRepository {
    fun observeMonth(month: YearMonth): Flow<List<DayMoodCell>>
    fun observeDay(date: LocalDate): Flow<JournalDayWithChildren?>     // children sorted by position
    fun observeResolvedReferences(dayId: Long): Flow<List<ResolvedReference>>
    fun searchReferenceTargets(query: String): Flow<List<RefTarget>>

    suspend fun saveDay(draft: JournalDraft)                          // one atomic transaction
    suspend fun deleteDay(date: LocalDate)
}
```

- [ ] **Step 3: Write the failing repository test**

```kotlin
package com.example.roadmap.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.roadmap.data.entity.RoadmapEntity
import com.example.roadmap.data.journal.EventDraft
import com.example.roadmap.data.journal.JournalDraft
import com.example.roadmap.data.journal.MoodTag
import com.example.roadmap.data.journal.RefType
import com.example.roadmap.data.journal.ReferenceDraft
import com.example.roadmap.data.journal.RoomJournalRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.YearMonth

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomJournalRepositoryTest {
    private lateinit var db: RoadmapDatabase
    private lateinit var repo: RoomJournalRepository
    private var clock = 1000L
    private val date = LocalDate.of(2026, 6, 9)

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RoadmapDatabase::class.java
        ).build()
        repo = RoomJournalRepository(db) { clock }
    }
    @After fun teardown() = db.close()

    @Test fun saveDay_inserts_day_with_sorted_children() = runTest {
        repo.saveDay(
            JournalDraft(
                date = date, moodScale = 4, moodTags = listOf(MoodTag.Focused),
                summary = "good day",
                events = listOf(EventDraft("standup", important = true, time = "10am"), EventDraft("ship")),
            )
        )
        val day = repo.observeDay(date).first()!!
        assertEquals(4, day.day.moodScale)
        assertEquals(listOf(MoodTag.Focused), day.day.moodTags)
        assertEquals(listOf("standup", "ship"), day.events.map { it.text })
        assertEquals(listOf(0, 1), day.events.map { it.position })
    }

    @Test fun saveDay_again_preserves_createdAt_bumps_updatedAt_and_replaces_children() = runTest {
        repo.saveDay(JournalDraft(date = date, moodScale = 3, events = listOf(EventDraft("first"))))
        val created = repo.observeDay(date).first()!!.day.createdAt

        clock = 2000L
        repo.saveDay(JournalDraft(date = date, moodScale = 5, events = listOf(EventDraft("second"))))
        val day = repo.observeDay(date).first()!!
        assertEquals(created, day.day.createdAt)      // preserved
        assertEquals(2000L, day.day.updatedAt)        // bumped
        assertEquals(5, day.day.moodScale)
        assertEquals(listOf("second"), day.events.map { it.text })   // replaced, not appended
    }

    @Test fun deleteDay_removes_entry() = runTest {
        repo.saveDay(JournalDraft(date = date, moodScale = 3))
        repo.deleteDay(date)
        assertNull(repo.observeDay(date).first())
    }

    @Test fun observeMonth_lists_days_of_that_month() = runTest {
        repo.saveDay(JournalDraft(date = LocalDate.of(2026, 6, 1), moodScale = 2))
        repo.saveDay(JournalDraft(date = LocalDate.of(2026, 6, 30), moodScale = 4))
        repo.saveDay(JournalDraft(date = LocalDate.of(2026, 7, 1), moodScale = 5))
        val cells = repo.observeMonth(YearMonth.of(2026, 6)).first()
        assertEquals(listOf("2026-06-01", "2026-06-30"), cells.map { it.date })
    }

    @Test fun reference_resolves_then_becomes_deleted() = runTest {
        val rid = db.roadmapDao().insert(RoadmapEntity(title = "Kotlin"))
        repo.saveDay(
            JournalDraft(date = date, moodScale = 4, references = listOf(ReferenceDraft(RefType.Roadmap, rid)))
        )
        val dayId = repo.observeDay(date).first()!!.day.id
        assertEquals("Kotlin", repo.observeResolvedReferences(dayId).first()[0].resolvedTitle)

        db.roadmapDao().deleteById(rid)
        assertNull(repo.observeResolvedReferences(dayId).first()[0].resolvedTitle)
    }
}
```

- [ ] **Step 4: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*RoomJournalRepositoryTest"`
Expected: FAIL — unresolved reference `RoomJournalRepository`.

- [ ] **Step 5: Write `RoomJournalRepository.kt`**

```kotlin
package com.example.roadmap.data.journal

import androidx.room.withTransaction
import com.example.roadmap.data.RoadmapDatabase
import com.example.roadmap.data.journal.entity.JournalDayEntity
import com.example.roadmap.data.journal.entity.JournalEventEntity
import com.example.roadmap.data.journal.entity.JournalLinkEntity
import com.example.roadmap.data.journal.entity.JournalReferenceEntity
import com.example.roadmap.data.journal.relation.DayMoodCell
import com.example.roadmap.data.journal.relation.JournalDayWithChildren
import com.example.roadmap.data.journal.relation.RefTarget
import com.example.roadmap.data.journal.relation.ResolvedReference
import com.example.roadmap.data.journal.relation.sorted
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth

class RoomJournalRepository(
    private val db: RoadmapDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) : JournalRepository {

    private val days = db.journalDayDao()
    private val events = db.journalEventDao()
    private val links = db.journalLinkDao()
    private val references = db.journalReferenceDao()

    override fun observeMonth(month: YearMonth): Flow<List<DayMoodCell>> =
        days.observeMonth(month.atDay(1).toString(), month.plusMonths(1).atDay(1).toString())

    override fun observeDay(date: LocalDate): Flow<JournalDayWithChildren?> =
        days.observeWithChildren(date.toString()).map { it?.sorted() }

    override fun observeResolvedReferences(dayId: Long): Flow<List<ResolvedReference>> =
        references.observeResolved(dayId)

    override fun searchReferenceTargets(query: String): Flow<List<RefTarget>> =
        references.searchTargets(query)

    /** Full-day atomic upsert (spec §4): upsert the day by date, then delete+reinsert all children. */
    override suspend fun saveDay(draft: JournalDraft) = db.withTransaction {
        val iso = draft.date.toString()
        val t = now()
        val existing = days.getByDate(iso)
        val dayId = if (existing == null) {
            days.insert(
                JournalDayEntity(
                    date = iso, moodScale = draft.moodScale, moodTags = draft.moodTags,
                    summary = draft.summary, createdAt = t, updatedAt = t,
                )
            )
        } else {
            days.update(
                existing.copy(
                    moodScale = draft.moodScale, moodTags = draft.moodTags,
                    summary = draft.summary, updatedAt = t,        // createdAt preserved
                )
            )
            existing.id
        }
        events.deleteByDay(dayId)
        links.deleteByDay(dayId)
        references.deleteByDay(dayId)
        events.insertAll(draft.events.mapIndexed { i, e ->
            JournalEventEntity(dayId = dayId, text = e.text, important = e.important, time = e.time, position = i)
        })
        links.insertAll(draft.links.mapIndexed { i, l ->
            JournalLinkEntity(dayId = dayId, url = l.url, label = l.label, position = i)
        })
        references.insertAll(draft.references.mapIndexed { i, r ->
            JournalReferenceEntity(dayId = dayId, type = r.type, roadmapId = r.roadmapId, milestoneId = r.milestoneId, position = i)
        })
    }

    override suspend fun deleteDay(date: LocalDate) = days.deleteByDate(date.toString())
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*RoomJournalRepositoryTest"`
Expected: PASS (5 tests).

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/roadmap/data/journal/JournalDraft.kt \
        app/src/main/java/com/example/roadmap/data/journal/JournalRepository.kt \
        app/src/main/java/com/example/roadmap/data/journal/RoomJournalRepository.kt \
        app/src/test/java/com/example/roadmap/data/RoomJournalRepositoryTest.kt
git commit -m "data: JournalRepository with full-day atomic upsert"
```

---

## Task 7: Wire the repository into RoadmapGraph

**Files:**
- Modify: `app/src/main/java/com/example/roadmap/data/RoadmapGraph.kt`

- [ ] **Step 1: Add the `journalRepository` factory**

Add the import and a factory method that reuses the shared database:

```kotlin
import com.example.roadmap.data.journal.JournalRepository
import com.example.roadmap.data.journal.RoomJournalRepository
```

```kotlin
    fun journalRepository(context: Context): JournalRepository = RoomJournalRepository(database(context))
```

(Place it next to the existing `repository(context)` method.)

- [ ] **Step 2: Verify compile**

Run: `./gradlew :app:compileDebugKotlin`
Expected: SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/roadmap/data/RoadmapGraph.kt
git commit -m "data: expose JournalRepository from RoadmapGraph"
```

---

## Task 8: Domain dirty-check

**Files:**
- Create: `app/src/main/java/com/example/roadmap/domain/JournalDirty.kt`
- Test: `app/src/test/java/com/example/roadmap/domain/JournalDirtyTest.kt`

> Scope note: `normalized()` caps mood tags at 3 (spec §3). The numeric caps for events (≤20), links (≤10), and references (≤10) are enforced as a UI affordance in PR 4 (the relevant "Add" button disables at the cap, per spec §8) — there is no DB-level constraint, so no data-layer guard is needed here.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.example.roadmap.domain

import com.example.roadmap.data.journal.EventDraft
import com.example.roadmap.data.journal.JournalDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class JournalDirtyTest {
    private val date = LocalDate.of(2026, 6, 9)

    @Test fun normalized_trims_summary_and_drops_blank_events_and_caps_tags() {
        val d = JournalDraft(
            date = date, moodScale = 4,
            summary = "  hi  ",
            events = listOf(EventDraft("  keep  "), EventDraft("   ")),
        ).normalized()
        assertEquals("hi", d.summary)
        assertEquals(listOf("keep"), d.events.map { it.text })
    }

    @Test fun whitespace_only_change_is_not_dirty() {
        val saved = JournalDraft(date = date, moodScale = 4, summary = "hi")
        val edited = saved.copy(summary = "hi   ")
        assertFalse(canSaveJournal(edited, saved))
    }

    @Test fun real_change_with_mood_is_saveable() {
        val saved = JournalDraft(date = date, moodScale = 4, summary = "hi")
        val edited = saved.copy(summary = "bye")
        assertTrue(canSaveJournal(edited, saved))
    }

    @Test fun new_day_needs_a_mood_to_be_saveable() {
        assertFalse(canSaveJournal(JournalDraft(date = date, moodScale = 0), saved = null))
        assertTrue(canSaveJournal(JournalDraft(date = date, moodScale = 3), saved = null))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "*JournalDirtyTest"`
Expected: FAIL — unresolved references `normalized`, `canSaveJournal`.

- [ ] **Step 3: Write `JournalDirty.kt`**

```kotlin
package com.example.roadmap.domain

import com.example.roadmap.data.journal.JournalDraft

/** Canonical form for fair comparison: trim text, drop blank children, cap tags at 3. */
fun JournalDraft.normalized(): JournalDraft = copy(
    summary = summary?.trim()?.ifBlank { null },
    moodTags = moodTags.distinct().take(3),
    events = events.mapNotNull { e ->
        e.text.trim().ifBlank { null }?.let { e.copy(text = it, time = e.time?.trim()?.ifBlank { null }) }
    },
    links = links.mapNotNull { l ->
        l.url.trim().ifBlank { null }?.let { l.copy(url = it, label = l.label?.trim()?.ifBlank { null }) }
    },
)

/** Save is allowed only when a mood (1..5) is set AND the day differs from what's stored. */
fun canSaveJournal(draft: JournalDraft, saved: JournalDraft?): Boolean =
    draft.moodScale in 1..5 && (saved == null || draft.normalized() != saved.normalized())
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*JournalDirtyTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/roadmap/domain/JournalDirty.kt \
        app/src/test/java/com/example/roadmap/domain/JournalDirtyTest.kt
git commit -m "domain: journal normalized() + canSaveJournal dirty-check"
```

---

## Task 9: Mood color tokens

**Files:**
- Modify: `app/src/main/java/com/example/roadmap/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/example/roadmap/ui/theme/RoadmapColors.kt`
- Test: `app/src/test/java/com/example/roadmap/ui/theme/MoodColorTest.kt`

- [ ] **Step 1: Add amber / neutral / sky tokens to `Color.kt`**

In `LightTokens`, add:
```kotlin
    val amber = Color(0xFFF59E0B)
    val amberContainer = Color(0xFFFCEFD2)
    val neutral = Color(0xFF94A3B8)
    val neutralContainer = Color(0xFFEEF1F0)
    val sky = Color(0xFF0EA5E9)
    val skyContainer = Color(0xFFE0F2FE)
```

In `DarkTokens`, add:
```kotlin
    val amber = Color(0xFFFBBF24)
    val amberContainer = Color(0xFF2C2510)
    val neutral = Color(0xFF93AB9D)
    val neutralContainer = Color(0xFF1B2A21)
    val sky = Color(0xFF38BDF8)
    val skyContainer = Color(0xFF0E2A38)
```

- [ ] **Step 2: Add fields + mood extensions to `RoadmapColors.kt`**

Add six parameters to the `RoadmapColors` class constructor (after `faint`):
```kotlin
    val amber: Color,
    val amberContainer: Color,
    val neutral: Color,
    val neutralContainer: Color,
    val sky: Color,
    val skyContainer: Color,
```

Add them to `LightRoadmapColors`:
```kotlin
    amber = LightTokens.amber,
    amberContainer = LightTokens.amberContainer,
    neutral = LightTokens.neutral,
    neutralContainer = LightTokens.neutralContainer,
    sky = LightTokens.sky,
    skyContainer = LightTokens.skyContainer,
```

Add them to `DarkRoadmapColors`:
```kotlin
    amber = DarkTokens.amber,
    amberContainer = DarkTokens.amberContainer,
    neutral = DarkTokens.neutral,
    neutralContainer = DarkTokens.neutralContainer,
    sky = DarkTokens.sky,
    skyContainer = DarkTokens.skyContainer,
```

Add the mood mapping extensions (after the `primaryBrush` extension), with an import `import androidx.compose.ui.graphics.Color` (already present):
```kotlin
/** Mood scale 1..5 → accent token (spec §7). 5 = sky (brand collides with mood-4 emerald). */
fun RoadmapColors.moodAccent(scale: Int): Color = when (scale) {
    1 -> overdue
    2 -> amber
    3 -> neutral
    4 -> done
    5 -> sky
    else -> neutral
}

/** Mood scale 1..5 → soft container tint used by the heatmap cells. */
fun RoadmapColors.moodTint(scale: Int): Color = when (scale) {
    1 -> overdueContainer
    2 -> amberContainer
    3 -> neutralContainer
    4 -> doneContainer
    5 -> skyContainer
    else -> neutralContainer
}
```

- [ ] **Step 3: Write the test**

```kotlin
package com.example.roadmap.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class MoodColorTest {
    @Test fun light_mood_tints_map_to_the_right_tokens() {
        assertEquals(LightTokens.overdueContainer, LightRoadmapColors.moodTint(1))
        assertEquals(LightTokens.amberContainer, LightRoadmapColors.moodTint(2))
        assertEquals(LightTokens.neutralContainer, LightRoadmapColors.moodTint(3))
        assertEquals(LightTokens.doneContainer, LightRoadmapColors.moodTint(4))
        assertEquals(LightTokens.skyContainer, LightRoadmapColors.moodTint(5))
    }

    @Test fun mood_accents_use_sky_for_five_and_amber_for_two() {
        assertEquals(DarkTokens.sky, DarkRoadmapColors.moodAccent(5))
        assertEquals(DarkTokens.amber, DarkRoadmapColors.moodAccent(2))
    }

    @Test fun out_of_range_scale_falls_back_to_neutral() {
        assertEquals(LightTokens.neutral, LightRoadmapColors.moodAccent(0))
        assertEquals(LightTokens.neutralContainer, LightRoadmapColors.moodTint(9))
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "*MoodColorTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/roadmap/ui/theme/Color.kt \
        app/src/main/java/com/example/roadmap/ui/theme/RoadmapColors.kt \
        app/src/test/java/com/example/roadmap/ui/theme/MoodColorTest.kt
git commit -m "ui: amber/neutral/sky mood tokens + moodAccent/moodTint"
```

---

## Definition of done (PR 1)

- [ ] Full suite green: `./gradlew :app:testDebugUnitTest`
- [ ] Debug APK builds: `./gradlew :app:assembleDebug`
- [ ] (Optional but recommended) Install on the emulator (`./gradlew :app:installDebug`) and confirm the app still launches and existing roadmaps are intact — this exercises the real `MIGRATION_1_2` on the on-device v1 DB.
- [ ] Push `feat/journal` and open a PR against `main`:

```bash
git push -u origin feat/journal
gh pr create --base main --title "Journal PR 1: data layer" \
  --body "Adds the journal data layer: 4 Room tables, converters, additive Migration(1->2), JournalRepository with full-day atomic upsert, mood color tokens, and the domain dirty-check. JVM/Robolectric tested. No UI yet (PRs 2-4). Spec: docs/superpowers/specs/2026-06-09-journal-integration-design.md"
```

**Next:** PR 2 (bottom-nav shell) gets its own plan. Note that `feat/journal` is based on `main` (no theme-toggle); the nav refactor in PR 2 must account for whichever of theme-toggle / journal merges to `main` first.
