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
