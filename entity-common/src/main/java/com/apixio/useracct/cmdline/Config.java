package com.apixio.useracct.cmdline;

/**
 * Contains configuration extracted from the yaml config file needed to communicate
 * with the RESTful services.
 */
public class Config {

    private String  tokenizerServer;
    private String  userAcctServer;

    public String getTokenizerServer()
    {
        return tokenizerServer;
    }

    public void setTokenizerServer(String server)
    {
        this.tokenizerServer = server;
    }

    public String getUserAcctServer()
    {
        return userAcctServer;
    }

    public void setUserAcctServer(String server)
    {
        this.userAcctServer = server;
    }

    @Override
    public String toString()
    {
        return ("{config tokenizer=" + tokenizerServer + ", useracct=" + userAcctServer +
                "}");
    }
}
