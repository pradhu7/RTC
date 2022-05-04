package com.apixio.logger.graphite;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import com.apixio.logger.EventLogger;

public class GraphiteClient {
	private static final Logger log = Logger.getLogger(GraphiteClient.class);

	private static final int WAITTIME = 1000;
	private static final int TIMEOUT = 300000;

	private final String prefix;
	private final String server;
	private final int port;
	private Socket sock = null;
	private long starttime = 0;
	
	public GraphiteClient(String server, int port, String prefix) throws IOException {
		this.server = server;
		this.port = port;
		this.prefix = prefix;
	}

	private void openSocket(int wait) {
		if (sock != null)
			return;
		try {
			sock = new Socket(server, port); 
			sock.setSoTimeout(wait);
		} catch (UnknownHostException e) {
			log.warn("Graphite connection failure: " + e.getMessage());
			e.printStackTrace();
			sock = null;
		} catch (IOException e) {
			log.warn("Graphite connection failure: " + e.getMessage());
			e.printStackTrace();
			sock = null;
		}
	}
	
	public void post(String name, String data, Long seconds) throws Exception {
		String send = prefix + "." + name + " " + data + " " + seconds.toString() + "\n";
		write(send);
	}
	
	public void write(String data) throws Exception {
		IOException ex = null;
		while(starttime < TIMEOUT) {
			openSocket(WAITTIME);
			try {
				byte[] bytes = data.getBytes("utf-8");
				OutputStream outputStream = sock.getOutputStream();
				outputStream.write(bytes);
				outputStream.flush();
				starttime = 0;
				return;
			} catch (UnsupportedEncodingException e) {
				// can't happen
				log.error("Cannot happen! " + e.getMessage());
				e.printStackTrace(System.err);
				return;
			} catch (IOException e) {
				ex = e;
				try {
					Thread.sleep(WAITTIME);
					log.warn("Graphite connection timeout: " + ex.getMessage());
				} catch (InterruptedException e1) {
					
				}
				sock = null;
				starttime += WAITTIME;
			}
		}
		log.warn("Graphite connection timeout: " + ex.getMessage());
		throw new Exception("GraphiteClient.write: " + server, ex);
	}
	
	public void flush() {
		try {
			sock.getOutputStream().flush();
		} catch (Exception e) {
			close();
		}
	}
	
	public void close() {
		try {
			sock.getOutputStream().flush();
		} catch (Exception e) {
		}
		try {
			sock.getOutputStream().close();
		} catch (Exception e) {
		}
		try {
			sock.close();
		} catch (Exception e) {
		}
		sock = null;
	}

}
