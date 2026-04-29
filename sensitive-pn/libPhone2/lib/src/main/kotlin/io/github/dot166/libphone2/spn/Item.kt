package io.github.dot166.libphone2.spn

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import javax.xml.datatype.DatatypeConfigurationException

open class Item {
    open var categories: String? = null
    open var languages: String? = null
    open var name: String? = null
    open var number: String? = null
    open var organization: String? = null
    open var website: String? = null

    @Throws(
        XmlPullParserException::class,
        DatatypeConfigurationException::class,
        IOException::class
    )
    open fun read(xmlPullParser: XmlPullParser): Item {
        var next: Int
        xmlPullParser.depth
        while (true) {
            next = xmlPullParser.next()
            if (next == 1 || next == 3) {
                break
            }
            if (xmlPullParser.eventType == 2) {
                val xmlName = xmlPullParser.name
                when (xmlName) {
                    "number" -> {
                        number =
                            XmlParser.readText(xmlPullParser)
                    }
                    "name" -> {
                        name = XmlParser.readText(xmlPullParser)
                    }
                    "categories" -> {
                        categories =
                            XmlParser.readText(xmlPullParser)
                    }
                    "languages" -> {
                        languages =
                            XmlParser.readText(xmlPullParser)
                    }
                    "organization" -> {
                        organization =
                            XmlParser.readText(xmlPullParser)
                    }
                    "website" -> {
                        website =
                            XmlParser.readText(xmlPullParser)
                    }
                    else -> {
                        XmlParser.skip(xmlPullParser)
                    }
                }
            }
        }
        if (next == 3) {
            return this
        }
        throw DatatypeConfigurationException("Item is not closed")
    }
}