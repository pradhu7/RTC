package com.apixio.useracct.messager;

public abstract class Messenger
{
    private String messengertype;

    public abstract boolean sendMessage(String message, String phoneNumber, String perMessageType);

    public String getMessengertype() {
        return messengertype;
    }

    public void setMessengertype(String messengertype) {
        this.messengertype = messengertype;
    }
}
