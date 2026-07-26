package com.google.android.gsa.overlay

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.google.android.gsa.overlay.controllers.OverlaysController
import com.google.android.gsa.overlay.controllers.OverlayController as GsaOverlayController

abstract class MinusOneService: Service() {

    private lateinit var overlaysController: OverlaysController

    abstract fun getOverlay(uid: Int, context: Context): NexusOverlay

    override fun onCreate() {
        super.onCreate()
        overlaysController = OverlayController()
    }

    override fun onDestroy() {
        overlaysController.onDestroy()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return overlaysController.onBind(intent, null)
    }

    override fun onUnbind(intent: Intent): Boolean {
        overlaysController.onUnbind(intent)
        return false
    }

    private inner class OverlayController: ConfigurationOverlayController(this) {
        override fun getOverlay(uid: Int, context: Context): GsaOverlayController {
            return this@MinusOneService.getOverlay(uid, context)
        }
    }
}
