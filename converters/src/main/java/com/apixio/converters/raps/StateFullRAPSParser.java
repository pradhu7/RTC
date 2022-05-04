package com.apixio.converters.raps;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Scanner;

import org.joda.time.DateTime;

import com.apixio.model.patient.Patient;

public class StateFullRAPSParser implements Iterable<Patient>, Iterator<Patient> {
	
	private Scanner lineScanner = null;
	private String currentLine = null;
	
	private String submitterId;
	private String fileId;
	private DateTime transactionDate;
	private String fileDiagType;
	
	public void close() {
		if(lineScanner != null) {
			lineScanner.close();
		}
	}
	
	public StateFullRAPSParser(InputStream is) throws Exception {
		lineScanner = new Scanner(is);
		if(lineScanner.hasNextLine())
			consumeAAARecord(lineScanner.nextLine());
		else 
			throw new StateFullRAPSParserException("AAA record not found in the begining of the file - might be an invalid raps file");
	}
	
	private void consumeAAARecord(String nextLine) throws StateFullRAPSParserException {
		// consumes an AAA Record, of which there is exactly one in the file
		String line = nextLine;
		if(line.startsWith("AAA")) {
			submitterId = line.substring(3,9);
			fileId = line.substring(9,19);
			transactionDate = DateTime.parse(line.substring(19,27));
			fileDiagType = line.substring(31,36);
			if(org.apache.commons.lang.StringUtils.isBlank(fileDiagType)) {
				fileDiagType = "ICD9";
			}
		}
		else {
			throw new StateFullRAPSParserException("AAA record should start with the string AAA");
		}
	}

	@Override
	public Iterator<Patient> iterator() {
		// TODO Auto-generated method stub
		return this;
	}

	@Override
	public boolean hasNext() {
		while(lineScanner != null && lineScanner.hasNext()) {
			currentLine = lineScanner.nextLine();
			if(currentLine.startsWith("CCC"))
				return true;
		}
		currentLine = null;
		return false;
	}
	
	@Override
	public Patient next() {
		// at this point, the calling of hasNext() would have positioned
		// our line pointer at the place where we have a ccc record.
		if(currentLine == null)
			throw new RuntimeException(new StateFullRAPSParserException("Invalid file or iterator exception"));
		
		CCCRecord record = new CCCRecord(currentLine, fileDiagType, submitterId, fileId, transactionDate);
		try {
			return record.getPatient();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Cannot remove on a constant iterator");		
	}

	public String getSubmitterId() {
		return submitterId;
	}

	public void setSubmitterId(String submitterId) {
		this.submitterId = submitterId;
	}

	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	public DateTime getTransactionDate() {
		return transactionDate;
	}

	public void setTransactionDate(DateTime transactionDate) {
		this.transactionDate = transactionDate;
	}

	public String getFileDiagType() {
		return fileDiagType;
	}

	public void setFileDiagType(String fileDiagType) {
		this.fileDiagType = fileDiagType;
	}
}
