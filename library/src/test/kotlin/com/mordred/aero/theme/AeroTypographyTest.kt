package com.mordred.aero.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AeroTypographyTest {

    @Test
    fun titleIs18spBold() {
        val t = AeroTypography()
        assertEquals(18.sp, t.title.fontSize, "title.fontSize")
        assertEquals(FontWeight.Bold, t.title.fontWeight, "title.fontWeight")
    }

    @Test
    fun bodyLargeIs14spNormal() {
        val t = AeroTypography()
        assertEquals(14.sp, t.bodyLarge.fontSize, "bodyLarge.fontSize")
        assertEquals(FontWeight.Normal, t.bodyLarge.fontWeight, "bodyLarge.fontWeight")
    }

    @Test
    fun bodyMediumIs13spNormal() {
        val t = AeroTypography()
        assertEquals(13.sp, t.bodyMedium.fontSize, "bodyMedium.fontSize")
        assertEquals(FontWeight.Normal, t.bodyMedium.fontWeight, "bodyMedium.fontWeight")
    }

    @Test
    fun bodySmallIs12spNormal() {
        val t = AeroTypography()
        assertEquals(12.sp, t.bodySmall.fontSize, "bodySmall.fontSize")
        assertEquals(FontWeight.Normal, t.bodySmall.fontWeight, "bodySmall.fontWeight")
    }

    @Test
    fun labelIs11spBold() {
        val t = AeroTypography()
        assertEquals(11.sp, t.label.fontSize, "label.fontSize")
        assertEquals(FontWeight.Bold, t.label.fontWeight, "label.fontWeight")
    }

    // Note: @Immutable from androidx.compose.runtime uses AnnotationRetention.BINARY (Java CLASS retention),
    // which means it is NOT accessible via runtime reflection. The data class property verifies structural
    // immutability as the runtime-verifiable proxy for @Immutable (same fix applied as AeroColorSchemeTest).
    @Test
    fun aeroTypographyIsDataClass() {
        assertTrue(AeroTypography::class.isData, "AeroTypography must be a data class (structural immutability)")
    }

    @Test
    fun copyChangesOnlyTheTargetedSlot() {
        val custom = AeroTypography().copy(title = TextStyle(fontSize = 24.sp))
        assertEquals(24.sp, custom.title.fontSize, "title overridden")
        assertEquals(14.sp, custom.bodyLarge.fontSize, "bodyLarge preserved")
    }
}
