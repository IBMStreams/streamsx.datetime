/*
# Licensed Materials - Property of IBM
# Copyright IBM Corp. 2015  
# US Government Users Restricted Rights - Use, duplication or
# disclosure restricted by GSA ADP Schedule Contract with
# IBM Corp.
*/

package com.ibm.streamsx.datetime.convert;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumMap;
import java.util.TimeZone;

import com.ibm.streams.function.model.Function;

/**
 * Conversion of ISO8601 time values.
 *
 */
public class ISO8601 {

    public enum Fmt {

        XXX(FULL_ISO), XX(FULL_ISO.substring(0, FULL_ISO.length() - 1)), X(
                FULL_ISO.substring(0, FULL_ISO.length() - 2));

        private final String fmt;

        Fmt(String fmt) {
            this.fmt = fmt;
        }

        String getIso() {
            return fmt;
        }
    }

    public static final String FULL_ISO = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    // Use a thread local since SimpleDateFormat is not thread safe
    private static final ThreadLocal<EnumMap<Fmt, SimpleDateFormat>> convertors = new ThreadLocal<>();

    @Function(description = "Convert a full ISO 8601 date to number of milliseconds since the epoch. A full ISO 8061 date looks like `2015-11-24T22:54:19.427Z`, "
            + "`2015-11-24T14:54:19.427-08:00`, `2015-11-24T14:54:19.427-0800` or `2015-11-24T14:54:19.427-08`. "
            + "Returns a value that can be used with the type [TimeMillis]. "
            + "The argument is trimmed of leading and trailing whitespace, if the result is an empty string then 0 is returned.")
    public static long fromIso8601ToMillis(String date) throws ParseException {

        date = date.trim();
        if (date.isEmpty())
            return 0;

        SimpleDateFormat convertor = getIso8601Convertor(date);

        return convertor.parse(date).getTime();
    }

    @Function(description = "Convert a [TimeMillis] value to an full ISO 8061 date using UTC as the time zone.")
    public static String toIso8601(long millis) {
        SimpleDateFormat convertor = getIso8601Convertor(Fmt.XX);

        convertor.setTimeZone(UTC);
        return convertor.format(new Date(millis));
    }

    private static SimpleDateFormat getIso8601Convertor(String date) {

        Fmt fmt = Fmt.XX;
        if (date.length() > 3 && date.charAt(date.length() - 1) != 'Z') {

            char tc = date.charAt(date.length() - 3);
            // XXX is timezone with a colon. (r.g. ending with -08:00)
            // X is two digit timezone, e.g. -09
            if (tc == ':')
                fmt = Fmt.XXX;
            else if (tc == '+' || tc == '-')
                fmt = Fmt.X;
        }

        return getIso8601Convertor(fmt);
    }

    private static SimpleDateFormat getIso8601Convertor(Fmt fmt) {
        EnumMap<Fmt, SimpleDateFormat> map = convertors.get();
        if (map == null)
            map = new EnumMap<>(Fmt.class);

        SimpleDateFormat convertor = map.get(fmt);

        if (convertor == null) {
            map.put(fmt, convertor = new SimpleDateFormat(fmt.getIso()));
        }

        return convertor;
    }
}
