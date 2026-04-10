package io.github.dot166.libphone2

import android.Manifest
import android.content.Context
import android.telephony.PhoneNumberUtils
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat
import com.google.i18n.phonenumbers.Phonenumber
import io.github.dot166.libphone2.spn.Item
import io.github.dot166.libphone2.spn.XmlParser
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import javax.xml.datatype.DatatypeConfigurationException

open class SensitivePhoneNumbers private constructor() {
    private val logTag: String = this.javaClass.getSimpleName()

    private val mSensitiveNumbersMap: HashMap<String?, ArrayList<Item>> =
        HashMap()

    private fun loadSensiblePhoneNumbers() {
        if (sNumbersLoaded) {
            return
        }

        val sensiblePNFile = File(SENSIBLE_PHONENUMBERS_FILE_PATH)
        val sensiblePNInputStream: FileInputStream?

        try {
            sensiblePNInputStream = FileInputStream(sensiblePNFile)
        } catch (_: FileNotFoundException) {
            Log.w(logTag, "Can not open " + sensiblePNFile.absolutePath)
            return
        }

        try {
            for (sensitivePN in XmlParser.read(sensiblePNInputStream)!!.sensitivePN!!) {
                val mccs: List<String> = sensitivePN.network!!.split(",")
                for (mcc in mccs) {
                    mSensitiveNumbersMap[mcc] = ArrayList(sensitivePN.item!!)
                }
            }
        } catch (e: DatatypeConfigurationException) {
            Log.w(logTag, "Exception in spn-conf parser", e)
        } catch (e: IOException) {
            Log.w(logTag, "Exception in spn-conf parser", e)
        } catch (e: XmlPullParserException) {
            Log.w(logTag, "Exception in spn-conf parser", e)
        }

        sNumbersLoaded = true
    }

    open fun getSensitivePnInfosForMcc(mcc: String?): ArrayList<Item> {
        loadSensiblePhoneNumbers()
        return mSensitiveNumbersMap.getOrDefault(mcc, ArrayList())
    }

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    open fun isSensitiveNumber(context: Context, numberToCheck: String?, subId: Int): Boolean {
        var subId = subId
        val nationalNumber = formatNumberToNational(context, numberToCheck)
        if (TextUtils.isEmpty(nationalNumber)) {
            return false
        }
        loadSensiblePhoneNumbers()

        var telephonyManager =
            context.getSystemService(TelephonyManager::class.java)
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            subId = SubscriptionManager.getDefaultSubscriptionId()
        }
        telephonyManager = telephonyManager!!.createForSubscriptionId(subId)
        val subManager =
            context.getSystemService(SubscriptionManager::class.java)
        val list = subManager!!.getActiveSubscriptionInfoList()
        if (list != null) {
            // Test all subscriptions so an accidental use of a wrong sim also hides the number
            for (subInfo in list) {
                val mcc = subInfo.mccString
                if (isSensitiveNumber(nationalNumber, mcc, telephonyManager.networkCountryIso)) {
                    return true
                }
            }
        }

        // Fall back to check with the passed subId
        val networkUsed = telephonyManager.networkOperator
        if (!TextUtils.isEmpty(networkUsed)) {
            val networkMCC = networkUsed!!.substring(0, 3)
            if (isSensitiveNumber(nationalNumber, networkMCC, telephonyManager.networkCountryIso)) {
                return true
            }
        }

        // Also try the sim's operator
        if (telephonyManager.getSimState() == TelephonyManager.SIM_STATE_READY) {
            val simOperator = telephonyManager.simOperator
            if (!TextUtils.isEmpty(simOperator)) {
                val networkMCC = simOperator!!.substring(0, 3)
                if (isSensitiveNumber(nationalNumber, networkMCC, telephonyManager.networkCountryIso)) {
                    return true
                }
            }
        }

        return false
    }

    private fun isSensitiveNumber(numberToCheck: String?, mcc: String?, countryIso: String): Boolean {
        if (mSensitiveNumbersMap.containsKey(mcc)) {
            for (item in mSensitiveNumbersMap[mcc]!!) {
                if (PhoneNumberUtils.areSamePhoneNumber(numberToCheck!!, item.number!!, countryIso)) {
                    return true
                }
            }
        }
        return false
    }

    private fun formatNumberToNational(context: Context, number: String?): String? {
        val util: PhoneNumberUtil = PhoneNumberUtil.getInstance()
        val countryIso = context.resources.configuration.locales[0].country

        var pn: Phonenumber.PhoneNumber? = null
        try {
            pn = util.parse(number, countryIso)
        } catch (_: NumberParseException) {
        }

        return if (pn != null) {
            util.format(pn, PhoneNumberFormat.NATIONAL)
        } else {
            number
        }
    }

    companion object {
        const val SENSIBLE_PHONENUMBERS_FILE_PATH: String = "/product/etc/sensitive_pn.xml"

        private var sInstance: SensitivePhoneNumbers? = null
        private var sNumbersLoaded = false

        val instance: SensitivePhoneNumbers
            get() {
                if (sInstance == null) {
                    sInstance = SensitivePhoneNumbers()
                }
                return sInstance!!
            }
    }
}