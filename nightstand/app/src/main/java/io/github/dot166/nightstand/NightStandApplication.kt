package io.github.dot166.nightstand

import io.github.dot166.jlib.app.CoreApplication

class NightStandApplication: CoreApplication() {
    override fun onCreate() {
        super.onCreate()
        Utils.init(this)
    }
}