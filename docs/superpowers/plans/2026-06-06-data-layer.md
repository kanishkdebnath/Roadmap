# Data Layer Implementation Plan (Phase 2)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist the Roadmap tree (roadmap → milestones → steps → links) in Room with cascade deletes, explicit `position` ordering, derived milestone completion, and a reactive `RoadmapRepository` — all JVM-unit-tested.

**Architecture:** Relational Room schema (4 entities, `ON DELETE CASCADE` FKs, `(parent, position)` indices per `PRODUCT_SPEC.md` §6). DAOs expose suspend writes + `Flow` reads. A nested `@Relation` POJO (`RoadmapWithChildren`) is the read model; the repository sorts children by `position`. Pure helpers (`Completion`, `Reorder`) hold the write-path logic the repository depends on. `RoadmapRepository` (interface + Room impl) injects a `now: () -> Long` clock for deterministic tests and wraps multi-step writes in transactions.

**Tech Stack (PROVEN — already on the branch, do not re-derive):** Room 2.8.4 (KSP 2.2.10-2.0.2), `androidx.room:room-ktx` for `withTransaction`, kotlinx-coroutines, JUnit4. **Tests run on the JVM under Robolectric** (`@RunWith(RobolectricTestRunner::class) @Config(sdk = [34])`, `Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), …)`), verified by a passing smoke test. `gradle.properties` has `android.disallowKotlinSourceSets=false` (AGP 9 built-in Kotlin + KSP).

**Conventions:**
- Branch: `feat/data-layer` (already created; the build harness commit `7b52d5c` is already here). Stay on it; never `main`.
- Package: `com.example.roadmap`. `data/entity`, `data/dao`, `data/relation`, `data/` (db + repo), `domain/` (pure helpers).
- Run tests: `./gradlew :app:testDebugUnitTest`. Compile: `./gradlew :app:compileDebugKotlin`. Full: `./gradlew :app:assembleDebug`.
- Commit after each task. Entities are Room `@Entity` data classes suffixed `Entity`.
- The repo currently has a **minimal** `RoadmapEntity(id, title)`, `RoadmapDao(insert, getAll)`, `RoadmapDatabase`, and `RoadmapDatabaseSmokeTest` from the harness commit — Tasks 1–3 expand these into the real schema; keep the smoke test compiling (or let Task 2 replace it with real DAO tests).

---

## File structure (this phase)

```
app/build.gradle.kts                                   (modify: add room-ktx, coroutines if needed)
app/src/main/java/com/example/roadmap/
  data/entity/RoadmapEntity.kt   MilestoneEntity.kt  StepEntity.kt  LinkEntity.kt
  data/dao/RoadmapDao.kt  MilestoneDao.kt  StepDao.kt  LinkDao.kt
  data/relation/RoadmapWithChildren.kt        (nested @Relation POJOs + sorted() extension + Drafts)
  data/RoadmapDatabase.kt                      (modify: 4 entities, version 1)
  data/RoadmapRepository.kt                    (interface + Draft models)
  data/RoomRoadmapRepository.kt                (Room impl)
  data/RoadmapGraph.kt                         (minimal DB + repo provider; no Hilt)
  domain/Completion.kt   domain/Reorder.kt     (pure helpers)
app/src/test/java/com/example/roadmap/
  domain/CompletionTest.kt   domain/ReorderTest.kt          (plain JVM)
  data/RoadmapDaoTest.kt  RelationTest.kt  CascadeTest.kt   (Robolectric)
  data/RoomRoadmapRepositoryTest.kt                          (Robolectric)
```

---

## Task 1: Full entities

**Files:** Replace `data/entity/RoadmapEntity.kt`; Create `MilestoneEntity.kt`, `StepEntity.kt`, `LinkEntity.kt`; Modify `data/RoadmapDatabase.kt`.

- [ ] **Step 1: Replace RoadmapEntity.kt**
```kotlin
package com.example.roadmap.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "roadmap", indices = [Index("archived", "updatedAt")])
data class RoadmapEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String? = null,
    val deadline: String? = null,          // ISO-8601 date YYYY-MM-DD
    val archived: Boolean = false,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
```

- [ ] **Step 2: Create MilestoneEntity.kt**
```kotlin
package com.example.roadmap.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "milestone",
    foreignKeys = [ForeignKey(
        entity = RoadmapEntity::class,
        parentColumns = ["id"], childColumns = ["roadmapId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("roadmapId", "position")],
)
data class MilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roadmapId: Long,
    val title: String,
    val description: String? = null,
    val deadline: String? = null,
    val position: Int,
    val completedAt: Long? = null,         // derived; set when all steps complete
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
```

