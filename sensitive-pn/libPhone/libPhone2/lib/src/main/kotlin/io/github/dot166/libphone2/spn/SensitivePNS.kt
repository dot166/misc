package io.github.dot166.libphone2.spn

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import javax.xml.datatype.DatatypeConfigurationException

class SensitivePNS {
    var sensitivePN: MutableList<SensitivePN>? = null
        get() {
            if (field == null) {
                field = ArrayList()
            }
            return field!!
        }

    @Throws(
        XmlPullParserException::class,
        DatatypeConfigurationException::class,
        IOException::class
    )
    fun read(xmlPullParser: XmlPullParser): SensitivePNS {
        var next: Int
        xmlPullParser.depth
        while (true) {
            next = xmlPullParser.next()
            if (next == 1 || next == 3) {
                break
            }
            if (xmlPullParser.eventType == 2) {
                if (xmlPullParser.name == "sensitivePN") {
                    sensitivePN!!.add(SensitivePN().read(xmlPullParser))
                } else {
                    XmlParser.skip(xmlPullParser)
                }
            }
        }
        if (next == 3) {
            return this
        }
        throw DatatypeConfigurationException("SensitivePNS is not closed")
    }
}