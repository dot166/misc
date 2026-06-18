/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:JvmName("EdgeToEdge")

package io.github.dot166.nexus

import android.app.UiModeManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
import androidx.annotation.ColorInt
import androidx.annotation.VisibleForTesting
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.insets.ColorProtection
import androidx.core.view.insets.ProtectionLayout

// The light scrim color used in the platform API 29+
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/com/android/internal/policy/DecorView.java;drc=6ef0f022c333385dba2c294e35b8de544455bf19;l=142
@VisibleForTesting internal val DefaultLightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)

// The dark scrim color used in the platform.
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/res/res/color/system_bar_background_semi_transparent.xml
// https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/res/remote_color_resources_res/values/colors.xml;l=67
@VisibleForTesting internal val DefaultDarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)

@JvmOverloads
fun NexusOverlay.enableEdgeToEdge(
    statusBarStyle: SystemBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
    navigationBarStyle: SystemBarStyle = SystemBarStyle.auto(DefaultLightScrim, DefaultDarkScrim),
) {
    val view = window!!.decorView
    val statusBarIsDark = statusBarStyle.detectDarkMode(view.resources)
    val navigationBarIsDark = navigationBarStyle.detectDarkMode(view.resources)
    WindowCompat.setDecorFitsSystemWindows(window, false)
    // TODO(b/438675320): Remove the attributes check after `activity` depends on a version of
    //                    `core` which doesn't have b/434987937.
    // The attributes check is safe because when the window size is WRAP_CONTENT, DecorView
    // would consume the system window insets, and the protection is not necessary.
    val attrs = window.attributes
    if (
        (attrs.flags and FLAG_LAYOUT_IN_SCREEN) != 0 ||
        attrs.width != WRAP_CONTENT ||
        attrs.height != WRAP_CONTENT
    ) {
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        val statusBarColor = statusBarStyle.getScrimWithEnforcedContrast(statusBarIsDark)
        val navBarColor = navigationBarStyle.getScrimWithEnforcedContrast(navigationBarIsDark)
        (view as ViewGroup).addView(
            ProtectionLayout(
                view.context,
                listOf(
                    ColorProtection(WindowInsetsCompat.Side.TOP, statusBarColor),
                    ColorProtection(WindowInsetsCompat.Side.LEFT, navBarColor),
                    ColorProtection(WindowInsetsCompat.Side.RIGHT, navBarColor),
                    ColorProtection(WindowInsetsCompat.Side.BOTTOM, navBarColor),
                ),
            )
        )
    }
    window.isNavigationBarContrastEnforced =
        navigationBarStyle.nightMode == UiModeManager.MODE_NIGHT_AUTO
    WindowInsetsControllerCompat(window, view).run {
        isAppearanceLightStatusBars = !statusBarIsDark
        isAppearanceLightNavigationBars = !navigationBarIsDark
    }
    window.attributes.layoutInDisplayCutoutMode =
        WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
}

/** The style for the status bar or the navigation bar used in [enableEdgeToEdge]. */
class SystemBarStyle
private constructor(
    private val lightScrim: Int,
    internal val darkScrim: Int,
    internal val nightMode: Int,
    internal val detectDarkMode: (Resources) -> Boolean,
) {

    companion object {

        /**
         * Creates a new instance of [SystemBarStyle]. This style detects the dark mode
         * automatically and applies the recommended style for each of the status bar and the
         * navigation bar. If this style doesn't work for your app, consider using either [dark] or
         * [light].
         * - On API level 29 and above, both the status bar and the navigation bar will be
         *   transparent. However, the navigation bar with 3 or 2 buttons will have a translucent
         *   scrim. This scrim color is provided by the platform and *cannot be customized*.
         * - On API level 28 and below, the status bar will be transparent, and the navigation bar
         *   will have one of the specified scrim colors depending on the dark mode.
         *
         * @param lightScrim The scrim color to be used for the background when the app is in light
         *   mode. Note that this is used only on API level 28 and below.
         * @param darkScrim The scrim color to be used for the background when the app is in dark
         *   mode. This is also used on devices where the system icon color is always light. Note
         *   that this is used only on API level 28 and below.
         * @param detectDarkMode Optional. Detects whether UI currently uses dark mode or not. The
         *   default implementation can detect any of the standard dark mode features from the
         *   platform, appcompat, and Jetpack Compose.
         */
        @JvmStatic
        @JvmOverloads
        fun auto(
            @ColorInt lightScrim: Int,
            @ColorInt darkScrim: Int,
            detectDarkMode: (Resources) -> Boolean = { resources ->
                (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                        Configuration.UI_MODE_NIGHT_YES
            },
        ): SystemBarStyle {
            return SystemBarStyle(
                lightScrim = lightScrim,
                darkScrim = darkScrim,
                nightMode = UiModeManager.MODE_NIGHT_AUTO,
                detectDarkMode = detectDarkMode,
            )
        }

        /**
         * Creates a new instance of [SystemBarStyle]. This style consistently applies the specified
         * scrim color regardless of the system navigation mode.
         *
         * @param scrim The scrim color to be used for the background. It is expected to be dark for
         *   the contrast against the light system icons.
         */
        @JvmStatic
        fun dark(@ColorInt scrim: Int): SystemBarStyle {
            return SystemBarStyle(
                lightScrim = scrim,
                darkScrim = scrim,
                nightMode = UiModeManager.MODE_NIGHT_YES,
                detectDarkMode = { _ -> true },
            )
        }

        /**
         * Creates a new instance of [SystemBarStyle]. This style consistently applies the specified
         * scrim color regardless of the system navigation mode.
         *
         * @param scrim The scrim color to be used for the background. It is expected to be light
         *   for the contrast against the dark system icons.
         * @param darkScrim The scrim color to be used for the background on devices where the
         *   system icon color is always light. It is expected to be dark.
         */
        @JvmStatic
        fun light(@ColorInt scrim: Int, @ColorInt darkScrim: Int): SystemBarStyle {
            return SystemBarStyle(
                lightScrim = scrim,
                darkScrim = darkScrim,
                nightMode = UiModeManager.MODE_NIGHT_NO,
                detectDarkMode = { _ -> false },
            )
        }
    }

    internal fun getScrimWithEnforcedContrast(isDark: Boolean): Int {
        return when {
            nightMode == UiModeManager.MODE_NIGHT_AUTO -> Color.TRANSPARENT
            isDark -> darkScrim
            else -> lightScrim
        }
    }
}


