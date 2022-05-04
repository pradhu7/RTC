package com.apixio.dao.blobdao.test;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;
import java.util.List;

import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;

import com.apixio.model.blob.BlobType;
import com.apixio.datasource.afs.ApixioFS;
import com.apixio.datasource.s3.S3Connector;
import com.apixio.datasource.s3.S3Ops;
import com.apixio.dao.storage.ApixioStorage;
import com.apixio.dao.blobdao.BlobDAO;
import com.apixio.dao.customerproperties.CustomerProperties;

public class BlobDAOTest
{
    private static Options cOptions = new Options();

    static
    {
        cOptions.addOption(null, "help", false, "Print help information and exit");
        cOptions.addOption(null, "input", true, "Input file/full path - required");
        cOptions.addOption(null, "loop", true, "loop / number of files to write and read - required");
        cOptions.addOption(null, "oregon", false, "Oregon - defaults to North Cal");
        cOptions.addOption(null, "sync", false, "defaults to async");
    }

    private static final String org = "1";

    private boolean runProgram;
    private String  inputFile;
    private int     loop;
    private boolean oregon;
    private boolean sync;

    private InputStream inputStream;
    private BlobDAO     blobDAO;

    public static void main(String[] args)
    {
        try
        {
            BlobDAOTest blobDAOTest = new BlobDAOTest(args);
            blobDAOTest.executeProgram();
        }
        catch (Throwable e)
        {
            System.out.println("ERROR: " + e);
            e.printStackTrace();
        }
        finally
        {
            System.exit(0);
        }
    }

    private BlobDAOTest(String[] args)
    {
        parseCommandLine(args);
    }

    private void parseCommandLine(String[] args)
    {
        if (args.length == 0)
        {
            runProgram = false;
            return;
        }

        try
        {
            CommandLineParser parser = new PosixParser();
            CommandLine line = parser.parse(cOptions, args);

            if (line.hasOption("help"))
            {
                runProgram = false;
            }
            else
            {
                runProgram = true;

                if (line.hasOption("input"))
                {
                    inputFile = line.getOptionValue("input");

                    File file = new File(inputFile);
                    if (!file.exists())
                    {
                        runProgram = false;
                        System.out.println("Input file doesn't exist: " + inputFile);
                        return;
                    }

                    try
                    {
                        inputStream = new FileInputStream(file);
                    }
                    catch (FileNotFoundException e)
                    {
                        runProgram = false;
                        System.out.println("Input file doesn't exist: " + inputFile);
                        return;
                    }
                }

                if (line.hasOption("loop"))
                {
                    loop = Integer.valueOf(line.getOptionValue("loop"));
                }

                if (line.hasOption("oregon"))
                {
                    oregon = true;
                }

                if (line.hasOption("sync"))
                {
                    sync = true;
                }
            }
        }
        catch (ParseException e)
        {
            e.printStackTrace();
            runProgram = false;
        }
    }

    private void printUsage(String name)
    {
        (new HelpFormatter()).printHelp(name + ": ", cOptions);
    }

    private void executeProgram() throws Exception
    {
        if (runProgram)
        {
            System.out.println("Program Started");
            setup();
            run();
            System.out.println("Program Done");
        }
        else
        {
            printUsage("BlobDAOTest");
        }
    }

    private void setup()
    {
        String container = oregon ? "pages-migration-test-or" : "pages-migration-test";
        String toStorageTypes = "S3";
        String fromStorageType = "S3";

        S3Connector s3Connector = new S3Connector();
        s3Connector.setUnencryptedAccessKey("AKIAIGCOTGSVPIUMSJJA");
        s3Connector.setUnencryptedSecretKey("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");
        s3Connector.init();

        S3Ops s3Ops = new S3Ops();
        s3Ops.setS3Connector(s3Connector);

        ApixioFS apxFs = new ApixioFS();
        apxFs.setS3Ops(s3Ops);

        ApixioStorage apxStorage = new ApixioStorage();
        apxStorage.setApixioFS(apxFs);
        apxStorage.setMountPoint(container);
        apxStorage.setToStorageTypes(toStorageTypes);
        apxStorage.setFromStorageType(fromStorageType);
        apxStorage.setCustomerProperties(getCustomerProperties());

        blobDAO = new BlobDAO();
        blobDAO.setApixioStorage(apxStorage);
    }

    private void run()
    {
        List<BlobType> types = new ArrayList<>();

        for (int i = 0; i < loop; i++)
        {
            long ct = System.currentTimeMillis();
            System.out.println("Write Loop Number " + i + " Started");

            BlobType blobType = new BlobType.Builder(UUID.randomUUID().toString(), "pdf").resolution("low").pageNumber(i).build();

            if (sync)
            {
                if (writeBlob(blobType, loop, false))
                    types.add(blobType);
            }
            else
            {
                if (writeBlob(blobType, loop, true))
                    types.add(blobType);
            }

            blobDAO.waitForBatchCompletion();

            System.out.println("Write Loop Number " + i + " Done in " + (System.currentTimeMillis() - ct));
        }

        blobDAO.waitForAllCompletion();
//        sleep(1000 * 60L);

        int j = 0;
        for (BlobType type : types)
        {
            long ct = System.currentTimeMillis();
            System.out.println("Read Loop Number " + j + " Started");

            readBlob(type);

            System.out.println("Read Loop Number " + j++ + " Done in " + (System.currentTimeMillis() - ct));
        }
    }

    private boolean writeBlob(BlobType blobType, int loop, boolean async)
    {
        try
        {
            blobDAO.write(blobType, inputStream, org, async);

            return true;
        }
        catch (Exception e)
        {
            System.out.println("Failed to write - loop number: " +  loop);
            e.printStackTrace();

            return false;
        }
    }

    private void readBlob(BlobType blobType)
    {
        try
        {
            InputStream result = blobDAO.read(blobType, org);
            if (result == null)
            {
                System.out.println("Failed to read - blobType: " + blobType);
                return;
            }

            if (!compareData(inputStream, result))
            {
                System.out.println("Files not equal - blobType: " + blobType);
            }
        }
        catch (Exception e)
        {
            System.out.println("Failed to read - blobType: " + blobType);
            e.printStackTrace();
        }
    }

    private CustomerProperties getCustomerProperties()
    {
        try
        {
            CustomerProperties customerProperties = new CustomerProperties();
            customerProperties.setNonPersistentMode();
            customerProperties.setAFSFolder(org, org);

            return customerProperties;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private boolean compareData(InputStream in1, InputStream in2) throws Exception
    {
        StringWriter writer1 = new StringWriter();
        IOUtils.copy(in1, writer1, Charset.defaultCharset());
        String theString1 = writer1.toString();

        StringWriter writer2 = new StringWriter();
        IOUtils.copy(in1, writer2, Charset.defaultCharset());
        String theString2 = writer2.toString();

        return theString1.equals(theString2);
    }

    private final static void sleep(long ms)
    {
        System.out.println("Sleep Started");
        try
        {
            Thread.sleep(ms);
        }
        catch (InterruptedException ix)
        {
        }
        System.out.println("Sleep Done");
    }
}
