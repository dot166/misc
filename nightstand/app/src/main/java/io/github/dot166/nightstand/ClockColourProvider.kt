package io.github.dot166.nightstand

import android.content.Context
import com.android.settingslib.spa.widget.preference.ListPreferenceOption
import io.github.dot166.nightstand.Utils.screensaverClockColor

class ClockColourProvider(ctx: Context) {

    val colours: List<Pair<String, String>>
    init {
        val list = mutableListOf<Pair<String, String>>()
        val entries = ctx.resources.getStringArray(R.array.clock_color_entries)
        val values = listOf("FFFFFF", "FF0000", "00FF00", "0000FF", "FFC0CB")
        for ((i, colour) in values.withIndex()) {
            list.add(Pair(entries[i], colour))
        }
        colours = list
    }

    fun getColoursAsListOptions(): MutableList<ListPreferenceOption> {
        val options = mutableListOf<ListPreferenceOption>()
        for ((i, provider) in colours.withIndex()) {
            options.add(ListPreferenceOption(i, provider.first, provider.second))
        }
        return options
    }
    fun getColour(i: Int): String {
        if (i >= colours.size) {
            screensaverClockColor // just return the saved one
        }
        val provider = colours[i]
        return provider.second
    }
    fun getIndexOfColour(s: String): Int {
        for ((i, provider) in colours.withIndex()) {
            if (provider.second == s) {
                return i
            }
        }
        return 0
    }
}