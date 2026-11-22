package io.github.dot166.otwarchive.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import io.github.dot166.jlib.compose.JLibScreen
import io.github.dot166.jlib.compose.preference.ScrollableScreenColumn
import io.github.dot166.libotwarchive.AO3Exception
import io.github.dot166.otwarchive.MainViewModel
import io.github.dot166.otwarchive.R
import io.github.dot166.otwarchive.ui.dialog.CloudflareBypassDialog
import kotlinx.coroutines.launch
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterScreen(viewModel: MainViewModel, workId: Long, chapterNumber: Int) {
    var error by remember {
        mutableStateOf<String?>(null)
    }
    var reloadCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadCount) {
        try {
            when (val result = viewModel.loadWork(workId)) {
                null -> Unit // assume handled error
                else -> viewModel.currentChapter.value = result.chapters[chapterNumber - 1]
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

        viewModel.currentChapter.collectAsState().value != null -> {
            val currentChapter = viewModel.currentChapter.collectAsState().value ?: return
            val sheetState = rememberModalBottomSheetState()
            val scope = rememberCoroutineScope()
            var showBottomSheet by remember { mutableStateOf(false) }
            JLibScreen(
                title = "${currentChapter.work.title} - Chapter ${currentChapter.number} - ${currentChapter.title}",//"${stringResource(R.string.app_name)} - ${currentChapter.work.title} - Chapter ${currentChapter.number} - ${currentChapter.title}",
                bottomBar = {
                    Row {
                        Button(onClick = {
                            viewModel.previousChapter()
                        }) {
                            Text(text = stringResource(R.string.previous_chapter))
                        }
                        Button(onClick = { showBottomSheet = true }) {
                            Text(text = stringResource(R.string.kudos))
                        }
                        Button(onClick = {
                            viewModel.nextChapter()
                        }) {
                            Text(text = stringResource(R.string.next_chapter))
                        }
                    }
                },
            ) { (contentPadding, translucent) ->
                ScrollableScreenColumn(contentPadding, translucent) {
                    // TODO: make a better UI
                    Text(
                        text = currentChapter.text
                    )
                }
                if (showBottomSheet) {
                    ModalBottomSheet(
                        onDismissRequest = {
                            showBottomSheet = false
                        },
                        sheetState = sheetState
                    ) {
                        Button(onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showBottomSheet = false
                                }
                            }
                        }) {
                            Text("Hide bottom sheet")
                        }
                    }
                }
            }
        }

        error != null -> Text("Error: $error")
        else -> CircularProgressIndicator()
    }
}