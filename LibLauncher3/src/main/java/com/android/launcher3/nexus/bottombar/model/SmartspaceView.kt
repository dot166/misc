package com.android.launcher3.nexus.bottombar.model

import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

interface SmartspaceView : Parcelable

interface SmartspaceButtonView: SmartspaceView {
    val pendingIntent: PendingIntent?
    val intent: Intent?
    @IgnoredOnParcel
    val onClick: Runnable?
}

@Parcelize
data class SmartspaceIconView(val icon: Icon, val contentDescription: String) : SmartspaceView

@Parcelize
data class SmartspaceTextView(val text: String, val contentDescription: String = text) : SmartspaceView

@Parcelize
data class SmartspaceIconButton(val icon: Icon, val contentDescription: String,
    override val pendingIntent: PendingIntent? = null,
    override val intent: Intent? = null,
    @IgnoredOnParcel
    override val onClick: Runnable? = null) : SmartspaceButtonView

@Parcelize
data class SmartspaceTextButton(val text: String, val contentDescription: String = text,
    override val pendingIntent: PendingIntent? = null,
    override val intent: Intent? = null,
    @IgnoredOnParcel
    override val onClick: Runnable? = null) : SmartspaceButtonView
