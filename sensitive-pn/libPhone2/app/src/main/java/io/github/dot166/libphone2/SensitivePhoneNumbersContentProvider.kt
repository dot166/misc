package io.github.dot166.libphone2

import android.annotation.SuppressLint
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.telephony.SubscriptionManager
import io.github.dot166.libphone2.spn.Item

class SensitivePhoneNumbersContentProvider : ContentProvider() {
    @SuppressLint("MissingPermission")
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val context = context ?: return null
        if (arg != null || extras == null || extras.isEmpty) {
            return null
        }
        return when (method) {
            SensitivePhoneNumbers.FUNCTION_IS_SENSITIVE_NUMBER -> {
                val numberToCheck = extras.getString(SensitivePhoneNumbers.PARAMETER_NUMBER_TO_CHECK)
                val subId = extras.getInt(SensitivePhoneNumbers.PARAMETER_SUB_ID, SubscriptionManager.INVALID_SUBSCRIPTION_ID)
                if (numberToCheck.isNullOrBlank()) {
                    null
                } else {
                    Bundle().apply {
                        putBoolean(
                            SensitivePhoneNumbers.FUNCTION_IS_SENSITIVE_NUMBER,
                            SensitivePhoneNumbersImpl.instance.isSensitiveNumber(
                                context,
                                numberToCheck,
                                subId
                            )
                        )
                    }
                }
            }
            SensitivePhoneNumbers.FUNCTION_GET_SENSITIVE_PN_INFOS_FOR_MCC -> {
                val mcc = extras.getString(SensitivePhoneNumbers.PARAMETER_MCC)
                if (mcc.isNullOrBlank()) {
                    null
                } else {
                    Bundle().apply {
                        classLoader = Item::class.java.classLoader
                        putParcelableArrayList(
                            SensitivePhoneNumbers.FUNCTION_GET_SENSITIVE_PN_INFOS_FOR_MCC,
                            SensitivePhoneNumbersImpl.instance.getSensitivePnInfosForMcc(
                                context,
                                mcc
                            )
                        )
                    }
                }
            }
            else -> null
        }
    }
    override fun getType(uri: Uri): String {
        throw RuntimeException("Unsupported Function")
    }
    override fun onCreate(): Boolean {
        return true
    }
    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor {
        throw RuntimeException("Unsupported Function")
    }
    override fun insert(uri: Uri, values: ContentValues?): Uri {
        throw RuntimeException("Unsupported Function")
    }
    override fun update(
        uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<String?>?
    ): Int {
        throw RuntimeException("Unsupported Function")
    }
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String?>?): Int {
        throw RuntimeException("Unsupported Function")
    }
}