- [ ] **Step 3: Create StepEntity.kt**
```kotlin
package com.example.roadmap.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "step",
    foreignKeys = [ForeignKey(
        entity = MilestoneEntity::class,
        parentColumns = ["id"], childColumns = ["milestoneId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("milestoneId", "position")],
)
data class StepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val milestoneId: Long,
    val title: String,
    val completed: Boolean = false,
    val completedAt: Long? = null,
    val position: Int,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
```

- [ ] **Step 4: Create LinkEntity.kt**
```kotlin
package com.example.roadmap.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "link",
    foreignKeys = [ForeignKey(
        entity = StepEntity::class,
        parentColumns = ["id"], childColumns = ["stepId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("stepId", "position")],
)
data class LinkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stepId: Long,
    val url: String,                       // http/https only (validated in import, Phase 7)
    val label: String? = null,
    val position: Int,
)
```

- [ ] **Step 5: Update RoadmapDatabase.kt to register all 4 entities** (keep `version = 1`, `exportSchema = false`). The `entities` array becomes `[RoadmapEntity::class, MilestoneEntity::class, StepEntity::class, LinkEntity::class]`. Leave `roadmapDao()` for now (more DAO accessors added in Task 2).

- [ ] **Step 6: Verify build + smoke test still green**

Run: `./gradlew :app:testDebugUnitTest --tests "*RoadmapDatabaseSmokeTest"`
Expected: PASS (the smoke `RoadmapEntity(title = "Learn Rust")` still compiles — all new fields have defaults).

- [ ] **Step 7: Commit**
```bash
git add app/src/main/java/com/example/roadmap/data/entity app/src/main/java/com/example/roadmap/data/RoadmapDatabase.kt
git commit -m "data: full Room entities (roadmap/milestone/step/link, FK cascade, position indices)"
```

---

## Task 2: DAOs

**Files:** Replace `data/dao/RoadmapDao.kt`; Create `MilestoneDao.kt`, `StepDao.kt`, `LinkDao.kt`; Modify `RoadmapDatabase.kt` (add DAO accessors).

- [ ] **Step 1: Replace RoadmapDao.kt**
```kotlin
package com.example.roadmap.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.roadmap.data.entity.RoadmapEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RoadmapDao {
    @Insert suspend fun insert(roadmap: RoadmapEntity): Long
    @Update suspend fun update(roadmap: RoadmapEntity)

    @Query("SELECT * FROM roadmap WHERE archived = :archived ORDER BY updatedAt DESC")
    fun observeByArchived(archived: Boolean): Flow<List<RoadmapEntity>>

    @Query("SELECT * FROM roadmap WHERE id = :id")
    suspend fun getById(id: Long): RoadmapEntity?

    @Query("UPDATE roadmap SET archived = :archived, updatedAt = :now WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean, now: Long)

    @Query("DELETE FROM roadmap WHERE id = :id")
    suspend fun deleteById(id: Long)
}
```

- [ ] **Step 2: Create MilestoneDao.kt**
```kotlin
package com.example.roadmap.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.roadmap.data.entity.MilestoneEntity

@Dao
interface MilestoneDao {
    @Insert suspend fun insert(milestone: MilestoneEntity): Long
    @Update suspend fun update(milestone: MilestoneEntity)

    @Query("SELECT * FROM milestone WHERE roadmapId = :roadmapId ORDER BY position ASC")
    suspend fun getByRoadmap(roadmapId: Long): List<MilestoneEntity>

    @Query("SELECT * FROM milestone WHERE id = :id")
    suspend fun getById(id: Long): MilestoneEntity?

    @Query("SELECT COALESCE(MAX(position), -1) FROM milestone WHERE roadmapId = :roadmapId")
    suspend fun maxPosition(roadmapId: Long): Int

    @Query("UPDATE milestone SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)

    @Query("UPDATE milestone SET completedAt = :completedAt, updatedAt = :now WHERE id = :id")
    suspend fun setCompletedAt(id: Long, completedAt: Long?, now: Long)

    @Query("DELETE FROM milestone WHERE id = :id")
    suspend fun deleteById(id: Long)
}
```

- [ ] **Step 3: Create StepDao.kt**
```kotlin
package com.example.roadmap.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.roadmap.data.entity.StepEntity

@Dao
interface StepDao {
    @Insert suspend fun insert(step: StepEntity): Long
    @Update suspend fun update(step: StepEntity)

    @Query("SELECT * FROM step WHERE milestoneId = :milestoneId ORDER BY position ASC")
    suspend fun getByMilestone(milestoneId: Long): List<StepEntity>

    @Query("SELECT * FROM step WHERE id = :id")
    suspend fun getById(id: Long): StepEntity?

    @Query("SELECT COALESCE(MAX(position), -1) FROM step WHERE milestoneId = :milestoneId")
    suspend fun maxPosition(milestoneId: Long): Int

    @Query("UPDATE step SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Long, position: Int)

    @Query("DELETE FROM step WHERE id = :id")
    suspend fun deleteById(id: Long)
}
```

