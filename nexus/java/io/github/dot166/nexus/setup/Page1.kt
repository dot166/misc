package io.github.dot166.nexus.setup

import android.app.UiModeManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.More
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.widget.preference.ListPreferenceModel
import com.android.settingslib.spa.widget.preference.ListPreferenceOption
import com.android.settingslib.spa.widget.preference.RadioPreferences
import com.android.settingslib.spa.widget.scaffold.BottomAppBarButton
import com.android.settingslib.spa.widget.scaffold.GlifScaffold
import io.github.dot166.nexus.R
import io.github.dot166.nexus.setup.SetupUtils.backToSuW


object Page1 : SettingsPageProvider {
    override val name = "SetupWizardPage1"

    @Composable
    override fun Page(arguments: Bundle?) {
        //val next = navigator("SetupWizardPage2")
        val activity = LocalActivity.current
        GlifScaffold(
            imageVector = Icons.AutoMirrored.Outlined.More,
            title = stringResource(R.string.extra_steps),
            description = stringResource(R.string.dark_theme),
            actionButton = BottomAppBarButton(stringResource(R.string.next)) { backToSuW(activity) },//next() },
        ) {
            Column(Modifier.padding(SettingsDimension.itemPadding)) {
                val uiModeManager: UiModeManager =
                    SpaEnvironmentFactory.instance.appContext.getSystemService(UiModeManager::class.java)
                RadioPreferences(
                    object : ListPreferenceModel {
                        override val title: String = ""
                        override val options: List<ListPreferenceOption> =
                            listOf(
                                ListPreferenceOption(id = 0, text = stringResource(R.string.light)),
                                ListPreferenceOption(id = 1, text = stringResource(R.string.dark)),
                            )
                        override val selectedId: IntState = remember { mutableIntStateOf(
                            if ((SpaEnvironmentFactory.instance.appContext.getResources().configuration.uiMode
                                        and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                            ) 1 else 0) }
                        override val onIdSelected: (Int) -> Unit = {
                            when (it) {
                                0 -> {
                                    uiModeManager.setNightModeActivated(false)
                                }
                                1 -> {
                                    uiModeManager.setNightModeActivated(true)
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}
