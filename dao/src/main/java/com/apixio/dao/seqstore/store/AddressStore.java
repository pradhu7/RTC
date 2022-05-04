package com.apixio.dao.seqstore.store;

import java.util.List;

import com.apixio.datasource.cassandra.CqlCrud;
import com.apixio.model.event.EventType;
import com.apixio.model.event.EventAddress;

import com.apixio.datasource.cassandra.CqlCache;
import com.apixio.datasource.cassandra.LocalCqlCache;
import com.apixio.datasource.utility.EventDataUtility;
import com.apixio.utility.DataSourceUtility;
import com.apixio.dao.seqstore.utility.*;

public class AddressStore
{
    private EventDataUtility dataUtility = new EventDataUtility();

    private CqlCache cqlCache;

    public void setCqlCache(CqlCrud cqlCrud)
    {
        this.cqlCache = cqlCrud.getCqlCache();
    }

    public void put(List<EventType> eventTypes, String insertionTime, String columnFamily, LocalCqlCache localCqlCache)
        throws Exception
    {
        for (EventType eventType : eventTypes)
        {
            String key    = SeqStoreKeyUtility.prepareAddressKey(EventAddress.getEventAddress(eventType));
            String column = SeqStoreKeyUtility.prepareAddressColumn(insertionTime);

            byte[] data   = dataUtility.makeEventBytes(eventType, true);

            if (localCqlCache == null)
                DataSourceUtility.saveRawData(cqlCache, key, column, data, columnFamily);
            else
                DataSourceUtility.saveRawData(localCqlCache, key, column, data, columnFamily);
        }
    }
}
