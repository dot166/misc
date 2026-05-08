package io.github.dot166.nexus

import android.content.Context
import io.github.dot166.jlib.app.RestorableSettingsApplication
import org.lsposed.hiddenapibypass.HiddenApiBypass

class Application: RestorableSettingsApplication() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        HiddenApiBypass.addHiddenApiExemptions("") // temporary, until privileged
    }
}