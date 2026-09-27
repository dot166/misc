package com.android.launcher3.nexus.bottombar.model

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Bundle
import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Deprecated("SmartspaceAction is deprecated")
@Parcelize
data class SmartspaceAction(
    val id: String,
    val icon: Icon? = null,
    val title: CharSequence,
    val subtitle: CharSequence? = null,
    val contentDescription: CharSequence? = null,
    val pendingIntent: PendingIntent? = null,
    val intent: Intent? = null,
    @IgnoredOnParcel
    val onClick: Runnable? = null,
    val extras: Bundle? = null,
) : Parcelable
