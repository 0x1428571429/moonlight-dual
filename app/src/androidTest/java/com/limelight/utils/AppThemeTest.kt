package com.limelight.utils

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppThemeTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val prefs = context.getSharedPreferences("AppTheme", Context.MODE_PRIVATE)
    private var mode: String? = null
    private var accent: String? = null
    private var bucket: Int? = null

    // XML-only resource fields can be removed from R by debug R8; resolve their stable resource names.
    private fun resource(name: String, type: String): Int =
        context.resources.getIdentifier(name, type, context.packageName).also { check(it != 0) { name } }

    @Before fun save() {
        mode = prefs.getString("theme_mode", null)
        accent = prefs.getString("accent_mode", null)
        bucket = if (prefs.contains("you_bg_accent_bucket")) prefs.getInt("you_bg_accent_bucket", -1) else null
    }

    @After fun restore() {
        val editor = prefs.edit().putString("theme_mode", mode).putString("accent_mode", accent)
        bucket?.let { editor.putInt("you_bg_accent_bucket", it) } ?: editor.remove("you_bg_accent_bucket")
        editor.commit()
        AppTheme.applyStoredAppTheme(context)
    }

    private fun accentColor(context: Context): Int {
        val value = TypedValue()
        assertTrue(context.theme.resolveAttribute(resource("appAccent", "attr"), value, true))
        return value.data
    }

    // Moonlight Dual ships one theme: dark + brand pink. Stored preferences are ignored.
    @Test fun storedAppearanceAndAccentAreIgnoredBecauseTheThemeIsFixed() {
        for (mode in listOf("system", "light", "dark")) {
            for (accent in listOf("pink", "bg")) {
                AppTheme.save(context, mode, accent)
                assertEquals("dark", AppTheme.getAppThemeMode(context))
                assertEquals("pink", AppTheme.getAccentMode(context))
            }
        }
    }

    @Test fun missingAccentPreferencePreservesBrandAndAppearance() {
        prefs.edit().remove("accent_mode").putString("theme_mode", "dark").commit()
        assertEquals("pink", AppTheme.getAccentMode(context))
        assertEquals("dark", AppTheme.getAppThemeMode(context))
    }

    @Test fun transparentPixelsCannotOutvoteVisibleContent() {
        val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
        try {
            bitmap.setPremultiplied(false)
            bitmap.eraseColor(Color.argb(0, 230, 0, 0))
            assertEquals(-1, BgAccent.extractHueBucket(bitmap))
            bitmap.eraseColor(Color.argb(1, 230, 0, 0))
            bitmap.setPixel(0, 0, Color.rgb(0, 230, 0))
            assertEquals(4, BgAccent.extractHueBucket(bitmap))
        } finally {
            bitmap.recycle()
        }
    }

    @Test fun storedBackgroundAccentNeverOverridesTheBrandAccent() {
        val styled = ContextThemeWrapper(context, resource("AppTheme", "style"))
        val brand = ContextCompat.getColor(styled, resource("ui_shell_accent", "color"))
        prefs.edit().putString("accent_mode", "bg").putInt("you_bg_accent_bucket", 3).commit()
        AppTheme.applyTo(styled)
        assertEquals(brand, accentColor(styled))
        AppTheme.applyStyle(styled, resource("PreferenceThemeWithShadow", "style"))
        assertEquals(brand, accentColor(styled))
    }

    @Test fun explicitNightModeRetainsOtherConfigurationDimensions() {
        val activityOverride = AppTheme.nightOverride(context)!!
        assertEquals(Configuration.ORIENTATION_UNDEFINED, activityOverride.orientation)
        assertEquals(0, activityOverride.densityDpi)
        val configuration = Configuration(context.resources.configuration).apply {
            orientation = Configuration.ORIENTATION_LANDSCAPE
            densityDpi = 320
            uiMode = Configuration.UI_MODE_NIGHT_NO
        }
        val themed = AppTheme.paletteContext(context.createConfigurationContext(configuration))
        val actual = themed.resources.configuration
        assertEquals(Configuration.UI_MODE_NIGHT_YES, actual.uiMode and Configuration.UI_MODE_NIGHT_MASK)
        assertEquals(Configuration.ORIENTATION_LANDSCAPE, actual.orientation)
        assertEquals(320, actual.densityDpi)
        assertEquals(ContextCompat.getColor(themed, resource("ui_shell_accent", "color")), accentColor(themed))
    }
}