- [ ] **Step 4: Create LinkDao.kt**
```kotlin
package com.example.roadmap.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.roadmap.data.entity.LinkEntity

@Dao
interface LinkDao {
    @Insert suspend fun insertAll(links: List<LinkEntity>)

    @Query("SELECT * FROM link WHERE stepId = :stepId ORDER BY position ASC")
    suspend fun getByStep(stepId: Long): List<LinkEntity>

    @Query("DELETE FROM link WHERE stepId = :stepId")
    suspend fun deleteByStep(stepId: Long)
}
```

- [ ] **Step 5: Add DAO accessors to RoadmapDatabase.kt**
Add `abstract fun milestoneDao(): MilestoneDao`, `abstract fun stepDao(): StepDao`, `abstract fun linkDao(): LinkDao` (and keep `roadmapDao()`), with imports.

- [ ] **Step 6: Write a Robolectric DAO test** — Create `app/src/test/java/com/example/roadmap/data/RoadmapDaoTest.kt`:
```kotlin
package com.example.roadmap.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.roadmap.data.entity.RoadmapEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoadmapDaoTest {
    private lateinit var db: RoadmapDatabase
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RoadmapDatabase::class.java
        ).build()
    }
    @After fun teardown() = db.close()

    @Test fun observeByArchived_filters_and_orders_by_updatedAt_desc() = runTest {
        val dao = db.roadmapDao()
        dao.insert(RoadmapEntity(title = "A", archived = false, updatedAt = 100))
        dao.insert(RoadmapEntity(title = "B", archived = false, updatedAt = 200))
        dao.insert(RoadmapEntity(title = "Z", archived = true, updatedAt = 300))
        val active = dao.observeByArchived(false).first()
        assertEquals(listOf("B", "A"), active.map { it.title })
        assertEquals(listOf("Z"), dao.observeByArchived(true).first().map { it.title })
    }
}
```

- [ ] **Step 7: Run + verify**
Run: `./gradlew :app:testDebugUnitTest --tests "*RoadmapDaoTest"` → Expected: PASS.

- [ ] **Step 8: Commit**
```bash
git add app/src/main/java/com/example/roadmap/data/dao app/src/main/java/com/example/roadmap/data/RoadmapDatabase.kt app/src/test/java/com/example/roadmap/data/RoadmapDaoTest.kt
git commit -m "data: DAOs for roadmap/milestone/step/link with Flow reads and reorder support"
```

---

## Task 3: Relation read model + nested observe

**Files:** Create `data/relation/RoadmapWithChildren.kt`; Modify `RoadmapDao.kt` (add the relation query); Test `RelationTest.kt`.

- [ ] **Step 1: Create RoadmapWithChildren.kt** (nested POJOs + a `sorted()` extension; Room `@Relation` does not order, so we sort by `position` after load)
```kotlin
package com.example.roadmap.data.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.example.roadmap.data.entity.LinkEntity
import com.example.roadmap.data.entity.MilestoneEntity
import com.example.roadmap.data.entity.RoadmapEntity
import com.example.roadmap.data.entity.StepEntity

data class StepWithLinks(
    @Embedded val step: StepEntity,
    @Relation(parentColumn = "id", entityColumn = "stepId") val links: List<LinkEntity>,
)

data class MilestoneWithSteps(
    @Embedded val milestone: MilestoneEntity,
    @Relation(entity = StepEntity::class, parentColumn = "id", entityColumn = "milestoneId")
    val steps: List<StepWithLinks>,
)

data class RoadmapWithChildren(
    @Embedded val roadmap: RoadmapEntity,
    @Relation(entity = MilestoneEntity::class, parentColumn = "id", entityColumn = "roadmapId")
    val milestones: List<MilestoneWithSteps>,
)

/** Returns a copy with every level ordered by its `position`. */
fun RoadmapWithChildren.sorted(): RoadmapWithChildren = copy(
    milestones = milestones.sortedBy { it.milestone.position }.map { m ->
        m.copy(steps = m.steps.sortedBy { it.step.position }.map { s ->
            s.copy(links = s.links.sortedBy { it.position })
        })
    },
)
```

- [ ] **Step 2: Add the nested observe to RoadmapDao.kt**
Add imports `androidx.room.Transaction` and `com.example.roadmap.data.relation.RoadmapWithChildren`, then:
```kotlin
    @Transaction
    @Query("SELECT * FROM roadmap WHERE id = :id")
    fun observeWithChildren(id: Long): Flow<RoadmapWithChildren?>
```

