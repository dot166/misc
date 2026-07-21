/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3.nexus.bottombar;

import java.util.Arrays;
import java.util.Locale;

/**
 * Operations to assist when working with a {@link Locale}.
 *
 * <p>
 * This class tries to handle {@code null} input gracefully. An exception will not be thrown for a {@code null} input. Each method documents its behavior in
 * more detail.
 * </p>
 *
 * @see Locale
 * copied from Apache commons-lang library, not the whole class, only the bit I need
 */
public class LocaleUtils {

    /**
     * The underscore character {@code '}{@value}{@code '}.
     */
    private static final char UNDERSCORE = '_';

    /**
     * The dash character {@code '}{@value}{@code '}.
     */
    private static final char DASH = '-';

    /**
     * Tests whether the given string is the length of an <a href="https://www.iso.org/iso-3166-country-codes.html">ISO 3166</a> alpha-2 country code.
     *
     * @param str The string to test.
     * @return whether the given string is the length of an <a href="https://www.iso.org/iso-3166-country-codes.html">ISO 3166</a> alpha-2 country code.
     */
    private static boolean isAlpha2Len(final String str) {
        return str.length() == 2;
    }

    /**
     * Tests whether the given string is the length of an <a href="https://www.iso.org/iso-3166-country-codes.html">ISO 3166</a> alpha-3 country code.
     *
     * @param str The string to test.
     * @return whether the given string is the length of an <a href="https://www.iso.org/iso-3166-country-codes.html">ISO 3166</a> alpha-3 country code.
     */
    private static boolean isAlpha3Len(final String str) {
        return str.length() == 3;
    }

    /**
     * Tests whether the given String is a <a href="https://www.iso.org/iso-3166-country-codes.html">ISO 3166</a> alpha-2 country code.
     *
     * @param str The String to check.
     * @return true, is the given String is a <a href="https://www.iso.org/iso-3166-country-codes.html">ISO 3166</a> compliant country code.
     */
    private static boolean isISO3166CountryCode(final String str) {
        return isAllUpperCase(str) && isAlpha2Len(str);
    }

    /**
     * Tests whether the given String is a <a href="https://www.iso.org/iso-639-language-code">ISO 639</a> compliant language code.
     *
     * @param str The String to check.
     * @return true, if the given String is a <a href="https://www.iso.org/iso-639-language-code">ISO 639</a> compliant language code.
     */
    private static boolean isISO639LanguageCode(final String str) {
        return isAllLowerCase(str) && (isAlpha2Len(str) || isAlpha3Len(str));
    }

    /**
     * TestsNo whether the given String is a UN M.49 numeric area code.
     *
     * @param str The String to check.
     * @return true, is the given String is a UN M.49 numeric area code.
     */
    private static boolean isNumericAreaCode(final String str) {
        return isNumeric(str) && isAlpha3Len(str);
    }

    /**
     * Tries to parse a Locale from the given String.
     * <p>
     * See {@link Locale} for the format.
     * </p>
     *
     * @param str The String to parse as a Locale.
     * @return A Locale parsed from the given String.
     * @throws IllegalArgumentException if the given String cannot be parsed.
     * @see Locale
     * @see <a href="https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Locale.html#special_cases_constructor">Locale special cases</a>
     */
    private static Locale parseLocale(final String str) {
        if (isISO639LanguageCode(str)) {
            return new Locale(str);
        }
        final int limit = 3;
        final char separator = str.indexOf(UNDERSCORE) != -1 ? UNDERSCORE : DASH;
        final String[] segments = str.split(String.valueOf(separator), 3);
        final String language = segments[0];
        if (segments.length == 2) {
            final String country = segments[1];
            if (isISO639LanguageCode(language) && (isISO3166CountryCode(country) || isNumericAreaCode(country))) {
                return new Locale(language, country);
            }
        } else if (segments.length == limit) {
            final String country = segments[1];
            final String variant = segments[2];
            // Special case 1: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Locale.html#special_cases_constructor
            if (str.equals("th_TH_TH_#u-nu-thai")) {
                return new Locale(language, country, "TH");
            }
            // Special case 2: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Locale.html#special_cases_constructor
            if (str.equals("ja_JP_JP_#u-ca-japanese")) {
                return new Locale(language, country, "JP");
            }
            if (isISO639LanguageCode(language) && (country.isEmpty() || isISO3166CountryCode(country) || isNumericAreaCode(country)) && !variant.isEmpty()) {
                return new Locale(language, country, variant);
            }
        }
        if (Arrays.asList(Locale.getISOCountries()).contains(str)) {
            return new Locale("", str);
        }
        throw new IllegalArgumentException("Invalid locale format: " + str);
    }

