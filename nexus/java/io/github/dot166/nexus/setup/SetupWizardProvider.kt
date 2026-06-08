package io.github.dot166.nexus.setup

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory
import com.android.settingslib.spa.framework.compose.navigator
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.android.settingslib.spa.widget.illustration.Illustration
import com.android.settingslib.spa.widget.illustration.IllustrationModel
import com.android.settingslib.spa.widget.illustration.ResourceType
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.scaffold.BottomAppBarButton
import com.android.settingslib.spa.widget.scaffold.GlifScaffold
import com.android.settingslib.spa.widget.ui.SettingsBody
import com.android.settingslib.spa.widget.ui.Spinner
import com.android.settingslib.spa.widget.ui.SpinnerOption
import io.github.dot166.jlib.app.DefaultHomePageProvider
import io.github.dot166.nexus.NexusSpaEnvironment

object SetupWizardProvider : SettingsPageProvider {
    override val name = "SetupWizardPage0"

    @Composable
    override fun Page(arguments: Bundle?) {
        val next = navigator("SetupWizardPage1")
        GlifScaffold(
            imageVector = Icons.Outlined.SignalCellularAlt,
            title = "Extra steps",
            description = "Select a mobile network to connect to",
            actionButton = BottomAppBarButton("Next") { next() },
            dismissButton = BottomAppBarButton("Skip") {  },
        ) {
            var selectedId by rememberSaveable { mutableIntStateOf(1) }
            Spinner(
                options = (1..3).map { SpinnerOption(id = it, text = "Option $it") },
                selectedId = selectedId,
                setId = { selectedId = it },
            )
            Column(Modifier.padding(SettingsDimension.itemPadding)) {
                SettingsBody("To add another SIM, download a new eSIM.")
            }
            Illustration(
                object : IllustrationModel {
                    override val resId = R.drawable.accessibility_captioning_banner
                    override val resourceType = ResourceType.IMAGE
                }
            )
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
        SetupWizardProvider.Page(null)
    }
}

@SuppressLint("ComposableNaming")
@Composable
private fun SpaEnvironmentFactory.resetForPreview2() {
    val context = LocalContext.current
    reset(NexusSpaEnvironment(context))
}
