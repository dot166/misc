package io.github.dot166.libphone2.spn

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Item(
    val categories: String?,
    val languages: String?,
    val name: String?,
    val number: String,
    val organization: String?,
    val website: String?
) : Parcelable