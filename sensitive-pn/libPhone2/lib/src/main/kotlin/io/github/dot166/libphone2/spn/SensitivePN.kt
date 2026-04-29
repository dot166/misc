package io.github.dot166.libphone2.spn

import org.xmlpull.v1.XmlPullParser
import javax.xml.datatype.DatatypeConfigurationException

class SensitivePN {
    var item: MutableList<Item>? = null
        get() {
            if (field == null) {
                field = ArrayList()
            }
            return field!!
        }
    var network: String? = null

    fun read(xmlPullParser: XmlPullParser): SensitivePN {
        var next: Int
        val attributeValue = xmlPullParser.getAttributeValue(null, "network")
        if (attributeValue != null) {
            network = attributeValue
        }
        xmlPullParser.depth
        while (true) {
            next = xmlPullParser.next()
            if (next == 1 || next == 3) {
                break
            }
            if (xmlPullParser.eventType == 2) {
                if (xmlPullParser.name == "item") {
                    item!!.add(Item().read(xmlPullParser))
                } else {
                    XmlParser.skip(xmlPullParser)
                }
            }
        }
        if (next == 3) {
            return this
        }
        throw DatatypeConfigurationException("SensitivePN is not closed")
    }
}