- [ ] **Step 3: Test the nested read + sorting** — Create `app/src/test/java/com/example/roadmap/data/RelationTest.kt`:
```kotlin
package com.example.roadmap.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.roadmap.data.entity.LinkEntity
import com.example.roadmap.data.entity.MilestoneEntity
import com.example.roadmap.data.entity.RoadmapEntity
import com.example.roadmap.data.entity.StepEntity
import com.example.roadmap.data.relation.sorted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RelationTest {
    private lateinit var db: RoadmapDatabase
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RoadmapDatabase::class.java
        ).build()
    }
    @After fun teardown() = db.close()

    @Test fun nested_tree_loads_and_sorts_by_position() = runTest {
        val rid = db.roadmapDao().insert(RoadmapEntity(title = "Goal"))
        val mid = db.milestoneDao().insert(MilestoneEntity(roadmapId = rid, title = "M1", position = 0))
        // insert steps out of position order to prove sorting
        val s2 = db.stepDao().insert(StepEntity(milestoneId = mid, title = "second", position = 1))
        val s1 = db.stepDao().insert(StepEntity(milestoneId = mid, title = "first", position = 0))
        db.linkDao().insertAll(listOf(LinkEntity(stepId = s1, url = "https://a", position = 0)))

        val tree = db.roadmapDao().observeWithChildren(rid).first()!!.sorted()
        assertEquals("Goal", tree.roadmap.title)
        assertEquals(1, tree.milestones.size)
        assertEquals(listOf("first", "second"), tree.milestones[0].steps.map { it.step.title })
        assertEquals(listOf("https://a"), tree.milestones[0].steps[0].links.map { it.url })
    }
}
```

- [ ] **Step 4: Run + verify** — `./gradlew :app:testDebugUnitTest --tests "*RelationTest"` → PASS.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/roadmap/data/relation app/src/main/java/com/example/roadmap/data/dao/RoadmapDao.kt app/src/test/java/com/example/roadmap/data/RelationTest.kt
git commit -m "data: RoadmapWithChildren @Relation read model with position sorting"
```

---

## Task 4: Cascade delete test

**Files:** Test `app/src/test/java/com/example/roadmap/data/CascadeTest.kt`.

- [ ] **Step 1: Write the cascade test** (Room enables FK enforcement by default, so deleting a roadmap must remove descendants)
```kotlin
package com.example.roadmap.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.roadmap.data.entity.LinkEntity
import com.example.roadmap.data.entity.MilestoneEntity
import com.example.roadmap.data.entity.RoadmapEntity
import com.example.roadmap.data.entity.StepEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CascadeTest {
    private lateinit var db: RoadmapDatabase
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RoadmapDatabase::class.java
        ).build()
    }
    @After fun teardown() = db.close()

    @Test fun deleting_roadmap_cascades_to_milestones_steps_links() = runTest {
        val rid = db.roadmapDao().insert(RoadmapEntity(title = "Goal"))
        val mid = db.milestoneDao().insert(MilestoneEntity(roadmapId = rid, title = "M", position = 0))
        val sid = db.stepDao().insert(StepEntity(milestoneId = mid, title = "S", position = 0))
        db.linkDao().insertAll(listOf(LinkEntity(stepId = sid, url = "https://a", position = 0)))

        db.roadmapDao().deleteById(rid)

        assertEquals(0, db.milestoneDao().getByRoadmap(rid).size)
        assertEquals(0, db.stepDao().getByMilestone(mid).size)
        assertEquals(0, db.linkDao().getByStep(sid).size)
    }
}
```

- [ ] **Step 2: Run + verify** — `./gradlew :app:testDebugUnitTest --tests "*CascadeTest"` → PASS. (If links/steps survive, FK enforcement is off — confirm Room's default `PRAGMA foreign_keys=ON` is active; with the standard builder it is.)

- [ ] **Step 3: Commit**
```bash
git add app/src/test/java/com/example/roadmap/data/CascadeTest.kt
git commit -m "test: verify FK cascade delete removes descendants"
```

---

## Task 5: Completion helper (pure, TDD)

**Files:** Create `domain/Completion.kt`; Test `domain/CompletionTest.kt`.

- [ ] **Step 1: Write the failing test** — `app/src/test/java/com/example/roadmap/domain/CompletionTest.kt`:
```kotlin
package com.example.roadmap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompletionTest {
    @Test fun all_complete_nonempty_returns_now() {
        assertEquals(42L, milestoneCompletedAt(listOf(true, true), now = 42L))
    }
    @Test fun any_incomplete_returns_null() {
        assertNull(milestoneCompletedAt(listOf(true, false), now = 42L))
    }
    @Test fun empty_returns_null() {
        assertNull(milestoneCompletedAt(emptyList(), now = 42L))
    }
}
```

- [ ] **Step 2: Run → FAIL** — `./gradlew :app:testDebugUnitTest --tests "*CompletionTest"` → unresolved `milestoneCompletedAt`.

- [ ] **Step 3: Create Completion.kt**
```kotlin
package com.example.roadmap.domain

