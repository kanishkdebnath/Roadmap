package com.example.roadmap.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ThemeStoreTest {
    private val ctx get() = ApplicationProvider.getApplicationContext<Context>()

    @Test fun defaults_to_system() {
        assertEquals(ThemeMode.System, ThemeStore(ctx).mode.value)
    }

    @Test fun set_mode_updates_flow_and_persists_across_instances() {
        val store = ThemeStore(ctx)
        store.setMode(ThemeMode.Dark)
        assertEquals(ThemeMode.Dark, store.mode.value)
        // a fresh instance reads the persisted value
        assertEquals(ThemeMode.Dark, ThemeStore(ctx).mode.value)
    }
}
