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