/**
 * A milestone is complete iff it has >= 1 step and every step is completed (spec §7.1).
 * Returns `now` when newly complete, else null.
 */
fun milestoneCompletedAt(stepCompletions: List<Boolean>, now: Long): Long? =
    if (stepCompletions.isNotEmpty() && stepCompletions.all { it }) now else null
```

- [ ] **Step 4: Run → PASS.**

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/roadmap/domain/Completion.kt app/src/test/java/com/example/roadmap/domain/CompletionTest.kt
git commit -m "domain: milestone completion derivation (pure, tested)"
```

---

## Task 6: Reorder helper (pure, TDD)

**Files:** Create `domain/Reorder.kt`; Test `domain/ReorderTest.kt`.

- [ ] **Step 1: Write the failing test** — `app/src/test/java/com/example/roadmap/domain/ReorderTest.kt`:
```kotlin
package com.example.roadmap.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReorderTest {
    @Test fun valid_when_same_id_set() {
        assertTrue(isValidReorder(current = listOf(1L, 2L, 3L), requested = listOf(3L, 1L, 2L)))
    }
    @Test fun invalid_when_ids_added_or_dropped() {
        assertFalse(isValidReorder(listOf(1L, 2L), listOf(1L, 2L, 3L)))
        assertFalse(isValidReorder(listOf(1L, 2L, 3L), listOf(1L, 2L)))
    }
    @Test fun positions_are_contiguous_from_zero() {
        assertEquals(mapOf(3L to 0, 1L to 1, 2L to 2), positionsFor(listOf(3L, 1L, 2L)))
    }
}
```

- [ ] **Step 2: Run → FAIL.**

- [ ] **Step 3: Create Reorder.kt**
```kotlin
package com.example.roadmap.domain

/** A reorder request is valid iff it is a permutation of the existing sibling ids (spec §7.4). */
fun isValidReorder(current: List<Long>, requested: List<Long>): Boolean =
    current.size == requested.size && current.toSet() == requested.toSet()

/** Maps each id to its new 0-based contiguous position. */
fun positionsFor(orderedIds: List<Long>): Map<Long, Int> =
    orderedIds.withIndex().associate { (index, id) -> id to index }
```

- [ ] **Step 4: Run → PASS.**

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/example/roadmap/domain/Reorder.kt app/src/test/java/com/example/roadmap/domain/ReorderTest.kt
git commit -m "domain: reorder validation + contiguous positions (pure, tested)"
```

---

## Task 7: Repository (interface + Room impl)

**Files:** Modify `app/build.gradle.kts` (add room-ktx); Create `data/RoadmapRepository.kt`, `data/RoomRoadmapRepository.kt`; Test `data/RoomRoadmapRepositoryTest.kt`.

- [ ] **Step 1: Add room-ktx for `withTransaction`**
In `gradle/libs.versions.toml` `[libraries]` add:
```toml
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
```
In `app/build.gradle.kts` dependencies add `implementation(libs.androidx.room.ktx)` next to `room.runtime`. Run `./gradlew :app:compileDebugKotlin` to confirm it resolves.

- [ ] **Step 2: Create RoadmapRepository.kt (interface + Draft models)**
```kotlin
package com.example.roadmap.data

import com.example.roadmap.data.entity.RoadmapEntity
import com.example.roadmap.data.relation.RoadmapWithChildren
import kotlinx.coroutines.flow.Flow

/** Plain models for atomic bulk import (Phase 7 maps validated JSON to these). */
data class LinkDraft(val url: String, val label: String? = null)
data class StepDraft(val title: String, val completed: Boolean = false, val links: List<LinkDraft> = emptyList())
data class MilestoneDraft(val title: String, val description: String? = null, val deadline: String? = null, val steps: List<StepDraft> = emptyList())
data class RoadmapDraft(val title: String, val description: String? = null, val deadline: String? = null, val milestones: List<MilestoneDraft> = emptyList())

interface RoadmapRepository {
    fun observeRoadmaps(archived: Boolean): Flow<List<RoadmapEntity>>
    fun observeRoadmap(id: Long): Flow<RoadmapWithChildren?>          // children sorted by position

