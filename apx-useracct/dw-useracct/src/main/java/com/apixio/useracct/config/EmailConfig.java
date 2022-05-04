package com.apixio.useracct.config;

import java.util.Properties;

/**
 * Contains the yaml-poked email-related configuration:
 *
 *  * defaultSender is the email address that the outgoing messages will be sent
 *     from.
 *
 *  * host, port, username, password are the SMTP server configuration points used to
 *     login and send an email.
 *
 *  * javaMailProperties is a Properties object that is passed to the Java mail API and
 *     typically contains properties to enable TLS.
 *
 *  * templateDir is the absolute path of the directory that contains email templates.
 *     Given the dropwizard and maven build, this should be simply rooted like "/templates"
 *
 *  * imageBase is the full "http://host:port/blah/blah/blah" prefix that will be prepended
 *     to the images in the html template of the emails.
 */
public class EmailConfig {
    
    private String     defaultSender;     // email address; e.g, "noreply@host.com"
    private int        port;              // of SMTP server
    private String     host;              // of SMTP server
    private String     username;          // of SMTP account
    private String     password;          // for SMTP account
    private Properties javaMailProperties;  // allows TLS config

    private String     templateDir;       // for email templates
    private String     imageBase;         // for images in email templates

    public void setDefaultSender(String defaultSender)
    {
        this.defaultSender = defaultSender;
    }
    public String getDefaultSender()
    {
        return defaultSender;
    }

    public void setSmtpPort(int port)
    {
        this.port = port;
    }
    public int getSmtpPort()
    {
        return port;
    }

    public void setSmtpHost(String host)
    {
        this.host = host;
    }
    public String getSmtpHost()
    {
        return host;
    }

    public void setSmtpUsername(String username)
    {
        this.username = username;
    }
    public String getSmtpUsername()
    {
        return username;
    }

    public void setSmtpPassword(String password)
    {
        this.password = password;
    }
    public String getSmtpPassword()
    {
        return password;
    }

    public void setTemplates(String templateDir)
    {
        this.templateDir = templateDir;
    }
    public String getTemplates()
    {
        return templateDir;
    }

    public void setJavaMailProperties(Properties props)
    {
        this.javaMailProperties = props;
    }
    public Properties getJavaMailProperties()
    {
        return javaMailProperties;
    }

    public void setImageBase(String imageBase)
    {
        this.imageBase=imageBase;
    }
    public String getImageBase()
    {
        return imageBase;
    }

}
