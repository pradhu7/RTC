package com.apixio.validation;

/**
 * Treat 'message' and 'messageType' properties as immutable if you want to use this class with Java Collections Framework (e.g. HashMaps).
 */
public class Message {

    private String message;
    private MessageType messageType;

    public Message(String message, MessageType messageType) {
        this.message = message;
        this.messageType = messageType;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public MessageType getMessageType() {
        return messageType;
    }
    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    @Override
    public String toString() {
        return messageType + ": " + message;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != getClass()) return false;

        Message that = (Message) obj;
        return message.equals(that.message) && messageType == that.messageType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + message.hashCode();
        result = prime * result + messageType.hashCode();
        return result;
    }
}