    suspend fun createRoadmap(title: String, description: String? = null, deadline: String? = null): Long
    suspend fun updateRoadmap(id: Long, title: String, description: String?, deadline: String?)
    suspend fun setArchived(id: Long, archived: Boolean)
    suspend fun deleteRoadmap(id: Long)

    suspend fun addMilestone(roadmapId: Long, title: String, description: String? = null, deadline: String? = null): Long
    suspend fun updateMilestone(id: Long, title: String, description: String?, deadline: String?)
    suspend fun deleteMilestone(id: Long)
    suspend fun reorderMilestones(roadmapId: Long, orderedIds: List<Long>)

    suspend fun addStep(milestoneId: Long, title: String): Long
    suspend fun updateStepTitle(id: Long, title: String)
    suspend fun setStepCompleted(id: Long, completed: Boolean)
    suspend fun deleteStep(id: Long)
    suspend fun reorderSteps(milestoneId: Long, orderedIds: List<Long>)
    suspend fun setStepLinks(stepId: Long, links: List<LinkDraft>)

    suspend fun importRoadmap(draft: RoadmapDraft): Long              // one atomic transaction
}
```

- [ ] **Step 3: Create RoomRoadmapRepository.kt**
```kotlin
package com.example.roadmap.data

import androidx.room.withTransaction
import com.example.roadmap.data.entity.LinkEntity
import com.example.roadmap.data.entity.MilestoneEntity
import com.example.roadmap.data.entity.RoadmapEntity
import com.example.roadmap.data.entity.StepEntity
import com.example.roadmap.data.relation.RoadmapWithChildren
import com.example.roadmap.data.relation.sorted
import com.example.roadmap.domain.isValidReorder
import com.example.roadmap.domain.milestoneCompletedAt
import com.example.roadmap.domain.positionsFor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomRoadmapRepository(
    private val db: RoadmapDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) : RoadmapRepository {

    private val roadmaps = db.roadmapDao()
    private val milestones = db.milestoneDao()
    private val steps = db.stepDao()
    private val links = db.linkDao()

    override fun observeRoadmaps(archived: Boolean): Flow<List<RoadmapEntity>> =
        roadmaps.observeByArchived(archived)

    override fun observeRoadmap(id: Long): Flow<RoadmapWithChildren?> =
        roadmaps.observeWithChildren(id).map { it?.sorted() }

    override suspend fun createRoadmap(title: String, description: String?, deadline: String?): Long {
        val t = now()
        return roadmaps.insert(RoadmapEntity(title = title, description = description, deadline = deadline, createdAt = t, updatedAt = t))
    }

    override suspend fun updateRoadmap(id: Long, title: String, description: String?, deadline: String?) {
        val existing = roadmaps.getById(id) ?: return
        roadmaps.update(existing.copy(title = title, description = description, deadline = deadline, updatedAt = now()))
    }

    override suspend fun setArchived(id: Long, archived: Boolean) = roadmaps.setArchived(id, archived, now())

    override suspend fun deleteRoadmap(id: Long) = roadmaps.deleteById(id)

    override suspend fun addMilestone(roadmapId: Long, title: String, description: String?, deadline: String?): Long {
        val t = now()
        val position = milestones.maxPosition(roadmapId) + 1
        return milestones.insert(MilestoneEntity(roadmapId = roadmapId, title = title, description = description, deadline = deadline, position = position, createdAt = t, updatedAt = t))
    }

    override suspend fun updateMilestone(id: Long, title: String, description: String?, deadline: String?) {
        val existing = milestones.getById(id) ?: return
        milestones.update(existing.copy(title = title, description = description, deadline = deadline, updatedAt = now()))
    }

    override suspend fun deleteMilestone(id: Long) = milestones.deleteById(id)

    override suspend fun reorderMilestones(roadmapId: Long, orderedIds: List<Long>) = db.withTransaction {
        val current = milestones.getByRoadmap(roadmapId).map { it.id }
        require(isValidReorder(current, orderedIds)) { "reorder ids must be a permutation of existing milestones" }
        positionsFor(orderedIds).forEach { (id, pos) -> milestones.updatePosition(id, pos) }
    }

    override suspend fun addStep(milestoneId: Long, title: String): Long = db.withTransaction {
        val t = now()
        val position = steps.maxPosition(milestoneId) + 1
        val id = steps.insert(StepEntity(milestoneId = milestoneId, title = title, position = position, createdAt = t, updatedAt = t))
        recompute(milestoneId)
        id
    }

    override suspend fun updateStepTitle(id: Long, title: String) {
        val existing = steps.getById(id) ?: return
        steps.update(existing.copy(title = title, updatedAt = now()))
    }

    override suspend fun setStepCompleted(id: Long, completed: Boolean) = db.withTransaction {
        val existing = steps.getById(id) ?: return@withTransaction
        val t = now()
        steps.update(existing.copy(completed = completed, completedAt = if (completed) t else null, updatedAt = t))
        recompute(existing.milestoneId)
    }

    override suspend fun deleteStep(id: Long) = db.withTransaction {
        val existing = steps.getById(id) ?: return@withTransaction
        steps.deleteById(id)
        recompute(existing.milestoneId)
    }

    override suspend fun reorderSteps(milestoneId: Long, orderedIds: List<Long>) = db.withTransaction {
        val current = steps.getByMilestone(milestoneId).map { it.id }
        require(isValidReorder(current, orderedIds)) { "reorder ids must be a permutation of existing steps" }
        positionsFor(orderedIds).forEach { (id, pos) -> steps.updatePosition(id, pos) }
    }

    override suspend fun setStepLinks(stepId: Long, links: List<LinkDraft>) = db.withTransaction {
        this.links.deleteByStep(stepId)
        this.links.insertAll(links.mapIndexed { i, l -> LinkEntity(stepId = stepId, url = l.url, label = l.label, position = i) })
    }

    override suspend fun importRoadmap(draft: RoadmapDraft): Long = db.withTransaction {
        val t = now()
        val rid = roadmaps.insert(RoadmapEntity(title = draft.title, description = draft.description, deadline = draft.deadline, createdAt = t, updatedAt = t))
        draft.milestones.forEachIndexed { mIndex, m ->
            val mid = milestones.insert(MilestoneEntity(roadmapId = rid, title = m.title, description = m.description, deadline = m.deadline, position = mIndex, createdAt = t, updatedAt = t))
            m.steps.forEachIndexed { sIndex, s ->
                val sid = steps.insert(StepEntity(milestoneId = mid, title = s.title, completed = s.completed, completedAt = if (s.completed) t else null, position = sIndex, createdAt = t, updatedAt = t))
                if (s.links.isNotEmpty()) {
                    links.insertAll(s.links.mapIndexed { lIndex, l -> LinkEntity(stepId = sid, url = l.url, label = l.label, position = lIndex) })
                }
            }
            recompute(mid)
        }
        rid
    }

    /** Recompute a milestone's derived completedAt from its current steps. */
    private suspend fun recompute(milestoneId: Long) {
        val t = now()
        val completions = steps.getByMilestone(milestoneId).map { it.completed }
        milestones.setCompletedAt(milestoneId, milestoneCompletedAt(completions, t), t)
    }
}
```

- [ ] **Step 4: Write the repository test** — `app/src/test/java/com/example/roadmap/data/RoomRoadmapRepositoryTest.kt`:
```kotlin
package com.example.roadmap.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.roadmap.data.relation.sorted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomRoadmapRepositoryTest {
    private lateinit var db: RoadmapDatabase
    private lateinit var repo: RoomRoadmapRepository
    private var clock = 1000L

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), RoadmapDatabase::class.java
        ).build()
        repo = RoomRoadmapRepository(db) { clock }
    }
    @After fun teardown() = db.close()

    @Test fun completing_all_steps_sets_milestone_completedAt_and_unchecking_clears_it() = runTest {
        val rid = repo.createRoadmap("Goal")
        val mid = repo.addMilestone(rid, "M")
        val s1 = repo.addStep(mid, "s1")
        val s2 = repo.addStep(mid, "s2")

        repo.setStepCompleted(s1, true)
        assertNull(repo.observeRoadmap(rid).first()!!.milestones[0].milestone.completedAt)

        clock = 2000L
        repo.setStepCompleted(s2, true)
        assertEquals(2000L, repo.observeRoadmap(rid).first()!!.milestones[0].milestone.completedAt)

        repo.setStepCompleted(s1, false)
        assertNull(repo.observeRoadmap(rid).first()!!.milestones[0].milestone.completedAt)
    }

    @Test fun reorderMilestones_persists_positions() = runTest {
        val rid = repo.createRoadmap("Goal")
        val a = repo.addMilestone(rid, "A")
        val b = repo.addMilestone(rid, "B")
        val c = repo.addMilestone(rid, "C")
        repo.reorderMilestones(rid, listOf(c, a, b))
        val titles = repo.observeRoadmap(rid).first()!!.milestones.map { it.milestone.title }
        assertEquals(listOf("C", "A", "B"), titles)
    }

    @Test fun importRoadmap_inserts_full_tree_atomically_with_completion() = runTest {
        val id = repo.importRoadmap(
            RoadmapDraft(
                title = "Imported",
                milestones = listOf(
                    MilestoneDraft("M1", steps = listOf(StepDraft("done", completed = true, links = listOf(LinkDraft("https://a", "A"))))),
                    MilestoneDraft("M2", steps = listOf(StepDraft("todo"))),
                ),
            ),
        )
        val tree = repo.observeRoadmap(id).first()!!.sorted()
        assertEquals("Imported", tree.roadmap.title)
        assertEquals(2, tree.milestones.size)
        assertNotNull(tree.milestones[0].milestone.completedAt)   // all steps complete
        assertNull(tree.milestones[1].milestone.completedAt)
        assertEquals(listOf("https://a"), tree.milestones[0].steps[0].links.map { it.url })
    }

    @Test fun deleteRoadmap_removes_it() = runTest {
        val rid = repo.createRoadmap("X")
        repo.deleteRoadmap(rid)
        assertEquals(0, repo.observeRoadmaps(false).first().size)
    }
}
```

- [ ] **Step 5: Run + verify** — `./gradlew :app:testDebugUnitTest --tests "*RoomRoadmapRepositoryTest"` → PASS (4 tests).

- [ ] **Step 6: Commit**
```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/java/com/example/roadmap/data/RoadmapRepository.kt app/src/main/java/com/example/roadmap/data/RoomRoadmapRepository.kt app/src/test/java/com/example/roadmap/data/RoomRoadmapRepositoryTest.kt
git commit -m "data: RoadmapRepository (Room impl) with derived completion, reorder, atomic import"
```

---

## Task 8: App wiring + wrap-up

**Files:** Create `data/RoadmapGraph.kt`; Delete the obsolete smoke test; full sweep.

- [ ] **Step 1: Create a minimal provider (no Hilt)** — `data/RoadmapGraph.kt`:
```kotlin
package com.example.roadmap.data

