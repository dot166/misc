package io.github.dot166.nexus

import io.github.dot166.jlib.app.JLibSpaEnvironmentStub
import io.github.dot166.jlib.app.RestorableSettingsApplication

class NexusApplication: RestorableSettingsApplication() {
    override fun onCreate() {
        super.onCreate()
        setSpaEnvironment(NexusSpaEnvironment(this))
    }
}