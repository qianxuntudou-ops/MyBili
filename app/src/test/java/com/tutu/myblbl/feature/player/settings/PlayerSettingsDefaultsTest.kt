package com.tutu.myblbl.feature.player.settings

import org.junit.Assert.assertFalse
import org.junit.Test

class PlayerSettingsDefaultsTest {

    @Test
    fun sponsorBlockIsOffByDefault() {
        assertFalse(PlayerSettings().sponsorBlockEnabled)
    }
}
