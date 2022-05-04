package com.apixio.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * CommandBase is the base class for Spring-enabled command line applications.
 * It provides a small pattern for loading up beans defined in .xml files and
 * for pulling in properties defined in .properties files.
 */
public class CommandBase {

    /**
     * contexts contains the system-level defined core Spring information (beans)
     * that should be available to all command line applications.  Application-
     * specific extra beans SHOULD be specified in an application-specific .xml
     * file and that filename(s) should be passed as the "moreContexts" parameter
     * on the protected constructore.
     */
    protected final static String[]  contexts = new String[] {"spring.xml"};

    /**
     * The loaded Spring application context.  Beans can be retrieved and cached
     * from this object.
     */
    protected AbstractApplicationContext appContext;
    protected boolean goodInit;

    /**
     * Constructor that will load up Spring beans from a set of .xml bean definition
     * files.
     */
    public CommandBase(String[] moreContexts)
    {
        try
        {
            this.appContext = new ClassPathXmlApplicationContext(concatArrays(contexts, moreContexts));

            goodInit = true;
        }
        catch (Exception x)
        {
            x.printStackTrace();   // temporary logging only
        }
    }

    /**
     * Constructor that will load up Spring beans from a set of .xml bean definition
     * files.
     *
     * Note: It will only from contexts defined in children classes.
     */
    public CommandBase(String[] moreContexts, boolean ignoreBaseClassSpringXml)
    {
        try
        {
            this.appContext = new ClassPathXmlApplicationContext(moreContexts);

            goodInit = true;
        }
        catch (Exception x)
        {
            x.printStackTrace();   // temporary logging only
        }
    }

    /**
     * Constructor that will load up Spring beans from a set of .xml bean definition
     * files.
     */
    protected CommandBase(String[] moreContexts, ApplicationContext parent)
    {
        try
        {
            this.appContext = new ClassPathXmlApplicationContext(concatArrays(contexts, moreContexts), parent);

            goodInit = true;
        }
        catch (Exception x)
        {
            x.printStackTrace();   // temporary logging only
        }
    }

    /**
     * Constructor that will load up Spring beans from a set of .xml bean definition
     * files.
     *
     * Note: It will only from contexts defined in children classes.
     */
    protected CommandBase(String[] moreContexts, boolean ignoreBaseClassSpringXml, ApplicationContext parent)
    {
        try
        {
            this.appContext = new ClassPathXmlApplicationContext(moreContexts, parent);

            goodInit = true;
        }
        catch (Exception x)
        {
            x.printStackTrace();   // temporary logging only
        }
    }

    /**
     * Append two arrays to make a single array.
     */
    private static String[] concatArrays(String[] sys, String[] app)
    {
        if ((app == null) || (app.length == 0))
            return sys;

        String[] joined = new String[sys.length + app.length];

        System.arraycopy(sys, 0, joined, 0,          sys.length);
        System.arraycopy(app, 0, joined, sys.length, app.length);

        return joined;
    }

}
