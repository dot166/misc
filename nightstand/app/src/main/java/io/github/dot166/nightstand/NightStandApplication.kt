package io.github.dot166.nightstand

import io.github.dot166.jlib.app.RestorableSettingsApplication

class NightStandApplication: RestorableSettingsApplication() {
    override fun onCreate() {
        super.onCreate()
        Utils.init(this)
        setSpaEnvironment(NightStandSpaEnvironment(this))
    }
}