import android.content.Context
import androidx.room.Room

/** Minimal manual DI: a process-wide database + repository. Replace with Hilt later if desired. */
object RoadmapGraph {
    @Volatile private var database: RoadmapDatabase? = null

    fun database(context: Context): RoadmapDatabase =
        database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext, RoadmapDatabase::class.java, "roadmap.db"
            ).build().also { database = it }
        }

    fun repository(context: Context): RoadmapRepository = RoomRoadmapRepository(database(context))
}
```

- [ ] **Step 2: Remove the obsolete smoke test** (real DAO/relation/repo tests now cover the harness):
```bash
git rm app/src/test/java/com/example/roadmap/data/RoadmapDatabaseSmokeTest.kt
```

- [ ] **Step 3: Full sweep** — Run:
```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```
Expected: all unit tests pass (Phase 1 + Completion + Reorder + DAO + Relation + Cascade + Repository), `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**
```bash
git add -A
git commit -m "data: app-level DB/repository provider; drop smoke test"
```

---

## Self-Review (completed during planning)

- **Spec §6 (data model):** all four entities with PK/FK CASCADE + `(parent, position)` indices (Task 1) ✓; `RoadmapWithChildren` `@Relation` read model exposed as `Flow`, sorted by position (Task 3) ✓.
- **Spec §7 (business logic):** completion derivation (Task 5, used in repo Task 7) ✓; progress/overdue are read-side → **Phase 3** (not here) ✓; reorder validation + contiguous positions (Task 6, used in repo) ✓; import validation+insertion — atomic insertion here (Task 7 `importRoadmap`), schema **validation** → Phase 7 ✓.
- **Spec Appendix B (operations):** roadmap list/create/get-with-children/update/delete; milestone add/update/delete/reorder; step add/update/delete/reorder + completion recompute; bulk import — all on `RoadmapRepository` (Task 7) ✓.
- **Atomicity (spec §10):** reorder, step-toggle+recompute, setStepLinks, and import are `db.withTransaction` (Task 7); delete-cascade is a single FK operation ✓.
- **Type consistency:** `RoadmapEntity/MilestoneEntity/StepEntity/LinkEntity`, DAO names, `RoadmapWithChildren`/`MilestoneWithSteps`/`StepWithLinks` + `.sorted()`, `milestoneCompletedAt(List<Boolean>, Long)`, `isValidReorder`/`positionsFor`, `RoadmapRepository` signatures + `*Draft` models — defined before use and referenced consistently across tasks ✓.
- **Test strategy:** pure helpers = plain JVM JUnit; everything touching Room = Robolectric (`@Config(sdk=[34])`) JVM unit tests — all run under `./gradlew :app:testDebugUnitTest`, no emulator ✓.
- **Deferred (tracked, not gaps):** `exportSchema=false` until migrations are introduced; URL/JSON-schema import validation → Phase 7; progress %, overdue, formatting → Phase 3.
```
