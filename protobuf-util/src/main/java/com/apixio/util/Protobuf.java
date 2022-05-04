package com.apixio.util;

import java.net.InetAddress;
import java.util.UUID;
import java.time.Instant;
import com.google.protobuf.Timestamp;
import com.apixio.XUUID;
import com.apixio.messages.MessageMetadata;
import com.apixio.messages.MessageMetadata.MessageType;
import com.apixio.messages.MessageMetadata.MessageHeader;
import com.apixio.messages.MessageMetadata.SenderID;
import com.apixio.logs.Common;
import com.apixio.logs.Common.LogHeader;
import com.apixio.logs.Common.LogLevel;
import com.apixio.logs.Common.LogType;

public class Protobuf {
    static String   hostFQDN;
    static SenderID senderID = null; // this must be initialized with setSenderID before first MakeMessageHeader call
    static
    {
        // get our hostFQDN
        byte[] ipAddress = new byte[]{(byte) 127, (byte) 0, (byte) 0, (byte) 1};
        try {
            InetAddress address = InetAddress.getByAddress(ipAddress);
            hostFQDN = address.getCanonicalHostName();
        } catch (Exception e) {
            // TODO: jos log initialization ERROR
        }
   }

    /**
     * convert MessageMetadata.XUUID to java String
     */
    public static String ProtoXUUIDtoString(MessageMetadata.XUUID id) {
        return String.format("%s_%s", id.getType(), id.getUuid());
    }

    public static MessageMetadata.XUUID stringToProtoXUUID(String id) {
        String[] parts = id.split("_");
        return MessageMetadata.XUUID.newBuilder()
                .setType(parts[0])
                .setUuid(parts[1])
                .build();
    }
    /**
     * take an Apixio protobuf XUUID (see MessageMetadata.proto) and emit a
     * com.apixio.XUUID
     */
    public static XUUID XUUIDfromProtoXUUID(MessageMetadata.XUUID xuuid) {
        try {
            return XUUID.create(
                    xuuid.getType(),
                    UUID.fromString(xuuid.getUuid()),
                    false,
                    true);
        } catch (Exception e) {
            // TODO: jos, is this a loggable error?
            return null;
        }
    }

    public static MessageMetadata.XUUID XUUIDtoProtoXUUID(XUUID xuuid) {
        return MessageMetadata.XUUID.newBuilder()
                .setUuid(xuuid.getUUID().toString())
                .setType(xuuid.getType())
                .build();
    }

    /*
       TimestampToMillies, a convenience function useful if using two timestamps to
       time something that
     */
    public static long TimestampToMillies(Timestamp ts) {
        return (ts.getSeconds() * 1000) + (ts.getNanos()/1000000);
    }

    public static MessageMetadata.MessageHeader makeMessageHeader(MessageType type) {
        // See README.MD in this package source directory for documentation on
        // how to initialize your service properly to set the senderId via the
        // setSenderID() call below
        if (senderID == null) {
            throw new ExceptionInInitializerError("com.apixio.util.Protobuf requires that the senderID has been set in the service bootstrap. See com.apixio.util.Protobuf.makeMessageHeader() for details");
        } else {
            return MessageHeader.newBuilder()
                    .setHostFqdn(hostFQDN)
                    // TODO: jos, replace hard coded type with type from an enum
                    .setMessageID(createMessageID("MSG"))
                    .setMessageType(type)
                    .setSenderID(senderID)
                    .setMessageDateTime(makeTimestamp())
                    .build();
        }
    }

    public static Common.LogHeader makeLogHeader(
            LogType         type,
            LogLevel        level,
            String          loggerName) {
        if (senderID == null) {
            throw new ExceptionInInitializerError("com.apixio.util.Protobuf requires that the senderID has been set in the service bootstrap. See com.apixio.util.Protobuf.makeMessageHeader() for details");
        } else {
            Timestamp ts = makeTimestamp();
            return LogHeader.newBuilder()
                    .setHostFqdn(hostFQDN)
                    .setLogID(createLogID())
                    .setLogType(type)
                    .setLogLevel(level)
                    .setLoggerID(Common.LoggerID.newBuilder().setLoggerName(loggerName).setComponentName(senderID.getName()))
                    .setDateTime(ts)
                    .setTime(TimestampToMillies(ts))
                    .build();
        }
    }

    public static void setSenderID(String name, String version) {
        // back door incase we cannot get from environment
        senderID = createSenderID(name, version);
    }

    private static SenderID createSenderID(String name, String version) {
        return MessageMetadata.SenderID.newBuilder()
                .setName(name)
                .setVersion(version)
                .build();
    }

    private static MessageMetadata.XUUID createMessageID(String type) {
        return MessageMetadata.XUUID.newBuilder()
                .setType(type)
                .setUuid(UUID.randomUUID().toString())
                .build();
    }

    private static MessageMetadata.XUUID createLogID() {
        return MessageMetadata.XUUID.newBuilder()
                .setType("log")
                .setUuid(UUID.randomUUID().toString())
                .build();
    }

    private static Timestamp makeTimestamp() {
        Instant time = Instant.now();
        return Timestamp.newBuilder()
                .setSeconds(time.getEpochSecond())
                .setNanos(time.getNano())
                .build();
    }
}

