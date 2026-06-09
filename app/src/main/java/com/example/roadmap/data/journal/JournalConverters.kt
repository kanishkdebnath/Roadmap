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
