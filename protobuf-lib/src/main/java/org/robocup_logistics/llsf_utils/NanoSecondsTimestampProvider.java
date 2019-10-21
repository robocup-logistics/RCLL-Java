package org.robocup_logistics.llsf_utils;

/**
 * The Class NanoSecondsTimestampProvider provides a timestamp in nanoseconds.
 */
public class NanoSecondsTimestampProvider {

	private long nanoSecondsOffset;

	/**
	 * Instantiates a new NanoSecondsTimestampProvider.
	 */
	public NanoSecondsTimestampProvider() {
		long curMilliSecs0, curMilliSecs1, curNanoSecs;
		
		do {
			curMilliSecs0 = System.currentTimeMillis();
			curNanoSecs = System.nanoTime();
			curMilliSecs1 = System.currentTimeMillis();
		} while (curMilliSecs0 == curMilliSecs1);

		nanoSecondsOffset = 1000000L * curMilliSecs1 - curNanoSecs;
	}

	/**
	 * Get the current time (UTC) in nanoseconds.
	 * 
	 * @return the time in nanoseconds since midnight, January 1, 1970 UTC
	 */
	public long currentNanoSecondsTimestamp() {
		return System.nanoTime() + nanoSecondsOffset;
	}
}