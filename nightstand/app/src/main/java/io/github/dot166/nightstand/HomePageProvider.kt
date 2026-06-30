package io.github.dot166.nightstand

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.android.settingslib.spa.widget.preference.ListPreference
import com.android.settingslib.spa.widget.preference.ListPreferenceModel
import com.android.settingslib.spa.widget.preference.ListPreferenceOption
import com.android.settingslib.spa.widget.preference.SliderPreference
import com.android.settingslib.spa.widget.preference.SliderPreferenceModel
import com.android.settingslib.spa.widget.preference.SwitchPreference
import com.android.settingslib.spa.widget.preference.SwitchPreferenceModel
import com.android.settingslib.spa.widget.scaffold.HomeScaffold
import com.android.settingslib.spa.widget.ui.Category
import io.github.dot166.nightstand.Utils.screensaverBoldTextOn
import io.github.dot166.nightstand.Utils.screensaverClockColor
import io.github.dot166.nightstand.Utils.screensaverClockNightModeColor
import io.github.dot166.nightstand.Utils.screensaverNightModeBrightness
import io.github.dot166.nightstand.Utils.screensaverNightModeDndOn
import io.github.dot166.nightstand.Utils.screensaverNightModeOn
import io.github.dot166.nightstand.Utils.screensaverShowAmPmOn

object HomePageProvider : SettingsPageProvider {
    override val name = "NightStand Preference"
    override val displayName = "Home"

    override fun getTitle(arguments: Bundle?): String {
        return SpaEnvironmentFactory.instance.appContext.getString(R.string.app_name)
    }

    @Composable
    override fun Page(arguments: Bundle?) {
        val title = remember { getTitle(arguments) }
        HomeScaffold(title) {
            val provider = ClockColourProvider(SpaEnvironmentFactory.instance.appContext)
            Category {
                val model = object : ListPreferenceModel {
                    override val title: String
                        get() = SpaEnvironmentFactory.instance.appContext.getString(R.string.clock_color_title)
                    override val options: List<ListPreferenceOption>
                        get() = provider.getColoursAsListOptions()
                    override val selectedId: IntState
                        get() = mutableIntStateOf(provider.getIndexOfColour(screensaverClockColor))
                    override val onIdSelected: (id: Int) -> Unit
                        get() = {
                            screensaverClockColor = provider.getColour(it)
                        }
                }
                ListPreference(model)
                SwitchPreference(object : SwitchPreferenceModel {
                    override val title: String
                        get() = SpaEnvironmentFactory.instance.appContext.getString(R.string.bold_text_title)
                    override val checked: () -> Boolean?
                        get() = { screensaverBoldTextOn }
                    override val onCheckedChange: (newChecked: Boolean) -> Unit
                        get() = { screensaverBoldTextOn = it }
                })
                SwitchPreference(object : SwitchPreferenceModel {
                    override val title: String
                        get() = SpaEnvironmentFactory.instance.appContext.getString(R.string.show_ampm_title)
                    override val checked: () -> Boolean?
                        get() = { screensaverShowAmPmOn }
                    override val onCheckedChange: (newChecked: Boolean) -> Unit
                        get() = { screensaverShowAmPmOn = it }
                    override val changeable: () -> Boolean
                        get() = { !DateFormat.is24HourFormat(SpaEnvironmentFactory.instance.appContext) }
                })
            }
            Category(stringResource(R.string.night_mode_title)) {
                SwitchPreference(object : SwitchPreferenceModel {
                    override val title: String
                        get() = SpaEnvironmentFactory.instance.appContext.getString(R.string.night_mode_enabled_title)
                    override val checked: () -> Boolean?
                        get() = { screensaverNightModeOn }
                    override val onCheckedChange: (newChecked: Boolean) -> Unit
                        get() = { screensaverNightModeOn = it }
                    override val summary: () -> CharSequence
                        get() = { SpaEnvironmentFactory.instance.appContext.getString(R.string.night_mode_summary)}
                })
                SwitchPreference(object : SwitchPreferenceModel {
                    override val title: String
                        get() = SpaEnvironmentFactory.instance.appContext.getString(R.string.night_mode_dnd_title)
                    override val checked: () -> Boolean?
                        get() = { screensaverNightModeDndOn }
                    override val onCheckedChange: (newChecked: Boolean) -> Unit
                        get() = { screensaverNightModeDndOn = it }
                    override val summary: () -> CharSequence
                        get() = { SpaEnvironmentFactory.instance.appContext.getString(R.string.night_mode_dnd_summary)}
                })
                val model = object : ListPreferenceModel {
                    override val title: String
                        get() = SpaEnvironmentFactory.instance.appContext.getString(R.string.clock_color_title)
                    override val options: List<ListPreferenceOption>
                        get() = provider.getColoursAsListOptions()
                    override val selectedId: IntState
                        get() = mutableIntStateOf(provider.getIndexOfColour(screensaverClockNightModeColor))
                    override val onIdSelected: (id: Int) -> Unit
                        get() = {
                            screensaverClockNightModeColor = provider.getColour(it)
                        }
                }
                ListPreference(model)
                SliderPreference(object : SliderPreferenceModel {
                    override val title: String
                        get() = SpaEnvironmentFactory.instance.appContext.getString(R.string.night_mode_brightness_text_title)
                    override val initValue: Int
                        get() = screensaverNightModeBrightness
                    override val onValueChange: ((value: Int) -> Unit) = { screensaverNightModeBrightness = it }
                })
            }
        }
    }
}

@Preview(showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    wallpaper = Wallpapers.RED_DOMINATED_EXAMPLE
)
@Composable
private fun HomeScreenPreview() {
    SpaEnvironmentFactory.resetForPreview2()
    SettingsTheme {
        HomePageProvider.Page(null)
    }
}

@SuppressLint("ComposableNaming")
@Composable
private fun SpaEnvironmentFactory.resetForPreview2() {
    val context = LocalContext.current
    reset(NightStandSpaEnvironment(context))
}
