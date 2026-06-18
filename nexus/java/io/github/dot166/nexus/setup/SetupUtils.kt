package io.github.dot166.nexus.setup

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory

object SetupUtils {
    fun backToSuW(activity: Activity?) {
        if (activity != null) {
            activity.apply {
                setResult(Activity.RESULT_OK, null)
                finish()
            }
        } else {
            // well, fuck
            SpaEnvironmentFactory.instance.logger.message("well, fuck", "activity doesn't exist, we cant exit cleanly, crash it and hope")
            throw Exception()
        }
    }
}