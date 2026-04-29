package org.lineageos.lib.phone.spn

import io.github.dot166.libphone2.flags.Flags
import io.github.dot166.libphone2.spn.Item as Item2
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import javax.xml.datatype.DatatypeConfigurationException

@Deprecated("This is only for backwards compatibility, please move to the libPhone2 classes")
class Item : Item2() {
    @Deprecated("This is only for backwards compatibility, please move to the libPhone2 classes")
    override var number: String?
        @Deprecated("This is only for backwards compatibility, please move to the libPhone2 classes")
        get() {
            if (Flags.isCompatWithLibphone1Enabled()) {
                return super.number
            } else {
                throw UnsupportedOperationException("Compat with libPhone1 is disabled")
            }
        }
        @Deprecated("This is only for backwards compatibility, please move to the libPhone2 classes")
        set(str) {
            if (Flags.isCompatWithLibphone1Enabled()) {
                super.number = str
            } else {
                throw UnsupportedOperationException("Compat with libPhone1 is disabled")
            }
        }

    @Deprecated("This is only for backwards compatibility, please move to the libPhone2 classes")
    override var name: String?
        @Deprecated("This is only for backwards compatibility, please move to the libPhone2 classes")
        get() {
            if (Flags.isCompatWithLibphone1Enabled()) {
                return super.name
            } else {
                throw UnsupportedOperationException("Compat with libPhone1 is disabled")
            }
        }
        @Deprecated("This is only for backwards compatibility, please move to the libPhone2 classes")
        set(str) {
            if (Flags.isCompatWithLibphone1Enabled()) {
                super.name = str
            } else {
                throw UnsupportedOperationException("Compat with libPhone1 is disabled")
            }
        }

    @Deprecated("This is only for backwards compatibility, please move to the libPhone2 classes")
    override var categories: String?
        @Deprecated("This is only for backwards compatibility, please move to the libPhone2 classes")
        get() {
            if (Flags.isCompatWithLibphone1Enabled()) {
                return super.categories
            } else {
                throw UnsupportedOperationException("Compat with libPhone1 is disabled")
            }
        }
        @Deprecated("This is only for backwards compatibility, please move to the libPhone2 classes")
        set(str) {
            if (Flags.isCompatWithLibphone1Enabled()) {
                super.categories = str
            } else {
                throw UnsupportedOperationException("Compat with libPhone1 is disabled")
            }
        }

    @Deprecated("This is only for backwards compatibility, please move to the libPhone2 classes")
    override var languages: String?
        @Deprecated("This is only for backwards compatibility, please move to the libPhone2 classes")
        get() {
            if (Flags.isCompatWithLibphone1Enabled()) {
                return super.languages
            } else {
                throw UnsupportedOperationException("Compat with libPhone1 is disabled")
            }
        }
        @Deprecated("This is only for backwards compatibility, please move to the libPhone2 classes")
        set(str) {
            if (Flags.isCompatWithLibphone1Enabled()) {
                super.languages = str
            } else {
                throw UnsupportedOperationException("Compat with libPhone1 is disabled")
            }
        }

    @Deprecated("This is only for backwards compatibility, please move to the libPhone2 classes")
    override var organization: String?
        @Deprecated("This is only for backwards compatibility, please move to the libPhone2 classes")
        get() {
            if (Flags.isCompatWithLibphone1Enabled()) {
                return super.organization
            } else {
                throw UnsupportedOperationException("Compat with libPhone1 is disabled")
            }
        }
        @Deprecated("This is only for backwards compatibility, please move to the libPhone2 classes")
        set(str) {
            if (Flags.isCompatWithLibphone1Enabled()) {
                super.organization = str
            } else {
                throw UnsupportedOperationException("Compat with libPhone1 is disabled")
            }
        }

    @Deprecated("This is only for backwards compatibility, please move to the libPhone2 classes")
    override var website: String?
        @Deprecated("This is only for backwards compatibility, please move to the libPhone2 classes")
        get() {
            if (Flags.isCompatWithLibphone1Enabled()) {
                return super.website
            } else {
                throw UnsupportedOperationException("Compat with libPhone1 is disabled")
            }
        }
        @Deprecated("This is only for backwards compatibility, please move to the libPhone2 classes")
        set(str) {
            if (Flags.isCompatWithLibphone1Enabled()) {
                super.website = str
            } else {
                throw UnsupportedOperationException("Compat with libPhone1 is disabled")
            }
        }

    @Deprecated("This is only for backwards compatibility, please move to the libPhone2 classes")
    @Throws(
        XmlPullParserException::class,
        DatatypeConfigurationException::class,
        IOException::class
    )
    override fun read(xmlPullParser: XmlPullParser): Item {
        if (Flags.isCompatWithLibphone1Enabled()) {
            return super.read(xmlPullParser) as Item
        } else {
            throw UnsupportedOperationException("Compat with libPhone1 is disabled")
        }
    }
}