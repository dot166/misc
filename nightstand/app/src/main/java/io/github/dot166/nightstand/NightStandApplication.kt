package io.github.dot166.nightstand

import android.app.Application

class NightStandApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        Utils.init(this)
    }
}