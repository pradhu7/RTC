package com.apixio.restbase;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;

import com.apixio.SysServices;
import com.apixio.logger.LogHelper;
import com.apixio.restbase.apiacl.ApiAcls.LogLevel;
import com.apixio.restbase.apiacl.ApiAcls;
import com.apixio.restbase.apiacl.dwutil.ApiReaderInterceptor;
import com.apixio.restbase.config.ConfigSet;
import com.apixio.restbase.config.ConfigUtil;
import com.apixio.restbase.config.MicroserviceConfig;
import com.apixio.restbase.web.AclChecker;
import com.apixio.restbase.web.AclContainerFilter;
import com.apixio.restbase.web.Dispatcher;
import com.apixio.restbase.web.Microfilter;

public abstract class MicroserviceApplication<C extends MicroserviceConfig> extends Application<C>
{
    public static interface SysServicesFactory<S extends DataServices, C>
    {
        public S createSysServices(DaoBase seed, C configuration) throws Exception;
    }

    protected <S extends DataServices> S createSysServices(
        C                        configuration,
        SysServicesFactory<S, C> factory
        ) throws Exception
    {
        PersistenceServices ps        = ConfigUtil.createPersistenceServices(configuration);
        DaoBase             daoBase;

        // set up services, especially DataAccessObjects.  Add to this list as needed.
        // BaseServices-derived class!

        daoBase = new DaoBase(ps);

        return factory.createSysServices(daoBase, configuration);
    }

    /**
     * Set up API ACL checking by initializing that subsystem and modifying the
     * AclChecker filter (if it's been installed) and registering the ReaderInterceptor
     * with the JAX-RS container (only dropwizard 0.8.1+ supports this).
     */
    protected void setupApiAcls(Environment environment, SysServices sysServices, List<Microfilter> filters, MicroserviceConfig config) throws FileNotFoundException, IOException
    {
        // When the temporary condition described in ConfigUtil.setupApiAcls
        // is fixed, then that code should be moved back here
        ConfigUtil.setupApiAcls(environment, sysServices, filters, config);
        /*
        ConfigSet aclConfig = config.getConfigSubtree(MicroserviceConfig.CSET_APIACL);

        if (aclConfig != null)
        {
            String jsonFile     = aclConfig.getString(MicroserviceConfig.ACL_DEFFILE);
            Long   cacheTimeout = aclConfig.getLong(MicroserviceConfig.ACL_CACHETIMEOUT);

            if (cacheTimeout != null)
                sysServices.getAclLogic().setHasPermissionCacheTimeout(cacheTimeout.longValue());

            if (jsonFile != null)
            {
                ApiAcls                acls        = ApiAcls.fromJsonFile(sysServices, jsonFile);
                ApiReaderInterceptor   interceptor = new ApiReaderInterceptor(acls.getPermissionEnforcer());
                String                 aclDebug    = aclConfig.getString(MicroserviceConfig.ACL_DEBUG);

                if (aclDebug != null)
                {
                    ConfigSet lc = config.getConfigSubtree(MicroserviceConfig.CSET_LOGGING);

                    if (lc != null)
                    {
                        acls.setLogLevel(LogLevel.valueOf(aclDebug.toUpperCase()),
                                         LogHelper.getEventLogger(toProperties(lc.getConfigSubtree(MicroserviceConfig.LOG_PROPERTIES).getProperties()),
                                                                  lc.getString(MicroserviceConfig.LOG_APPNAME),
                                                                  lc.getString(MicroserviceConfig.LOG_LOGGERNAME)));
                    }
                }

                environment.jersey().register(interceptor);
                environment.jersey().register(new AclContainerFilter());  // because messing with ResponseWrappers isn't sufficient...

                for (Microfilter mf : filters)
                {
                    if (mf instanceof AclChecker)
                        ((AclChecker) mf).setApiAcls(acls);
                }
            }
        }
        */
    }

    protected <S extends DataServices> List<Microfilter> setupFilters(C configuration, Environment environment, S sysServices) throws Exception
    {
        ConfigSet                  filterConfigs = configuration.getMicrofilterConfig();
        List<Microfilter>          microfilters  = new ArrayList<Microfilter>();
        FilterRegistration.Dynamic reg;
        Dispatcher                 filter;

        for (Object filterClass : filterConfigs.getList(MicroserviceConfig.MF_REQUESTFILTERS))
            microfilters.add(makeMicrofilter(filterConfigs, ((String) filterClass), sysServices));

        filter = new Dispatcher();
        filter.setMicrofilters(microfilters);

        reg = environment.servlets().addFilter("useracct", filter);

        reg.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false,
                                     listToArray(filterConfigs.getList(MicroserviceConfig.MF_URLPATTERN)));

        return microfilters;
    }

    private static String[] listToArray(List<Object> list)
    {
        String[] ary = new String[list.size()];

        for (int i = 0; i < ary.length; i++)
            ary[i] = (String) list.get(i);   // we want ClassCastException if config is wrong

        return ary;
    }

    private static <S extends DataServices> Microfilter makeMicrofilter(ConfigSet config, String filterClass, S sysServices) throws Exception
    {
        Class<Microfilter> c  = (Class<Microfilter>) Class.forName(filterClass);
        Microfilter        mf = c.newInstance();
        ConfigSet          props;

        props = config.getConfigSubtree(filterClass);

        mf.configure(((props != null) ? props.getProperties() : null), sysServices);

        return mf;
    }

}
