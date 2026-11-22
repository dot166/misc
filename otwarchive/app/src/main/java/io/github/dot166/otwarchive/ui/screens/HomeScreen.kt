package io.github.dot166.otwarchive.ui.screens

import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import io.github.dot166.jlib.compose.JLibScreen
import io.github.dot166.jlib.compose.preference.ScrollableScreenColumn
import io.github.dot166.libotwarchive.AO3Exception
import io.github.dot166.libotwarchive.Work
import io.github.dot166.otwarchive.LocalNavController
import io.github.dot166.otwarchive.MainViewModel
import io.github.dot166.otwarchive.R
import io.github.dot166.otwarchive.Routes
import io.github.dot166.otwarchive.ui.dialog.CloudflareBypassDialog
import java.io.IOException

@Composable
fun HomeScreen(viewModel: MainViewModel) {
    val navController = LocalNavController.current
    JLibScreen(
        title = stringResource(R.string.app_name),
    ) { (contentPadding, translucent) ->
        ScrollableScreenColumn(contentPadding, translucent) {
            Text(
                text = "WIP!"
            )
            Button(onClick = {navController.navigate(Routes.Work(81769556))}) {
                Text(text = "Test it")
            }
            var results by remember {
                mutableStateOf<List<Work>?>(null)
            }
            var error by remember {
                mutableStateOf<String?>(null)
            }
            var reloadCount by remember { mutableIntStateOf(0) }

            LaunchedEffect(reloadCount) {
                try {
                    when (val result = viewModel.search()) {
                        null -> Unit // assume handled error
                        else -> results = result.results
                    }
                } catch (ex: Exception) {
                    when(ex) {
                        is AO3Exception, is IOException, is IllegalStateException, is IllegalArgumentException, is SecurityException -> {
                            error = ex.message
                        }

                        else -> throw ex
                    }
                }
            }

            val isBlocked by viewModel.isBlocked.collectAsState()

            when {
                isBlocked -> {
                    CloudflareBypassDialog(
                        onChallengeSolved = {
                            viewModel.dismissChallenge()
                            reloadCount++
                        }
                    )
                }

                results != null -> {
                    for (work in results!!) {
                        Button(onClick = {navController.navigate(Routes.Work(work.id))}) {
                            Text(text = work.title)
                        }
                    }
                }
                error != null -> Text("Error: $error")
                else -> CircularProgressIndicator()
            }
        }
    }
}