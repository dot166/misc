package io.github.dot166.otwarchive

import android.app.Application
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import io.github.dot166.jlib.compose.JLibAppTheme
import io.github.dot166.jlib.compose.conditional

val LocalNavController = staticCompositionLocalOf<NavController> {
    error("LocalNavController not initialized")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel = MainViewModel(applicationContext as Application)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            CompositionLocalProvider(
                LocalNavController provides navController,
            ) {
                JLibAppTheme {
                    Column(
                        modifier = Modifier
                            //.statusBarsPadding()
                            .navigationBarsPadding()
                            .conditional(LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                                displayCutoutPadding()
                            }
                            .imePadding(),
                    ) {
                        Routes.AppNavHost(
                            modifier = Modifier.weight(1.0f),
                            navController = navController,
                            startDestination = Routes.Home::class,
                            viewModel = viewModel,
                        )
                    }
                }
            }
        }
    }
}