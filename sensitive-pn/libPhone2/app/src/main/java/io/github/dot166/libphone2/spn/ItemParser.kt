package io.github.dot166.libphone2.spn

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import javax.xml.datatype.DatatypeConfigurationException

object ItemParser {
    @Throws(
        XmlPullParserException::class,
        DatatypeConfigurationException::class,
        IOException::class
    )
    fun read(xmlPullParser: XmlPullParser): Item {
        var categories: String? = null
        var languages: String? = null
        var name: String? = null
        var number: String? = null
        var organization: String? = null
        var website: String? = null
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
            if (number == null) {
                throw XmlPullParserException("Item has no number, invalid")
            }
            return Item(categories, languages, name, number, organization, website)
        }
        throw DatatypeConfigurationException("Item is not closed")
    }
}