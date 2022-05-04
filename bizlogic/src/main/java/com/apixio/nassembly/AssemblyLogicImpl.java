package com.apixio.nassembly;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.apixio.dao.nassembly.AssemblyDAO;
import com.apixio.dao.nassembly.AssemblyDAO.QueryResult;
import com.apixio.dao.utility.DaoServices;
import com.apixio.model.nassembly.AssemblySchema;

/**
 * AssemblyLogic contains business logic related to managing assembly things.
 * It contains all functionality for writing and reading final/merged/derived assembly data.
 */

public class AssemblyLogicImpl implements AssemblyLogic
{
    private AssemblyDAO assemblyDAO;
    private String      dataTypeTable;
    private Marshalling marshalling;


    public AssemblyLogicImpl(DaoServices daoServices)
    {
        this.assemblyDAO         = daoServices.getNewAssemblyDAO();
        this.dataTypeTable       = daoServices.getNewAssemblyDataType();
        this.marshalling         = new Marshalling();
    }

    /**
     * Write an assembly object given an assemblyMeta and assemblyData
     *
     * @param assemblyMeta
     * @param assemblyData
     */
    @Override
    public void write(AssemblyMeta assemblyMeta, AssemblyData assemblyData)
    {
        assemblyDAO.write(assemblyMeta.toDAOMeta(dataTypeTable), assemblyData.toDAOData(marshalling, assemblyMeta));
    }

    /**
     * Delete a certain parts (columns) of an assembly object given an assemblyMeta and assemblyData
     *
     * @param assemblyMeta
     * @param assemblyData
     */
    @Override
    public void deleteColumns(AssemblyMeta assemblyMeta, AssemblyData assemblyData)
    {
        assemblyDAO.deleteColumns(assemblyMeta.toDAOMeta(dataTypeTable), assemblyData.toDAOData(assemblyMeta));
    }

    /**
     * Delete an assembly object given an assemblyMeta and assemblyData
     *
     * @param assemblyMeta
     * @param assemblyData
     */
    @Override
    public void deleteRow(AssemblyMeta assemblyMeta, AssemblyData assemblyData)
    {
        assemblyDAO.deleteRow(assemblyMeta.toDAOMeta(dataTypeTable), assemblyData.toDAOData(assemblyMeta));
    }

    /**
     * flush all in memory data to disk
     *
     */
    @Override public void flush()
    {
        assemblyDAO.flush();
    }

    /**
     * Read an assembly object given an assemblyMeta and assemblyData
     *
     * @param assemblyMeta
     * @param assemblyData
     * @return List of Assembly Query Result
     */
    @Override
    public List<AssemblyQueryResult> read(AssemblyMeta assemblyMeta, AssemblyData assemblyData)
    {
        List<QueryResult> queryResults = assemblyDAO.read(assemblyMeta.toDAOMeta(dataTypeTable), assemblyData.toDAOData(assemblyMeta));

        List<AssemblyQueryResult> assemblyQueryResults = new ArrayList<>();

        for (QueryResult qr : queryResults)
        {
            assemblyQueryResults.add(buildNewAssemblyQueryResult(assemblyMeta.getAssemblySchema(), qr));
        }

        return assemblyQueryResults;
    }

    /**
     * get the list of dataTypes given an assemblyMeta and assemblyID
     *
     * @param assemblyMeta
     * @param assemblyID
     * @return List of dataTypeNames
     */
    @Override
    public List<String> getDataTypes(AssemblyMeta assemblyMeta, String assemblyID)
    {
        return assemblyDAO.getDataTypes(assemblyMeta.toDAOMeta(dataTypeTable), assemblyID);
    }

    private AssemblyQueryResult buildNewAssemblyQueryResult(AssemblySchema assemblySchema, QueryResult qr)
    {
        InputStream  proto = marshalling.decrypt(qr.getProtobuf());

        AssemblyQueryResult queryResult = new AssemblyQueryResult(qr.getRowKeyValue(), qr.getColFieldNamesToValues(), proto, qr.getTimeInMicroseconds());
        queryResult.fromPhysicalToLogicalData(assemblySchema);
        return queryResult;
    }
}
