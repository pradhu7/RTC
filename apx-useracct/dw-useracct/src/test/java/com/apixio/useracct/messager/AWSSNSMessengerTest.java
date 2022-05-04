package com.apixio.useracct.messager;

import org.junit.Ignore;
import org.junit.Test;

public class AWSSNSMessengerTest
{

    @Test
    @Ignore
    public void sendMessageForMessengerWithInvalidCred()
    {
        AWSSNSMessenger awssnsMessenger = new AWSSNSMessenger("jkasgdkjash", "ashjdgjhasgjh", AWSSNSMessenger.US_WEST_2, "Promotional");

        assert(awssnsMessenger.sendMessage("hi AJ this is jeswin, a Promotional message from " + AWSSNSMessenger.US_WEST_2, "+13522139925", AWSSNSMessenger.PROMOTIONAL));

        assert(awssnsMessenger.sendMessage("hi AJ this is jeswin, a transactional message from " + AWSSNSMessenger.US_WEST_2, "+13522139925", AWSSNSMessenger.PROMOTIONAL));

        assert(awssnsMessenger.sendMessage("hi AJ this is jeswin, a Promotional message from " + AWSSNSMessenger.US_WEST_2, "+13522139925", null));
    }
}