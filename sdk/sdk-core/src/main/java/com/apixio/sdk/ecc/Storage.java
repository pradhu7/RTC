package com.apixio.sdk.ecc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.apixio.dao.apxdata.ApxDataDao;
import com.apixio.dao.apxdata.DataType;
import com.apixio.sdk.Converter;
import com.apixio.sdk.FxExecEnvironment;
import com.apixio.sdk.FxLogger;

/**
 * Storage class manages the conversion between the interface-based representation of data to the
 * persistable protobuf-based representation of it.
 *
 * In order for data returned by some f(x) or consumed by some g(y) (where 'y' is the output of some
 * other f(x)) the type of that data must be registered in this class.  Required registration info
 * includes all info needed to create instance of DataType from ApxDataDao area and it's those
 * instances that provide the lowest-level of persistence management.
 *
 * Converters are identified by a unique and never changing ID; this ID is also scoped by the consistent
 * use of a prefix for whatever namespacing is needed.  For example, Apixio-supplied converter IDs will
 * all start with "apixio." (where the "." is just used for visual separation).
 *
 * Initialization/specification of converters is done via the protobuf class DataConverter in eval.proto
 */
public class Storage
{
    /**
     * List of Converter implementations.  DataType instances are dynamically generated from
     * client-supplied configuration.  Map is from interfaceClassname to Converter
     */
    private Map<String, Converter> converters = new HashMap<>();

    public Map<String, Converter> getConverters() {
        return converters;
    }

    /**
     * dynJars includes all accessor jars and f(x) implementation jar, in that order.  The
     * dynLoader is the single URLClassLoader that looks in all those jars.
     */
    private FxExecEnvironment environment;
    private FxLogger          logger;
    private ApxDataDao        apxDataDao;

    /**
     * Create a new storage management instance that can handle the converter classes listed
     * in classnames.
     */
    Storage(FxExecEnvironment fxEnv, ClassLoader dynLoader, List<String> classnames) throws Exception
    {
        environment = fxEnv;
        logger      = fxEnv.getLogger();

        apxDataDao = new ApxDataDao(environment.getDaoServices());

        for (String classname : classnames)
        {
            Class<Converter> clz       = (Class<Converter>) dynLoader.loadClass(classname);
            Converter        converter = clz.newInstance();
            Converter.Meta   md        = converter.getMetadata();

            logger.info("Initializing converter " + converter.getID() + " (" + classname + ")");

            converter.setEnvironment(environment);
            converters.put(md.interfaceClass.getCanonicalName(), converter);

            registerDataType(converter, md);
        }

        //!! TODO do we have to add any builtin converters??
    }

    Converter getConverter(String ifcClassname)
    {
        return converters.get(ifcClassname);
    }

    ApxDataDao getDataDao()
    {
        return apxDataDao;
    }

    /**
     * Creates an ApxDataDao DataType instance and registers with the dao in order to actually make
     * persistence of the protoClass work.
     */
    private void registerDataType(Converter converter, Converter.Meta md)
    {
        String         protoClassname = md.protoClass.getCanonicalName();
        DataType       dt             = new DataType(md.protoClass, protoClassname, converter.getID(), 0);

        logger.info("Registering datatype protoclassname=" + protoClassname);

        apxDataDao.registerDataType(dt);
    }

}
