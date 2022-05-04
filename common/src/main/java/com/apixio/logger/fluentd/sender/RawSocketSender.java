//
// A Structured Logger for Fluent
//
// Copyright (C) 2011 - 2013 Muga Nishizawa
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
package com.apixio.logger.fluentd.sender;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Map;

import org.msgpack.MessagePack;

public class RawSocketSender implements Sender {

    private MessagePack msgpack;

    private SocketAddress server;

    private Socket socket;

    private int timeout;

    private BufferedOutputStream out;

    private ByteBuffer pendings;

    private Reconnector reconnector;

    private String name;

    public RawSocketSender() {
        this("localhost", 24224);
    }

    public RawSocketSender(String host, int port) {
        this(host, port, 3 * 1000, 8 * 1024 * 1024);
    }

    public RawSocketSender(String host, int port, int timeout, int bufferCapacity) {
        msgpack = new MessagePack();
        msgpack.register(Event.class, Event.EventTemplate.INSTANCE);
        pendings = ByteBuffer.allocate(bufferCapacity);
        server = new InetSocketAddress(host, port);
        reconnector = new ExponentialDelayReconnector();
        name = String.format("%s_%d_%d_%d", host, port, timeout, bufferCapacity);
        this.timeout = timeout;
    }

    private void open() {
        if (! connect()) {
            System.err.println(this.getClass().getCanonicalName() + ".open host: " + server.toString() + " failed: " + new Date().toGMTString());
            close();
        }
    }

    private boolean connect() {
        try {
            socket = new Socket();
            socket.connect(server);
            socket.setSoTimeout(timeout); // the timeout value to be used in milliseconds
            out = new BufferedOutputStream(socket.getOutputStream());
            reconnector.clearErrorHistory();
            System.out.println(this.getClass().getCanonicalName() + ".connect");
            return true;
        } catch (IOException e) {
            System.err.println("connect: " + new Date().toGMTString());
            System.err.println(this.getClass().getCanonicalName() + ".connect host: " + server.toString() + " failed: " + new Date().toGMTString());
            reconnector.addErrorHistory(System.currentTimeMillis()/1000);
            e.printStackTrace(System.err);
            out = null;
            close();
            socket = null;
            return false;
        }
    }

    private boolean reconnect() throws IOException {
        if (socket == null) {
            return connect();
        } else if (socket.isClosed() || (!socket.isConnected())) {
            close();
            return connect();
        } else {
            return true;
        }
    }

    public void close() {
        // close output stream
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) { // ignore
            } finally {
                out = null;
            }
        }

        // close socket
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) { // ignore
            } finally {
                socket = null;
            }
        }
    }
    
    @Override
    public boolean isUp() {
        return socket != null;
    }

    @Override
    public boolean emit(String tag, Map<String, Object> data) throws IOException {
        return emit(tag, System.currentTimeMillis() / 1000, data);
    }

    @Override
    public boolean emit(String tag, long timestamp, Map<String, Object> data) throws IOException {
        return emit(new Event(tag, timestamp, data));
    }

    protected boolean emit(Event event) throws IOException {
        byte[] bytes = null;

        // serialize tag, timestamp and data
        bytes = msgpack.write(event);
        // send serialized data
        return send(bytes);
    }

    private synchronized boolean send(byte[] bytes) throws IOException {
        // buffering
        if (pendings.position() + bytes.length > pendings.capacity()) {
            System.out.println(this.getClass().getCanonicalName() + ".send flushing");
            reconnect();
            flush();
        }
        if (pendings.position() + bytes.length > pendings.capacity()) {
            System.err.println(this.getClass().getCanonicalName() + ".send host: " + server.toString() + " failed: " + new Date().toGMTString());
            return false;
        }
        pendings.put(bytes);

        // suppress reconnection burst
        if (!reconnector.enableReconnection(System.currentTimeMillis()/1000)) {
            return true;
        }

        // send pending data
        flush();

        return true;
    }

    public synchronized void flush() {
        try {
            // check whether connection is established or not
            if (reconnect()) {
                // write data
                out.write(getBuffer());
                out.flush();
                clearBuffer();
            }
        } catch (IOException e) {
            System.err.println(this.getClass().getCanonicalName() + ".flush host: " + server.toString() + " failed: " + new Date().toGMTString());
            e.printStackTrace(System.err);
            close();
        }
    }

    public byte[] getBuffer() {
        int len = pendings.position();
        pendings.position(0);
        byte[] ret = new byte[len];
        pendings.get(ret, 0, len);
        return ret;
    }

    private void clearBuffer() {
        pendings.clear();
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }
}
