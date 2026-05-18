package io.github.dot166.nexus

import android.Manifest
import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.hardware.input.InputManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.annotation.RequiresPermission
import androidx.core.view.doOnPreDraw
import androidx.preference.PreferenceManager
import com.google.android.gsa.overlay.controllers.OverlayController

class NexusOverlay(
    private val uid: Int,
    context: Context
): OverlayController(
    context,
    0,
    android.R.style.Theme_Translucent_NoTitleBar
) {

    companion object {
        private const val KEY_BACKGROUND_BLUR_PROGRESS = "background_blur_progress"
    }

    private var backgroundBlurProgress = 0f
    private val blurProvider = BlurProvider(resources)
    private var isResumed = false
    private var lockOn = false
    private var lockOff = true
    private var vd: VirtualDisplay? = null

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
        window?.decorView?.removeStatusNavBackgroundOnPreDraw()
        backgroundBlurProgress = bundle
            ?.getFloat(KEY_BACKGROUND_BLUR_PROGRESS, 0f) ?: 0f
    }

    override fun onPause() {
        if (!isResumed) return
        isResumed = false
        super.onPause()
    }

    override fun onDestroy(isFinishing: Boolean) {
        onPause()
        super.onDestroy(isFinishing)
    }

    override fun onStop() {
        onPause()
        super.onStop()
        blurProvider.applyBlurToWindow(window!!, 0f)
    }

    override fun onResume() {
        if (isResumed) return
        isResumed = true
        super.onResume()
        updateProgressViews(backgroundBlurProgress, true)
    }

    override fun onDragProgress(progress: Float) {
        super.onDragProgress(progress)
        updateProgressViews(progress)
        Log.i("AAAAAA", progress.toString())
        if (progress > 0.0f) {
            if (!lockOn) {
                lockOn = true
                lockOff = false
                val surface = TextureView(this)
                surface.setOnTouchListener(object : View.OnTouchListener {
                    @RequiresPermission(Manifest.permission.INJECT_EVENTS)
                    override fun onTouch(
                        v: View,
                        event: MotionEvent?
                    ): Boolean {
                        val currentVd = vd ?: return false
                        val copy = MotionEvent.obtain(event)
                        copy.displayId = currentVd.display.displayId
                        val im = getSystemService(INPUT_SERVICE) as InputManager
                        im.injectInputEvent(copy, InputManager.INJECT_INPUT_EVENT_MODE_ASYNC)
                        copy.recycle()
                        return true
                    }
                })
                surface.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(
                        surface: SurfaceTexture,
                        width: Int,
                        height: Int
                    ) {
                        val dm = getSystemService(DISPLAY_SERVICE) as DisplayManager
                        val metrics = DisplayMetrics()
                        dm.getDisplay(Display.DEFAULT_DISPLAY).getRealMetrics(metrics)
                        vd =
                            dm.createVirtualDisplay(
                                "overlay",
                                width,
                                height,
                                metrics.densityDpi,
                                Surface(surface),
                                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC or DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY or DisplayManager.VIRTUAL_DISPLAY_FLAG_DESTROY_CONTENT_ON_REMOVAL
                            )
                        val options = ActivityOptions.makeBasic()
                        options.setLaunchDisplayId(vd!!.display.displayId)
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
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        startActivity(intent, options.toBundle())
                    }

                    override fun onSurfaceTextureSizeChanged(
                        surface: SurfaceTexture,
                        width: Int,
                        height: Int
                    ) {
                        if (vd == null) {
                            return
                        }
                        val dm = getSystemService(DISPLAY_SERVICE) as DisplayManager
                        val metrics = DisplayMetrics()
                        dm.getDisplay(Display.DEFAULT_DISPLAY).getRealMetrics(metrics)
                        vd!!.resize(width, height, metrics.densityDpi)
                    }

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        closePanel()
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                    }
                }
                container?.addView(surface.removeStatusNavBackgroundOnPreDraw())
            }
        } else if (progress == 0.0f) {
            if (!lockOff) {
                lockOff = true
                lockOn = false
                if (vd == null) {
                    container?.removeAllViews() // nuke it
                    return
                }
                val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
                val tasks = am.getAppTasks()

                for (task in tasks) {
                    val info = task.taskInfo
                    if (info.displayId == vd!!.display.displayId) {
                        task.finishAndRemoveTask()
                    }
                }
                vd!!.release()
                vd = null
                container?.removeAllViews() // nuke it
            }
        }
    }

    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
        bundle.putFloat(KEY_BACKGROUND_BLUR_PROGRESS, backgroundBlurProgress)
    }

    private fun updateProgressViews(progress: Float, force: Boolean = false) {
        if (backgroundBlurProgress == progress && !force) return
        backgroundBlurProgress = progress
        container?.background = null
        blurProvider.applyBlurToWindow(window!!, progress)
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
