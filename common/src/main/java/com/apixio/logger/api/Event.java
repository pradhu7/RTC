package com.apixio.logger.api;

import org.apache.log4j.Level;

public class Event extends Level {
	private static final int SYSLOG_EQUIVALENT = 5;
	private static final int EVENT_INT= 25000; // halfway from INFO to WARN, see Level class
	public static final Level EVENT = new Event(EVENT_INT, "EVENT",  SYSLOG_EQUIVALENT); 

	private Event(int level, String levelStr, int syslogEquivalent) {
		super(level, levelStr, syslogEquivalent);
	}

}
