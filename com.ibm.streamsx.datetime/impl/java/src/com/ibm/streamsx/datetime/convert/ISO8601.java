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
import java.util.TimeZone;

import com.ibm.streams.function.model.Function;

/**
 * Conversion of ISO8601 time values.
 *
 */
public class ISO8601 {
	
	public static final String FULL_ISO = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
	private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
	
	// Use a thread local since SimpleDateFormat is not thread safe
	private static final ThreadLocal<SimpleDateFormat> convertors = new ThreadLocal<>();
	
	@Function(
			description="Convert a full ISO 8601 date to number of milliseconds since the epoch. A full ISO 8061 date looks like `2015-11-24T14:54:19.427+00:00`. Returns a value that can be used with the type [TimeMillis]. "
					+ "The argument is trimmed of leading and trailing whitespace, if the result is an empty string then 0 is returned.")
	public static long fromIso8601ToMillis(String date) throws ParseException {
		SimpleDateFormat convertor = getIso8601Convertor();
		
		date = date.trim();
		if (date.isEmpty())
			return 0;
		
		return convertor.parse(date).getTime();
	}
	
	@Function(
			description="Convert a [TimeMillis] value to an full ISO 8061 date using UTC as the time zone.")
	public static String toIso8601(long millis) {
		SimpleDateFormat convertor = getIso8601Convertor();
		
		convertor.setTimeZone(UTC);
		return convertor.format(new Date(millis));
	}

	private static SimpleDateFormat getIso8601Convertor() {
		SimpleDateFormat convertor = convertors.get();
		if (convertor == null)
			convertors.set(convertor = new SimpleDateFormat(FULL_ISO));
		return convertor;
	}
}
