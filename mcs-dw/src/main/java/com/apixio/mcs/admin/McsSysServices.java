package com.apixio.mcs.admin;

import com.apixio.SysServices;
import com.apixio.dao.utility.DaoServices;
import com.apixio.dao.utility.DaoServicesSet;
import com.apixio.fxs.FxCatalogService;
import com.apixio.mcs.ModelCatalogService;
import com.apixio.mcs.admin.dw.McsConfig;
import com.apixio.restbase.DaoBase;

public class McsSysServices extends SysServices
{
    private DaoBase              microServiceDao;
    private DaoServices          daoServices;
    private ModelCatalogService  mcsCatalogService;
    private FxCatalogService     fxCatalogService;

    public McsSysServices(DaoBase seed, McsConfig config) throws Exception
    {
        super(seed, config);

        this.microServiceDao   = seed;
        this.daoServices       = DaoServicesSet.createDaoServices(config.getMicroserviceConfig());
        this.mcsCatalogService = new ModelCatalogService(daoServices, config.getMcsConfig());
        this.fxCatalogService  = new FxCatalogService(daoServices, config.getMcsConfig());
    }

    public DaoBase getMicroserviceDao()
    {
        return microServiceDao;
    }

    public DaoServices getDaoServices()
    {
        return daoServices;
    }

    public ModelCatalogService getModelCatalogService()
    {
        return mcsCatalogService;
    }

    public FxCatalogService getFxCatalogService()
    {
        return fxCatalogService;
    }
}
