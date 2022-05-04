package com.apixio.dao.patient2;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.apixio.model.patient.Patient;
import com.apixio.model.utility.PatientJSONParser;
import com.apixio.security.Security;
import com.apixio.utility.DataCompression;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class PatientDataUtility
{
    private static final Logger logger = LoggerFactory.getLogger(PatientDataUtility.class);

    private Security encryptor;
    public static PatientJSONParser parser = new PatientJSONParser();

    public PatientDataUtility()
    {
        encryptor = Security.getInstance();
    }

    public PatientDataUtility(Security sec)
    {
        encryptor = sec;
    }

    /**
     * This method accepts encrypted patient data bytes and returns Patient Model object of version mentioned in the param "version" depending on
     * other parameter whether the bytes need to uncompressed or not.
     *
     * @param patientData
     * @param isCompressed
     * @return
     * @throws Exception
     */
    public Patient getPatientObj(byte[] patientData, boolean isCompressed) throws Exception
    {
        if (patientData != null)
        {
            InputStream deCompressedBytes = getDecryptedDecompressedBytes(patientData, isCompressed);

            try
            {
                if (deCompressedBytes != null)
                {
                    Patient patient = parser.parsePatientData(deCompressedBytes);
                    return patient;
                } else
                    throw new Exception("Could not decrypt and uncompress data.");
            }
            finally
            {
                IOUtils.closeQuietly(deCompressedBytes);
            }
        }

        return null;
    }

    private InputStream getDecryptedDecompressedBytes(byte[] patientData, boolean isCompressed) throws Exception
    {
        DataCompression compressor = new DataCompression();
        if (patientData != null)
        {
            InputStream decryptedBytes = null;
            try
            {
                /* If it's compressed, we decrypt bytes to bytes. else since there is no compression, we will use normal decryption. */
                if (isCompressed)
                    decryptedBytes = encryptor.decryptBytesToInputStream(patientData, false);
                else
                    decryptedBytes = new ByteArrayInputStream(encryptor.decrypt(new String(patientData, "UTF-8")).getBytes("UTF-8"));
            }
            catch (Exception e)
            {
                logger.error("Could not decrypt the patient data sent.", e);
            }

            if (decryptedBytes != null)
                return compressor.decompressData(decryptedBytes, isCompressed);
        }

        return null;
    }

    /**
     * This method accepts encrypted patient data bytes and returns Patient Model object of version mentioned in the param "version" depending on
     * other parameter whether the bytes need to uncompressed or not.
     *
     * @param patientData
     * @param isCompressed
     * @return
     * @throws Exception
     */
    public PatientWithStringSize getPatientObjWithStrSize(byte[] patientData, boolean isCompressed) throws Exception
    {
        PatientWithStringSize result = null;
        if (patientData != null)
        {
            byte[] deCompressedBytes = IOUtils.toByteArray(getDecryptedDecompressedBytes(patientData, isCompressed));
            if (deCompressedBytes != null)
            {
                String strForm = new String(deCompressedBytes, "UTF-8");
                Patient patient = parser.parsePatientData(strForm);
                if (patient != null && strForm != null)
                {
                    result = new PatientWithStringSize();
                    result.setPatient(patient);
                    result.setStringSize(strForm.length());
                }
            }
            else
            {
                throw new Exception("Could not decrypt & uncompress data.");
            }
        }

        return result;
    }

    public byte[] makePatientBytesWithScope(Patient p, String scope, boolean compress) throws Exception
    {
        if (p != null)
        {
            DataCompression compression = new DataCompression();

            String patientObjStr = parser.toJSON(p);
            logger.debug("Patient " + p.getPatientId() + " size:" + patientObjStr.length());
            byte[] result = compression.compressData(patientObjStr.getBytes("UTF-8"), compress);

            if (compress)
                return encryptor.encryptBytesToBytesWithScope(result, scope);
            else
                return encryptor.encryptWithScope(new String(result, "UTF-8"), scope).getBytes("UTF-8");
        }

        return null;
    }
}
