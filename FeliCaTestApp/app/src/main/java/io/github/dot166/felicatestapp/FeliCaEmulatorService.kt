package io.github.dot166.felicatestapp

import android.nfc.cardemulation.HostNfcFService
import android.os.Bundle

class FeliCaEmulatorService: HostNfcFService() {
    override fun onDeactivated(p0: Int) {
        // do nothing
    }

    override fun processNfcFPacket(
        p0: ByteArray?,
        p1: Bundle?
    ): ByteArray? {
        return null
    }
}