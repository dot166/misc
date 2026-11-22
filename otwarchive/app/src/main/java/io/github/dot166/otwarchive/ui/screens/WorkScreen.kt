package io.github.dot166.otwarchive.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalFlexBoxApi
import androidx.compose.foundation.layout.FlexAlignItems
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.FlexDirection
import androidx.compose.foundation.layout.FlexWrap
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.dot166.jlib.compose.JLibScreen
import io.github.dot166.jlib.compose.preference.ScrollableScreenColumn
import io.github.dot166.libotwarchive.AO3Exception
import io.github.dot166.libotwarchive.Work
import io.github.dot166.otwarchive.MainViewModel
import io.github.dot166.otwarchive.ui.dialog.CloudflareBypassDialog
import java.io.IOException

@OptIn(ExperimentalFlexBoxApi::class)
@Composable
fun WorkScreen(viewModel: MainViewModel, workId: Long) {
    var work by remember { mutableStateOf<Work?>(null) }
    var error by remember {
        mutableStateOf<String?>(null)
    }
    var reloadCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadCount) {
        try {
            when (val result = viewModel.loadWork(workId)) {
                null -> Unit // assume handled error
                else -> work = result
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

        work != null -> {
            JLibScreen(
                title = work!!.title,
            ) { (contentPadding, translucent) ->
                ScrollableScreenColumn(contentPadding, translucent) {
                    if (work!!.restricted) {
                        Spacer(modifier = Modifier.padding(4.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                contentColor = Color.White,
                                containerColor = Color.Red,
                                disabledContainerColor = Color.Red,
                                disabledContentColor = Color.White,
                            )
                        ) {
                            Text(
                                text = "Work is restricted!, reader discretion is advised.",
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                    if (work!!.rating != null && work!!.rating!!.isNotEmpty()) {
                        Spacer(modifier = Modifier.padding(4.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                contentColor = Color.White,
                                containerColor = Color.Red,
                                disabledContainerColor = Color.Red,
                                disabledContentColor = Color.White,
                            )
                        ) {
                            Text(
                                text = "Rating: ${work!!.rating}",
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                    if (work!!.warnings.isNotEmpty()) {
                        Spacer(modifier = Modifier.padding(4.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                contentColor = Color.Black,
                                containerColor = Color.Yellow,
                                disabledContainerColor = Color.Yellow,
                                disabledContentColor = Color.Black,
                            )
                        ) {
                            Text(
                                text = "Warnings: ${work!!.warnings.joinToString(", ")}",
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.padding(4.dp))
                    FlexBox(
                        config = {
                            direction(FlexDirection.Row)
                            wrap(FlexWrap.Wrap)
                            alignItems(FlexAlignItems.Center)
                            gap(4.dp)
                        },
                    ) {
                        for (tag in work!!.computeTags()) {
                            Card {
                                Text(text = tag, modifier = Modifier.padding(4.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.padding(4.dp))
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        Card {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = work!!.words.toString(),
                                    fontWeight = FontWeight.ExtraBold,
                                )
                                Text(
                                    text = "Words",
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                        Card {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = work!!.nchapters.toString(),
                                    fontWeight = FontWeight.ExtraBold,
                                )
                                Text(
                                    text = "Chapters",
                                    modifier = Modifier.padding(4.dp)
                                )
                                if (work!!.complete) {
                                    Text(
                                        text = "Complete",
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            }
                        }
                        Card {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = work!!.kudos.toString(),
                                    fontWeight = FontWeight.ExtraBold,
                                )
                                Text(
                                    text = "Kudos",
                                    modifier = Modifier.padding(4.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.padding(4.dp))
                    Card {
                        Text(
                            text = work!!.summary,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.padding(4.dp))
                    // TODO: make a better UI
                    Text(
                        text = work!!.text
                    )
                }
            }
        }

        error != null -> Text("Error: $error")
        else -> CircularProgressIndicator()
    }
}

private fun Work.computeTags(): List<String> {
    val tags = mutableListOf<String>()
    if (fandoms.isNotEmpty()) {
        tags.addAll(fandoms)
    }
    if (characters.isNotEmpty()) {
        tags.addAll(characters)
    }
    if (relationships.isNotEmpty()) {
        tags.addAll(relationships)
    }
    if (tags.isNotEmpty()) {
        tags.addAll(tags)
    }
    return tags.distinct()
}
