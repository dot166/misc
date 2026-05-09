/*
 * SPDX-FileCopyrightText: 2017 The Android Open Source Project
 * SPDX-FileCopyrightText: 2017-2021 The LineageOS Project
 * SPDX-FileCopyrightText: 2026 ._______166
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lib.phone

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import io.github.dot166.libphone2.flags.Flags
import org.lineageos.lib.phone.spn.Item
import io.github.dot166.libphone2.SensitivePhoneNumbers as SensitivePhoneNumbers2

@Deprecated("This is only for backwards compatibility, please move to the libPhone2 classes")
class SensitivePhoneNumbers private constructor() {

    @Deprecated("This is only for backwards compatibility, please move to the libPhone2 classes")
    fun getSensitivePnInfosForMcc(mcc: String?): ArrayList<Item> {
        if (Flags.isCompatWithLibphone1Enabled()) {
            return SensitivePhoneNumbers2.instance.getSensitivePnInfosForMcc(mcc).convertToCompat()
        } else {
            throw UnsupportedOperationException("Compat with libPhone1 is disabled")
        }
    }

    @Deprecated("This is only for backwards compatibility, please move to the libPhone2 classes")
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    fun isSensitiveNumber(context: Context, numberToCheck: String?, subId: Int): Boolean {
        if (Flags.isCompatWithLibphone1Enabled()) {
            return SensitivePhoneNumbers2.instance.isSensitiveNumber(context, numberToCheck, subId)
        } else {
            throw UnsupportedOperationException("Compat with libPhone1 is disabled")
        }
    }

    companion object {
        @Deprecated("This is only for backwards compatibility, please move to the libPhone2 classes")
        private var sInstance: SensitivePhoneNumbers? = null
        @Deprecated("This is only for backwards compatibility, please move to the libPhone2 classes")
        val instance: SensitivePhoneNumbers
            get() {
                if (sInstance == null) {
                    sInstance = SensitivePhoneNumbers()
                }
                return sInstance!!
            }
    }
}