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
