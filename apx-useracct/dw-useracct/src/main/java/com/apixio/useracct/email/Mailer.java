package com.apixio.useracct.email;

import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;

/**
 * Simple class to manage sending emails out.
 */
public class Mailer
{
    /**
     */
    private String         defaultSenderAddr;
    private JavaMailSender mailSender;

    private final static String DEFAULT_SUBJECT = "(no subject)";

    /**
     * Class for clients to collect/specify envelope-like information
     */
    public static class Envelope
    {
        public String               subject;
        public String               senderAddress;
        public List<String>         toRecipients;
        public List<String>         ccRecipients;
        public List<String>         bccRecipients;

        public Envelope setSubject(
            String  subject
            )
        {
            this.subject = subject;

            return this;
        }

        public Envelope addTo(
            String  to
            )
        {
            if (toRecipients == null)
                toRecipients = new ArrayList<String>();

            toRecipients.add(to);

            return this;
        }
    }

    /**
     * Spring setters
     */
    public void setDefaultSenderAddress(String defSender)
    {
        this.defaultSenderAddr = defSender;
    }
    public void setMailSender(JavaMailSender mailSender)
    {
        this.mailSender = mailSender;
    }

    /**
     * Simple method to send a plain-text email to the recipients given in the
     * envelope.
     */
    public void sendEmail(
        final Envelope    envelope,
        final String      textBody,
        final String      htmlBody
        )
    {
        MimeMessagePreparator preparator;

        if (textBody == null)
            throw new IllegalArgumentException("Sending email requires a non-empty text/plain body");

        preparator = new MimeMessagePreparator() {
                public void prepare(MimeMessage mimeMessage) throws Exception
                {
                    MimeMessageHelper  mmh     = new MimeMessageHelper(mimeMessage, true);
                    String             from    = calcSenderAddress(envelope);
                    String             subject = calcSubject(envelope);
                    String             plain   = textBody;

                    addRecipients(mmh, envelope);
                    mmh.setFrom(from);
                    mmh.setSubject(subject);

                    if (htmlBody != null)
                        mmh.setText(plain, htmlBody);
                    else
                        mmh.setText(plain);
                }
            };

        try
        {
            mailSender.send(preparator);
        }
        catch (MailException ex)
        {
            ex.printStackTrace();
        }
    }

    /**
     * Figure out and return the sender's address
     */
    private String calcSenderAddress(Envelope env)
    {
        if (env.senderAddress != null)
            return env.senderAddress;
        else
            return defaultSenderAddr;
    }

    /**
     * Figure out and return the subject.
     */
    private String calcSubject(Envelope env)
    {
        if (env.subject == null)
            return DEFAULT_SUBJECT;
        else
            return env.subject;
    }

    /**
     * Adds the recipients in the envelope to the MimeMessageHelper object
     */
    private void addRecipients(MimeMessageHelper mmh, Envelope env) throws MessagingException
    {
        if (env.toRecipients != null)
        {
            for (String recip : env.toRecipients)
                mmh.addTo(recip);
        }

        if (env.ccRecipients != null)
        {
            for (String recip : env.ccRecipients)
                mmh.addCc(recip);
        }

        if (env.bccRecipients != null)
        {
            for (String recip : env.ccRecipients)
                mmh.addBcc(recip);
        }
    }

}
