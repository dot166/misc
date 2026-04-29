package org.lineageos.lib.phone

import io.github.dot166.libphone2.flags.Flags
import org.lineageos.lib.phone.spn.Item
import io.github.dot166.libphone2.spn.Item as Item2

@Deprecated("This is only for backwards compatibility, please move to the libPhone2 classes")
fun ArrayList<Item2>.convertToCompat(): ArrayList<Item> {
    if (Flags.isCompatWithLibphone1Enabled()) {
        val array = ArrayList<Item>()
        for (item in this) {
            array.add(item as Item)
        }
        return array
    } else {
        throw UnsupportedOperationException("Compat with libPhone1 is disabled")
    }
}