package com.apixio.bizlogic.patient.logic;

import com.apixio.dao.Constants;
import com.apixio.dao.utility.LinkDataUtility;
import com.apixio.model.nassembly.AssemblySchema;
import com.apixio.model.nassembly.CPersistable;
import com.apixio.nassembly.AssemblyLogic;
import com.apixio.nassembly.locator.AssemblyLocator;
import com.apixio.nassembly.patientcategory.PatientCategoryTranslator;
import com.apixio.useracct.buslog.PatientDataSetLogic;

import java.util.*;
import java.util.stream.Collectors;

public class PatientLogicUtils
{
    public static UUID reservePatientUUID(PatientLogic patientLogic, String pdsID,  String patientKey)
        throws Exception
    {
        UUID patientUUID = patientLogic.getPatientUUID(pdsID, patientKey, Constants.KeyType.partialPatientKey);

        if (patientUUID == null)
        {
            // VK: MAKE SURE IT IS FALSE FOR NOW FOR BACKWARD COMPATIBILITY
            patientUUID = patientLogic.reservePatientUUID(pdsID, patientKey, false);
        }

        return patientUUID;
    }

    // VK - TODO - Error checking
    public static List<AssemblyLogic.AssemblyQueryResult> getAssemblyForDataType(PatientLogicBase patientLogic, String pdsID, UUID patientUUID, PatientCategoryTranslator translator, String partID) throws Exception
    {
        pdsID = PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        // we are replacing non auth patient uuid with auth one!!
        LinkDataUtility.PdsIDAndPatientUUID pdsIDAndPatientUUID = patientLogic.getPdsIDAndAuthPatientUUIDdByPatientUUID(patientUUID);
        if (pdsIDAndPatientUUID == null)
            return null;

        List<CPersistable> CPersistables = Arrays.stream(translator.getDataTypeNames(partID))
                .map(AssemblyLocator::getPersistable)
                .collect(Collectors.toList());

        String finalPdsID = pdsID;

        String[] translatedPartIds = translator.translatePartKeys(partID);

        return CPersistables.stream().map(cPersistable -> {
            com.apixio.nassembly.AssemblyLogic.AssemblyMeta assemblyMeta = new com.apixio.nassembly.AssemblyLogic.AssemblyMeta(cPersistable.getSchema(), finalPdsID, cPersistable.getDataTypeName());

            Map<String, Object> clusterColFieldNamesToValues = new HashMap<>();
            AssemblySchema.AssemblyCol[] clusteringCols = cPersistable.getSchema().getClusteringCols();

            int i = 0;
            for (String colVal: translatedPartIds)
            {
                clusterColFieldNamesToValues.put(clusteringCols[i].getName(), colVal);
                i++;
            }

            //clusterColFieldNamesToValues.put(cPersistable.fromColFieldNamesToPersistentFieldNamesForClusteredColumns().get(0).getPersistentFieldName(), partID);
            com.apixio.nassembly.AssemblyLogic.AssemblyData assemblyData = new com.apixio.nassembly.AssemblyLogic.AssemblyData(patientUUID.toString(),
                    clusterColFieldNamesToValues, null, null);

            return patientLogic.newAssemblyLogic.read(assemblyMeta, assemblyData);
        })
                .flatMap(Collection::stream)
                .collect(Collectors.toList());


    }

    // VK - TODO - Error checking
    public static List<AssemblyLogic.AssemblyQueryResult> getAssemblyForDataType(PatientLogicBase patientLogic, String pdsID, UUID patientUUID, PatientCategoryTranslator translator) throws Exception
    {
        pdsID = PatientDataSetLogic.normalizePdsIdToLongFormat(pdsID);

        // we are replacing non auth patient uuid with auth one!!
        LinkDataUtility.PdsIDAndPatientUUID pdsIDAndPatientUUID = patientLogic.getPdsIDAndAuthPatientUUIDdByPatientUUID(patientUUID);
        if (pdsIDAndPatientUUID == null)
            return null;


        List<CPersistable> CPersistables = Arrays.stream(translator.getDataTypeNames(null))
                .map(AssemblyLocator::getPersistable)
                .collect(Collectors.toList());

        String finalPdsID = pdsID;
        return CPersistables.stream()
                .map(CPersistable -> {
                    com.apixio.nassembly.AssemblyLogic.AssemblyMeta assemblyMeta = new com.apixio.nassembly.AssemblyLogic.AssemblyMeta(CPersistable.getSchema(), finalPdsID, CPersistable.getDataTypeName());

                    com.apixio.nassembly.AssemblyLogic.AssemblyData assemblyData = new com.apixio.nassembly.AssemblyLogic.AssemblyData(pdsIDAndPatientUUID.getPatientUUIDAsString(),
                            null, null, null);

                    return patientLogic.newAssemblyLogic.read(assemblyMeta, assemblyData);
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
