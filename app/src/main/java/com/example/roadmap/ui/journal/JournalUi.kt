package com.example.roadmap.ui.journal

import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/** A text-only tappable affordance (used for the inline "+ Add …" rows). */
fun Modifier.clickableText(onClick: () -> Unit): Modifier = this.clickable(role = Role.Button) { onClick() }
