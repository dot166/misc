package io.github.dot166.libphone2.spn

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.InputStream

object XmlParser {
    @Throws(XmlPullParserException::class, IOException::class)
    fun read(inputStream: InputStream?): SensitivePNS? {
        val xmlPullParserNewPullParser = XmlPullParserFactory.newInstance().newPullParser()
        xmlPullParserNewPullParser.setFeature(
            "http://xmlpull.org/v1/doc/features.html#process-namespaces",
            true
        )
        xmlPullParserNewPullParser.setInput(inputStream, null)
        xmlPullParserNewPullParser.nextTag()
        if (xmlPullParserNewPullParser.name == "sensitivePNS") {
            return SensitivePNS().read(xmlPullParserNewPullParser)
        }
        return null
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun readText(xmlPullParser: XmlPullParser): String? {
        if (xmlPullParser.next() != 4) {
            return ""
        }
        val text = xmlPullParser.text
        xmlPullParser.nextTag()
        return text
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun skip(xmlPullParser: XmlPullParser) {
        check(xmlPullParser.eventType == 2)
        var i = 1
        while (i != 0) {
            val next = xmlPullParser.next()
            if (next == 2) {
                i++
            } else if (next == 3) {
                i--
            }
        }
    }
}