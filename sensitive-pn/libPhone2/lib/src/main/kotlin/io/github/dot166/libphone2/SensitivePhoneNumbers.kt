package io.github.dot166.libphone2

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresPermission
import io.github.dot166.libphone2.spn.Item

class SensitivePhoneNumbers {
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    fun getSensitivePnInfosForMcc(context: Context, mcc: String?): ArrayList<Item> {
        val result = ArrayList<Item>()

        if (mcc.isNullOrBlank()) {
            return result
        }

        val bundle = context.contentResolver.call(
            PROVIDER_NAME,
            FUNCTION_GET_SENSITIVE_PN_INFOS_FOR_MCC,
            null,
            Bundle().apply {
                putString(PARAMETER_MCC, mcc)
            }
        ) ?: return result

        result.addAll(bundle.getItemList(FUNCTION_GET_SENSITIVE_PN_INFOS_FOR_MCC))

        return result
    }
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    fun isSensitiveNumber(context: Context, numberToCheck: String?, subId: Int): Boolean {
        if (numberToCheck.isNullOrBlank()) {
            return false
        }
        val bundle = context.contentResolver.call(
            PROVIDER_NAME,
            FUNCTION_IS_SENSITIVE_NUMBER,
            null,
            Bundle().apply {
                putString(PARAMETER_NUMBER_TO_CHECK, numberToCheck)
                putInt(PARAMETER_SUB_ID, subId)
            }
        ) ?: return false
        return bundle.getBoolean(FUNCTION_IS_SENSITIVE_NUMBER)
    }
    companion object {
        private fun Bundle.getItemList(string: String): List<Item> {
            classLoader = Item::class.java.classLoader
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getParcelableArrayList(string, Item::class.java)
            } else {
                getParcelableArrayList(string)
            } ?: emptyList()
        }
        const val PROVIDER_NAME = "io.github.dot166.libphone2.database"
        const val FUNCTION_IS_SENSITIVE_NUMBER = "isSensitiveNumber"
        const val FUNCTION_GET_SENSITIVE_PN_INFOS_FOR_MCC = "getSensitivePnInfosForMcc"
        const val PARAMETER_NUMBER_TO_CHECK = "numberToCheck"
        const val PARAMETER_SUB_ID = "subId"
        const val PARAMETER_MCC = "mcc"
    }
}
