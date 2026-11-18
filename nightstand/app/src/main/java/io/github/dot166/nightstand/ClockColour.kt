package io.github.dot166.nightstand

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.patrickgold.jetpref.datastore.ui.ListPreferenceEntry
import dev.patrickgold.jetpref.datastore.ui.listPrefEntries

enum class ClockColour(val value: String, val labelRes: Int) {
    WHITE("FFFFFF", R.string.white),
    RED("FF0000", R.string.red),
    GREEN("00FF00", R.string.green),
    BLUE("0000FF", R.string.blue),
    PINK("FFC0CB", R.string.pink);

    companion object {
        @Composable
        fun toListPrefEntries(): List<ListPreferenceEntry<ClockColour>> = listPrefEntries {
            for (color in entries) {
                entry(
                    key = color,
                    label = stringResource(color.labelRes),
                )
            }
        }
    }
}