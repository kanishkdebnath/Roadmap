package com.example.roadmap.data.journal

import kotlinx.serialization.Serializable

/** Fixed mood tag vocabulary (spec J3). Display labels are lowercased in the UI. */
@Serializable
enum class MoodTag { Focused, Tired, Anxious, Grateful, Restless, Excited, Low, Calm }
