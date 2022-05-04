package com.apixio.sdk.logging;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apixio.logger.EventLogger;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.restbase.util.DateUtil;
import com.apixio.sdk.FxEnvironment;
import com.apixio.sdk.FxLogger;
import com.apixio.sdk.FxRequest;
import com.apixio.sdk.metric.Metric;
import com.apixio.sdk.util.ExceptionUtil;

/**
 * Cheap FileLogger logs all FxLogger calls to the configured file.  Fields are tab separated and consist
 * of:
 *
 *  * datetime; ISO8601
 *  * thread
 *  * level; DEBUG, INFO, WARN, ERROR
 *  * component
 *  * context; can be empty
 *  * message
 *  * stacktrace; optional
 */
public class FileLogger extends BaseLogger
{

    /**
     *
     */
    private File    filepath;
    private boolean debugEnabled;
    private boolean infoEnabled;
    private boolean warnEnabled;
    private boolean errorEnabled;
    private Writer  writer;

    /**
     * Configure file logger
     */
    public void configure(ConfigSet config) throws IOException
    {
        this.filepath = new File(config.getString("filepath"));
        this.writer   = new FileWriter(this.filepath);

        this.debugEnabled = config.getBoolean("debug", Boolean.FALSE);
        this.infoEnabled  = config.getBoolean("info",  Boolean.TRUE);
        this.warnEnabled  = config.getBoolean("warn",  Boolean.TRUE);
        this.errorEnabled = config.getBoolean("error", Boolean.TRUE);
    }

    /**
     * Standard logging levels as methods
     */
    @Override
    public void info(String format, Object... args)
    {
        if (infoEnabled)
            logToFile("INFO", format, args, null);
    }

    @Override
    public void warn(String format, Object... args)
    {
        if (warnEnabled)
            logToFile("WARN", format, args, null);
    }

    @Override
    public void debug(String format, Object... args)
    {
        if (debugEnabled)
            logToFile("DEBUG", format, args, null);
    }

    /**
     * "Error" level logging; if throwable form is called then stack trace is
     * included automatically 
     */
    @Override
    public void error(String format, Object... args)
    {
        if (errorEnabled)
            logToFile("ERROR", format, args, null);
    }

    @Override
    public void error(String format, Throwable t, Object... args)
    {
        if (errorEnabled)
            logToFile("ERROR", format, args, ExceptionUtil.oneLineStackTrace(t));
    }

    /**
     * This is mostly a pass-through to EventLogger.event().  It adds context information...
     * TODO: finish this...
     */
    @Override
    public void event(Map<String,Object> packet)
    {
        //!! temporary??
        info("event(%s)", packet);
    }

    /**
     *
     */
    private void logToFile(String level, String fmt, Object[] args, String trace)
    {
        StringBuilder sb = new StringBuilder();
        String        v;

        // date
        sb.append(DateUtil.dateToIso8601(new Date()));
        sb.append('\t');

        // thread
        sb.append(Thread.currentThread().getName());
        sb.append('\t');

        // level
        sb.append(level);
        sb.append('\t');

        // component
        if ((v = getCurrentComponentName()) != null)
            sb.append(v);
        sb.append('\t');

        // context
        if ((v = getContext()) != null)
            sb.append(v);
        sb.append('\t');

        // message
        sb.append(String.format(fmt, args));
        sb.append('\t');

        // trace
        if (trace != null)
        {
            sb.append(trace);
            sb.append('\t');
        }

        try
        {
            writer.write(sb.toString());
            writer.write('\n');
            writer.flush();
        }
        catch (IOException x)
        {
            System.err.println("(Failed to write to log file " + filepath + ":  " + sb.toString());
        }
    }

}
