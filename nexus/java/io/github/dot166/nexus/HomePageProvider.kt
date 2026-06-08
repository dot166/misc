package io.github.dot166.nexus

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.android.settingslib.spa.widget.preference.ListPreference
import com.android.settingslib.spa.widget.preference.ListPreferenceModel
import com.android.settingslib.spa.widget.preference.ListPreferenceOption
import com.android.settingslib.spa.widget.scaffold.HomeScaffold
import io.github.dot166.jlib.app.DefaultHomePageProvider
import io.github.dot166.jlib.app.JLibSpaEnvironmentStub

object HomePageProvider : SettingsPageProvider {
    override val name = "Nexus"
    override val displayName = "Nexus"

    override fun getTitle(arguments: Bundle?): String {
        return SpaEnvironmentFactory.instance.appContext.getString(R.string.app_name)
    }

    @Composable
    override fun Page(arguments: Bundle?) {
        val title = remember { getTitle(arguments) }
        HomeScaffold(title) {
            val provider = FeedProvider(SpaEnvironmentFactory.instance.appContext)
            val model = object : ListPreferenceModel {
                override val title: String
                    get() = SpaEnvironmentFactory.instance.appContext.getString(R.string.feed_provider)
                override val options: List<ListPreferenceOption>
                    get() = provider.getProvidersAsListOptions()
                override val selectedId: androidx.compose.runtime.IntState
                    get() = mutableIntStateOf(provider.getIndexOfProvider(provider.getSavedFeed()))
                override val onIdSelected: (id: Int) -> Unit
                    get() = {
                        provider.setSavedFeed(provider.getProvider(it))
                    }
            }
            ListPreference(model)
//            Category {
//                PreferenceMainPageProvider.Entry()
//                RestrictedSwitchPreferencePageProvider.Entry()
//            }
//            Category {
//                SearchScaffoldPageProvider.Entry()
//                GlifScaffoldPageProvider.Entry()
//                ArgumentPageProvider.EntryItem(stringParam = "foo", intParam = 0)
//            }
//            Category {
//                SliderPageProvider.Entry()
//                SpinnerPageProvider.Entry()
//                PagerMainPageProvider.Entry()
//                FooterPageProvider.Entry()
//                IllustrationPageProvider.Entry()
//                CategoryPageProvider.Entry()
//                ActionButtonPageProvider.Entry()
//                ProgressBarPageProvider.Entry()
//                LoadingBarPageProvider.Entry()
//                ChartPageProvider.Entry()
//                DialogMainPageProvider.Entry()
//                EditorMainPageProvider.Entry()
//                BannerPageProvider.Entry()
//                CardPageProvider.Entry()
//                CopyablePageProvider.Entry()
//            }
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
    reset(NexusSpaEnvironment(context))
}