    /**
     * Converts a String to a Locale.
     *
     * <p>
     * This method takes the string format of a locale and creates the locale object from it.
     * </p>
     *
     * <pre>
     *   LocaleUtils.toLocale("")           = new Locale("", "")
     *   LocaleUtils.toLocale("en")         = new Locale("en", "")
     *   LocaleUtils.toLocale("en_GB")      = new Locale("en", "GB")
     *   LocaleUtils.toLocale("en-GB")      = new Locale("en", "GB")
     *   LocaleUtils.toLocale("en_001")     = new Locale("en", "001")
     *   LocaleUtils.toLocale("en_GB_xxx")  = new Locale("en", "GB", "xxx")   (#)
     *   LocaleUtils.toLocale("US")         = new Locale("", "US") // Because "US" is Locale.getISOCountries()
     * </pre>
     *
     * <p>
     * (#) The behavior of the JDK variant constructor changed between JDK1.3 and JDK1.4. In JDK1.3, the constructor upper cases the variant, in JDK1.4, it
     * doesn't. Thus, the result from getVariant() may vary depending on your JDK.
     * </p>
     *
     * <p>
     * This method validates the input strictly. The language code must be lowercase. The country code must be uppercase. The separator must be an underscore or
     * a dash. The length must be correct.
     * </p>
     *
     * @param str The locale String to convert, null returns null.
     * @return A Locale, null if null input.
     * @throws IllegalArgumentException if the string is an invalid format.
     * @see Locale#forLanguageTag(String)
     * @see Locale#getISOCountries()
     * @see <a href="https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Locale.html#special_cases_constructor">Locale special cases</a>
     */
    public static Locale toLocale(final String str) {
        if (str == null) {
            return new Locale("", "");
        }
        if (str.isEmpty()) { // LANG-941 - JDK 8 introduced an empty locale where all fields are blank
            return new Locale("", "");
        }
        final int len = str.length();
        if (len < 2) {
            throw new IllegalArgumentException("Invalid locale format: " + str);
        }
        final char ch0 = str.charAt(0);
        if (ch0 == UNDERSCORE || ch0 == DASH) {
            if (len < 3) {
                throw new IllegalArgumentException("Invalid locale format: " + str);
            }
            final char ch1 = str.charAt(1);
            final char ch2 = str.charAt(2);
            if (!Character.isUpperCase(ch1) || !Character.isUpperCase(ch2)) {
                throw new IllegalArgumentException("Invalid locale format: " + str);
            }
            if (len == 3) {
                return new Locale("", str.substring(1, 3));
            }
            if (len < 5) {
                throw new IllegalArgumentException("Invalid locale format: " + str);
            }
            if (str.charAt(3) != ch0) {
                throw new IllegalArgumentException("Invalid locale format: " + str);
            }
            return new Locale("", str.substring(1, 3), str.substring(4));
        }
        return parseLocale(str);
    }

    public static boolean isAllLowerCase(final CharSequence cs) {
        if (isEmpty(cs)) {
            return false;
        }
        final int sz = cs.length();
        for (int i = 0; i < sz;) {
            final int codePoint = Character.codePointAt(cs, i);
            if (!Character.isLowerCase(codePoint)) {
                return false;
            }
            i += Character.charCount(codePoint);
        }
        return true;
    }

    public static boolean isAllUpperCase(final CharSequence cs) {
        if (isEmpty(cs)) {
            return false;
        }
        final int sz = cs.length();
        for (int i = 0; i < sz;) {
            final int codePoint = Character.codePointAt(cs, i);
            if (!Character.isUpperCase(codePoint)) {
                return false;
            }
            i += Character.charCount(codePoint);
        }
        return true;
    }

    public static boolean isEmpty(final CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    public static boolean isNumeric(final CharSequence cs) {
        if (isEmpty(cs)) {
            return false;
        }
        final int sz = cs.length();
        for (int i = 0; i < sz;) {
            final int codePoint = Character.codePointAt(cs, i);
            if (!Character.isDigit(codePoint)) {
                return false;
            }
            i += Character.charCount(codePoint);
        }
        return true;
    }
}
