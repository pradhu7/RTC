/**
 * 
 */
package com.apixio.model.file.catalog.jaxb.utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipInputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.apixio.model.file.catalog.jaxb.generated.ApxCatalog;
import org.apache.commons.io.IOUtils;

/**
 * @author gaurav
 * 
 */
public class CatalogUtility {
    private static JAXBContext jaxbContext = null;
    static private ThreadLocal<Marshaller> marshallers = new ThreadLocal<Marshaller>();
    static private ThreadLocal<Unmarshaller> unmarshallers = new ThreadLocal<Unmarshaller>();

	//
	// @TODO: we need to tune this to run at a good trade off between memory, cpu load, and performance.
	//        These are set to reasonable defaults...
	//
	static private final int DEFAULT_BUFFER_SIZE = 1024 * 4;
	static private final int DEFAULT_EXECUTOR_CONCURRENCY_LEVEL = 10;
	static private final ExecutorService executorService = Executors.newFixedThreadPool(DEFAULT_EXECUTOR_CONCURRENCY_LEVEL);

    static private boolean first = true; // not volatile- race condition is ok

    private static void init() throws JAXBException {
        if (first) {
            jaxbContext = JAXBContext.newInstance("com.apixio.model.file.catalog.jaxb.generated");
            first = false;
        }
        if (marshallers.get() == null) {
            marshallers.set(jaxbContext.createMarshaller());
        }
        if (unmarshallers.get() == null) {
            unmarshallers.set(jaxbContext.createUnmarshaller());
        }
   }

	/**
	 * Converts the input stream of the catalog file to JavaBean ApxCatalog
	 *
	 */
	public static ApxCatalog convertToObject(final InputStream inputStream) throws JAXBException, IOException
	{
		init();
		Unmarshaller unmarshaller = unmarshallers.get();

		InputStream in = inputStream;
		if(inputStream instanceof ZipInputStream) {
			in = convertToInputStream(inputStream);
		}

		return (ApxCatalog) unmarshaller.unmarshal(in);
	}

	public static InputStream convertToInputStream(final InputStream inputStream) throws IOException
	{
		final PipedOutputStream outputStream = new PipedOutputStream();

		PipedInputStream in = new PipedInputStream(outputStream);

		executorService.submit(new Callable()
		{
			@Override
			public Object call() throws Exception
			{
				try
				{
					byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
					int n = 0;
					while (-1 != (n = inputStream.read(buffer)))
					{
						outputStream.write(buffer, 0, n);
					}
				} catch (IOException e)
				{
					e.printStackTrace();
					// logging and exception handling should go here
				} finally
				{
					IOUtils.closeQuietly(outputStream);
				}
				return 0;
			}
		});

		return in;
	}

	public static InputStream convertToInputStream(final ByteArrayOutputStream outputStream) throws IOException
	{
		final PipedOutputStream out = new PipedOutputStream();
		PipedInputStream in = new PipedInputStream(out);

		executorService.submit(new Callable()
		{
			@Override
			public Object call() throws Exception
			{
				try
				{
					outputStream.writeTo(out);
				}
				catch (Exception ex) {
					ex.printStackTrace();
				}
				finally {
					IOUtils.closeQuietly(out);
				}
				return 0;
			}
		});

		return in;
	}


	public static ByteArrayOutputStream convertToOutputStream(ApxCatalog catalogContent) throws JAXBException {
		init();
		Marshaller marshaller = marshallers.get();
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		marshaller.marshal(catalogContent, os);

		return os;
	}

	/**
	 * To convert the JavaBean ApxCatalog to byte[] (serializing it)
	 * 
	 * @param catalogContent
	 * @return
	 * @throws JAXBException
	 */
	public static byte[] convertToBytes(ApxCatalog catalogContent) throws JAXBException {
		return convertToOutputStream(catalogContent).toByteArray();
	}
}
