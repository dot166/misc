package io.github.dot166.nexus

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Display
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.preference.PreferenceManager
import com.google.android.gsa.overlay.controllers.OverlayController

class NexusOverlay(
    private val uid: Int,
    context: Context
): OverlayController(
    context,
    0,
    android.R.style.Theme_Translucent_NoTitleBar
), LifecycleOwner {

    private val lifecycleRegistry by lazy {
        LifecycleRegistry(this)
    }

    override val lifecycle
        get() = lifecycleRegistry
    private var backgroundBlurProgress = 0f
    private val blurProvider = BlurProvider(resources)
    private var isResumed = false

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        container?.fitsSystemWindows = false
        window?.let {
            it.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            it.addFlags(Window.FEATURE_NO_TITLE)
            it.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            it.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            it.statusBarColor = Color.TRANSPARENT
            it.navigationBarColor = Color.TRANSPARENT
        }
        lifecycleRegistry.handleLifecycleEventSafely(Lifecycle.Event.ON_CREATE)
        window?.decorView?.removeStatusNavBackgroundOnPreDraw()
        val surface = SurfaceView(this)
        val dm = getSystemService(DISPLAY_SERVICE) as DisplayManager
        val metrics = DisplayMetrics()
        dm.getDisplay(Display.DEFAULT_DISPLAY).getRealMetrics(metrics)
        var vd: VirtualDisplay? = null
        surface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                val surfaceWidth = holder.surfaceFrame.width()
                val surfaceHeight = holder.surfaceFrame.height()
                vd =
                    dm.createVirtualDisplay(
                        "overlay",
                        surfaceWidth,
                        surfaceHeight,
                        metrics.densityDpi,
                        holder.surface,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY
                    )
                val options = ActivityOptions.makeBasic()
                options.setLaunchDisplayId(vd.display.displayId)
                val intent: Intent =
                    Intent("io.github.dot166.nexus.WINDOW_OVERLAY_PROVIDER").apply {
                        component =
                            ComponentName.unflattenFromString(
                                PreferenceManager.getDefaultSharedPreferences(
                                    this@NexusOverlay
                                ).getString(
                                    "feed_provider",
                                    "io.github.dot166.nexus/io.github.dot166.nexus.DefaultStub"
                                )
                                    ?: "io.github.dot166.nexus/io.github.dot166.nexus.DefaultStub" // return stub activity when not set or null, prevents user confusion
                            )
                    }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent, options.toBundle())
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                vd?.resize(width, height, metrics.densityDpi)
                vd?.surface = holder.surface
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                vd?.release()
            }
        })
        container?.addView(surface.removeStatusNavBackgroundOnPreDraw())
    }

    override fun onPause() {
        if (!isResumed) return
        isResumed = false
        super.onPause()
        lifecycleRegistry.handleLifecycleEventSafely(Lifecycle.Event.ON_PAUSE)
    }

    override fun onDestroy(isFinishing: Boolean) {
        onPause()
        super.onDestroy(isFinishing)
        lifecycleRegistry.handleLifecycleEventSafely(Lifecycle.Event.ON_DESTROY)
    }

    override fun onStop() {
        onPause()
        super.onStop()
        lifecycleRegistry.handleLifecycleEventSafely(Lifecycle.Event.ON_STOP)
        blurProvider.applyBlurToWindow(window!!, 0f)
    }

    override fun onStart() {
        super.onStart()
        lifecycleRegistry.handleLifecycleEventSafely(Lifecycle.Event.ON_START)
    }

    override fun onResume() {
        if (isResumed) return
        isResumed = true
        super.onResume()
        lifecycleRegistry.handleLifecycleEventSafely(Lifecycle.Event.ON_RESUME)
        //updateProgressViews(backgroundBlurProgress, true)
    }

    override fun onDragProgress(progress: Float) {
        super.onDragProgress(progress)
        //updateProgressViews(progress)
    }

    private fun updateProgressViews(progress: Float, force: Boolean = false) {
        if (backgroundBlurProgress == progress && !force) return
        backgroundBlurProgress = progress
        container?.background = null
        blurProvider.applyBlurToWindow(window!!, progress)
    }

    fun LifecycleRegistry.handleLifecycleEventSafely(event: Lifecycle.Event) {
        try {
            handleLifecycleEvent(event)
        } catch (e: IllegalStateException) {
            //Already at this event
        }
    }

    fun View.removeStatusNavBackgroundOnPreDraw() = apply {
        doOnPreDraw {
            val statusBarBackground = it.findViewById<View>(android.R.id.statusBarBackground)
            statusBarBackground?.run {
                visibility = View.INVISIBLE
                alpha = 0f
            }
            val navigationBarBackground =
                it.findViewById<View>(android.R.id.navigationBarBackground)
            navigationBarBackground?.run {
                visibility = View.INVISIBLE
                alpha = 0f
            }
        }
    }

    override fun getSystemServiceName(serviceClass: Class<*>): String? {
        // fix compose by blocking requests to a service that doesn't exist for the 'window in a service' mess that L3 has for -1
        if (serviceClass.name.contains("ContentCapture")) {
            return null // "I don't know her. She doesn't live here."
        }
        return super.getSystemServiceName(serviceClass)
    }

}