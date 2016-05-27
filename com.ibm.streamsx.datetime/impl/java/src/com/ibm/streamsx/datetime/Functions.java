package com.ibm.streamsx.datetime;

import com.ibm.streams.function.model.Function;

/**
 * Collection of Java SPL functions.
 *
 */
public class Functions {
	
	@Function(stateful=true,
			description="Return the number of milliseconds since the epoch (1970-01-01 00:00:00.000). Returns a value that can be used with the type [TimeMillis].")
	public static long currentTimeMillis() {
		return System.currentTimeMillis();
	}
}
