package com.apixio.utility;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.zip.DataFormatException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataCompression {
	private static final Logger logger = LoggerFactory.getLogger(DataCompression.class);

	public byte[] compressData(byte[] input, boolean isCompressionNeeded) throws UnsupportedEncodingException, IOException {
		byte[] compressedBytes = null;
		if (isCompressionNeeded && input != null) {
			logger.debug("Uncompressed object byte size: " + input.length);

			ByteArrayOutputStream bs = new ByteArrayOutputStream(input.length);

			GZIPOutputStream gzipOutputStream = new GZIPOutputStream(bs);

			try
			{
				gzipOutputStream.write(input);
			}
			finally
			{
				gzipOutputStream.close();
				bs.close();
			}

			compressedBytes = bs.toByteArray();

			if (compressedBytes != null)
				logger.debug("Compressed object byte size: " + compressedBytes.length);
			else
				throw new IOException("Could not get compressed bytes.");
		} else {
			compressedBytes = input;
		}
		return compressedBytes;
	}

	public byte[] decompressData(byte[] input, boolean isCompressionDone) throws IOException, DataFormatException {
		byte[] result = null;
		if (isCompressionDone && input != null) {
			logger.debug("Decompressing -- Compressed object byte size: " + input.length);

			ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);
			IOUtils.copy(new GZIPInputStream(new ByteArrayInputStream(input)), bos);

			result = bos.toByteArray();

			if (result != null)
				logger.debug("After decompression -- uncompressed object byte size: " + result.length);
			else
				throw new IOException("Could not get decompressed bytes. (May be zip getNextEntry was null)");
		} else {
			result = input;
		}

		return result;
	}

	public OutputStream compressData(OutputStream outputStream) throws UnsupportedEncodingException, IOException {

		GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);

		return gzipOutputStream;
	}

	public InputStream decompressData(InputStream inputStream, boolean isCompressionDone) throws IOException, DataFormatException {
		if (isCompressionDone) {
			return new GZIPInputStream(inputStream);
		}

		return inputStream;
	}
}
