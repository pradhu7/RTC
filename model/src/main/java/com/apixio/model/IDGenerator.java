package com.apixio.model;

import java.net.InetAddress;

/**
 * A simple singleton class that generates ids, that are guaranteed to be
 * unique within a session, but are also probabilistically unique across
 * sessions so that we have a low collision rate with them.
 * 
 * @author vvyas
 *
 */
public class IDGenerator {
	private int sessionCounter = 0;
	private final long sessionInitial;
	private static final IDGenerator INSTANCE = new IDGenerator();
	
	private IDGenerator() {
		byte offset1 = 42; 	// set a default offset of 42, if hostname resolution fails, why, because 42 is the answer to life, universe and everything.
		byte offset2 = 19;  // set a default offset2 of 19, because, Imran thinks its the answer to life, universe and everything.
		
		try {
			byte [] inetAddr = InetAddress.getLocalHost().getAddress();
			offset1 = inetAddr[3];
			offset2 = inetAddr[2];			
		} catch(Exception e) {
			// we already have the default offset set, so we can
			// do nothing here.
		}				
		sessionInitial = (((System.currentTimeMillis()/1000) & 0x0000FFFF) << 48) | (((long)offset1 << 40) | ((long)offset2 << 32));
	}
	
	public synchronized long nextId() {
		sessionCounter +=  1;
		return (sessionInitial + sessionCounter);
	}
	
	public static IDGenerator getInstance() { return INSTANCE; }
}
