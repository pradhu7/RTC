package com.apixio.ensemblesdk.impl;

import com.apixio.dao.utility.DaoServices;
import com.apixio.ensemble.ifc.DataServices;
import com.apixio.ensemble.ifc.PlatformServices;
import com.apixio.ensemble.ifc.Reporter;
import com.apixio.ensemble.ifc.S3Services;
import com.apixio.ensemble.ifc.SequenceStoreServices;
import com.apixio.ensemble.ifc.TaggedResourceServices;
import com.apixio.sdk.FxLogger;

/**
 * SDK intentionally limits what MLC implementations can get.
 */
public class SdkPlatformServices implements PlatformServices
{

    private SdkS3Services sdkS3Services;

    SdkPlatformServices(DaoServices daoServices)
    {
        sdkS3Services = new SdkS3Services(daoServices.getS3Ops());
    }

    @Override
    public TaggedResourceServices getTaggedResources()
    {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public DataServices getDataServices()
    {
        throw new RuntimeException("Not implemented");  //!! page generators shouldn't be getting APOs
    }

    @Override
    public S3Services getS3Services()
    {
        return sdkS3Services;
    }

    @Override
    public SequenceStoreServices getSequenceStoreServices()
    {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Object getSparkSession()
    {
        throw new RuntimeException("Not implemented");
    }

}
