package com.apixio.converters.ccr.utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.joda.time.DateTime;

import com.apixio.converters.base.BaseUtils;
import com.apixio.converters.ccr.jaxb.ActorType.Address;
import com.apixio.converters.ccr.jaxb.CodeType;
import com.apixio.converters.ccr.jaxb.CodedDescriptionType;
import com.apixio.converters.ccr.jaxb.CommunicationType;
import com.apixio.converters.ccr.jaxb.DateTimeType;
import com.apixio.converters.ccr.jaxb.Direction;
import com.apixio.converters.ccr.jaxb.Direction.Dose;
import com.apixio.converters.ccr.jaxb.Direction.Route;
import com.apixio.converters.ccr.jaxb.Directions;
import com.apixio.converters.ccr.jaxb.FrequencyType;
import com.apixio.converters.ccr.jaxb.IDType;
import com.apixio.converters.ccr.jaxb.IndicationType;
import com.apixio.converters.ccr.jaxb.Indications;
import com.apixio.converters.ccr.jaxb.MeasureType.Units;
import com.apixio.converters.ccr.jaxb.NormalType;
import com.apixio.converters.ccr.jaxb.QuantityType;
import com.apixio.converters.ccr.jaxb.Reaction;
import com.apixio.converters.ccr.jaxb.StructuredProductType.Product.Form;
import com.apixio.converters.ccr.jaxb.StructuredProductType.Product.Strength;
import com.apixio.converters.ccr.jaxb.StructuredProductType.Refills;
import com.apixio.converters.ccr.jaxb.TestResultType;
import com.apixio.converters.ccr.jaxb.TestType.NormalResult;
import com.apixio.converters.ccr.utils.CCRUtils.DateTypes;
import com.apixio.model.EitherStringOrNumber;
import com.apixio.model.patient.ClinicalCode;
import com.apixio.model.patient.Gender;
import com.apixio.model.patient.LabFlags;
import com.apixio.model.patient.ResolutionStatus;
import com.apixio.model.patient.TelephoneNumber;
import com.apixio.model.patient.TelephoneType;
import com.apixio.model.patient.TypedDate;


public class CCRUtils {

	public static enum DateTypes { START, END, SAMPLE, DIAGNOSIS };

	public static DateTime getDate(List<DateTimeType> dateTime, DateTypes dateType) {
		List<String> dateTypeList = getDateTypeList(dateType);
		if (dateTypeList != null) {
			return getTypedDate(dateTime, dateTypeList);
		}
		return null;
	}
	
	public static List<String> getDateTypeList(DateTypes dateType) {
		List<String> dateTypeList = null;
		if (dateType.equals(DateTypes.START))
			dateTypeList = CCRConstants.StartDates;
		if (dateType.equals(DateTypes.END))
			dateTypeList = CCRConstants.EndDates;
		if (dateType.equals(DateTypes.SAMPLE))
			dateTypeList = CCRConstants.SampleDates;
		if (dateType.equals(DateTypes.DIAGNOSIS))
			dateTypeList = CCRConstants.DiagnosisDates;
		return dateTypeList;
	}

	public static List<TypedDate> getOtherDates(List<DateTimeType> dateTimes, DateTypes dateTypes) {
		List<String> dateTypeList = getDateTypeList(dateTypes);
		List<TypedDate> typedDates = new LinkedList<TypedDate>();
		if (dateTimes != null) {
			for (DateTimeType dateTimeType : dateTimes) {
				Boolean addDate = true;
				String currentDateType = null;
				if (dateTypeList != null) {
					if (dateTimeType != null && dateTimeType.getType() != null) {
						currentDateType = getDescriptionText(dateTimeType.getType());
						for (String dateType : dateTypeList) {
							if (currentDateType.toUpperCase().contains(dateType))
								addDate = false;
						}
					}					
				}
				if (addDate) {
					TypedDate typedDate = getTypedDate(dateTimeType, currentDateType);
					if (typedDate != null)
						typedDates.add(typedDate);
				}
					
			}
		}
		return typedDates;
	}

		
	private static TypedDate getTypedDate(DateTimeType dateTimeType, String dateType) {
		TypedDate typedDate = new TypedDate();
		typedDate.setDate(getDate(dateTimeType));
		typedDate.setType(dateType);
		return typedDate;
	}

	public static DateTime getDate(DateTimeType dateTimeType) {
		DateTime date = null;

		try{
			date = BaseUtils.parseDateTime(dateTimeType.getExactDateTime());

		}catch(Exception e){
			//log.error("Document date is invalid.");
			date = null;
		}
		return date;
	}

	public static DateTime getFirstDate(List<DateTimeType> dateTime) {
		DateTime firstDate = null;
		if (dateTime != null && dateTime.size() > 0) {
			firstDate = getDate(dateTime.get(0));
		}
		return firstDate;
	}
	
	public static DateTime getTypedDate(List<DateTimeType> dateTime, List<String> types) {
		DateTime typedDate = null;
		if (dateTime != null) {
			for (DateTimeType dateTimeType : dateTime) {
				if (dateTimeType != null && dateTimeType.getType() != null) {
					String currentDateType = getDescriptionText(dateTimeType.getType());
					for (String dateType : types) {
						if (currentDateType.toUpperCase().contains(dateType))
							typedDate = getDate(dateTimeType);
					}
				}
			}
		}
		return typedDate;
	}
	public static ResolutionStatus getResolutionStatus(CodedDescriptionType codedDescriptionType) {
		ResolutionStatus resolutionStatus = null;
		if (codedDescriptionType != null) {
			if (codedDescriptionType.getText().equalsIgnoreCase("Active"))
				resolutionStatus = ResolutionStatus.ACTIVE;
			else if (codedDescriptionType.getText().equalsIgnoreCase("Resolved"))
				resolutionStatus = ResolutionStatus.RESOLVED;
			else if (codedDescriptionType.getText().equalsIgnoreCase("Inactive"))
				resolutionStatus = ResolutionStatus.INACTIVE;
		}
		return resolutionStatus;
	}

	public static String getStrengthValue(List<Strength> strength) {
		String strengthValue = null;
		if (strength != null && strength.size() > 0) {
			strengthValue = strength.get(0).getValue();
		}
		
		return strengthValue;
	}
	
	public static String getStrengthUnits(List<Strength> strength) {
		String strengthUnits = null;
		if (strength != null && strength.size() > 0) {
			Units units = strength.get(0).getUnits();
			if (units != null)
				strengthUnits = units.getUnit();
		}
		
		return strengthUnits;
	}

	public static String getFormText(List<Form> form) {
		String formText = null;
		if (form != null && form.size() > 0) {
			formText = form.get(0).getText();
		}
		
		return formText;
	}

	public static String getDescriptionText(CodedDescriptionType description) {
		if (description != null)
			return description.getText();
		return null;
	}

	public static String getReactionSeverity(List<Reaction> reaction) {
		String reactionSeverity = null;
		if (reaction != null && reaction.size() > 0)
			reactionSeverity = getDescriptionText(reaction.get(0).getSeverity());
		return reactionSeverity;
	}

	public static ClinicalCode getReactionDescription(List<Reaction> reaction) {
		ClinicalCode reactionDescription = null;
		if (reaction != null && reaction.size() > 0)
			reactionDescription = getClinicalCode(reaction.get(0).getDescription());
		return reactionDescription;
	}
	
	public static ClinicalCode getClinicalCode(CodedDescriptionType codedDescriptionType) {
		ClinicalCode code = null;
		
		if (codedDescriptionType != null) {
			code = new ClinicalCode();
			code.setDisplayName(codedDescriptionType.getText());
			if (codedDescriptionType.getCode() != null && codedDescriptionType.getCode().size() > 0) {
				CodeType primaryCode = codedDescriptionType.getCode().get(0);
				code.setCode(primaryCode.getValue());
				code.setCodingSystem(primaryCode.getCodingSystem());
				code.setCodingSystemOID(primaryCode.getCodingSystem());
				code.setCodingSystemVersions(primaryCode.getVersion());
			}
		}
		return code;
	}

	public static Boolean isActiveStatus(CodedDescriptionType status) {
		Boolean active = null;
		if (status != null) {
			if (status.getText().equalsIgnoreCase("active"))
				active = true;
			else if(status.getText().equalsIgnoreCase("inactive") ||
					status.getText().equalsIgnoreCase("Prior History No Longer Active"))
				active = false;			
		}
		return active;
	}

	public static Integer getIntegerFromBigInteger(List<BigInteger> number) {
		Integer integer = null;
		if (number != null && number.size() > 0) {
			integer = number.get(0).intValue();
		}
		return integer;
	}

	public static String getDoseValue(List<Dose> doseList) {
		String doseValue = null;
		if (doseList != null && doseList.size() > 0) {
			Dose dose = doseList.get(0);
			doseValue = dose.getValue();
			// TODO: Why were we appending the unit to the dose?
//			if (dose.getUnits() != null)
//				doseValue += " " + dose.getUnits().getUnit();
		}
		return doseValue;
	}
	
	public static String getDoseUnits(List<Dose> doseList) {
		String doseUnits = null;
		if (doseList != null && doseList.size() > 0) {
			Dose dose = doseList.get(0);
			if (dose != null)
			{
				Units units = dose.getUnits();
				if (units != null)
					doseUnits = units.getUnit();
			}
		}
		return doseUnits;
	}
	
	public static String getRouteValue(List<Route> routeList) {
		String routeValue = null;
		if (routeList != null && routeList.size() > 0) {
			routeValue = routeList.get(0).getText();
		}
		return routeValue;
	}

	public static String getFrequencyValue(List<FrequencyType> frequencyList) {
		String frequencyValue = null;
		if (frequencyList != null && frequencyList.size() > 0) {
			frequencyValue = frequencyList.get(0).getValue();
		}
		return frequencyValue;
	}

	public static String getDirectionsText(List<Directions> directions) {
		String directionsText = null;
		Direction direction = getFirstDirection(directions);
		if (direction != null) {
			directionsText = CCRUtils.getDescriptionText(direction.getDescription());
		}
		return directionsText;
	}

	public static String getDosageText(List<Directions> directions) {
		String dosageText = null;
		Direction direction = getFirstDirection(directions);
		if (direction != null) {
			dosageText = CCRUtils.getDoseValue(direction.getDose());
		}
		return dosageText;
	}

	public static String getDosageUnits(List<Directions> directions) {
		String dosageText = null;
		Direction direction = getFirstDirection(directions);
		if (direction != null) {
			dosageText = CCRUtils.getDoseUnits(direction.getDose());
		}
		return dosageText;
	}
	
	public static String getFrequencyText(List<Directions> directions) {
		String frequencyText = null;
		Direction direction = getFirstDirection(directions);
		if (direction != null) {
			frequencyText = CCRUtils.getFrequencyValue(direction.getFrequency());
		}
		return frequencyText;
	}

	public static Direction getFirstDirection(List<Directions> directionsList) {
		Direction direction = null;
		if (directionsList != null && directionsList.size() > 0) {
			Directions directions = directionsList.get(0);
			if (directions.getDirection() != null && directions.getDirection().size() > 0)
				direction = directions.getDirection().get(0);
		}
		return direction;
	}

	public static Integer getRefills(Refills refills) {
		Integer refillCount = null;
		if (refills != null && refills.getRefill() != null && refills.getRefill().size() > 0) {
			refillCount = CCRUtils.getIntegerFromBigInteger(refills.getRefill().get(0).getNumber());
		}
		return refillCount;
	}

	public static Double getQuantity(List<QuantityType> quantityType) {
		Double quantity = null;
		if (quantityType != null && quantityType.size() > 0) {
			try {
				quantity = Double.parseDouble(quantityType.get(0).getValue());
			} catch (Exception ex) {
				
			}
		}
		return quantity;
	}

	public static LabFlags getLabFlag(List<CodedDescriptionType> flag) {
		LabFlags labFlag = null;
		if (flag != null && flag.size() > 0) {
			String flagText = getDescriptionText(flag.get(0));
			if (flagText.equalsIgnoreCase("H") || flagText.equalsIgnoreCase("HIGH"))
				labFlag = LabFlags.HIGH;
			else if (flagText.equalsIgnoreCase("L") || flagText.equalsIgnoreCase("LOW"))
				labFlag = LabFlags.LOW;
			else if (flagText.equalsIgnoreCase("N") || flagText.equalsIgnoreCase("NORMAL"))
				labFlag = LabFlags.NORMAL;
			else if (flagText.equalsIgnoreCase("A") || flagText.equalsIgnoreCase("ABNORMAL"))
				labFlag = LabFlags.ABNORMAL;
		}
		return labFlag;
	}

	public static String getNormalResult(NormalResult normalResult) {
		String normalResultValue = null;
		if (normalResult != null && normalResult.getNormal() != null && normalResult.getNormal().size() > 0) {
			NormalType normal = normalResult.getNormal().get(0);
			normalResultValue = normal.getValue();
//			if (normal.getUnits() != null) {
//				normalResultValue += " " + normal.getUnits().getUnit();
//			}
		}
		return normalResultValue;
	}

	public static EitherStringOrNumber getTestResultValue(TestResultType testResult) {
		EitherStringOrNumber testResultValue = null;
		if (testResult != null) {
			String testResultString = testResult.getValue();
			try {
				testResultValue = new EitherStringOrNumber(Double.parseDouble(testResultString));				
			} catch (Exception ex) {
				testResultValue = new EitherStringOrNumber(testResultString);
			}
		}
		return testResultValue;
	}

	public static String getTestResultUnits(TestResultType testResult) {
		String testResultUnits = null;
		if (testResult != null && testResult.getUnits() != null) {
			testResultUnits = testResult.getUnits().getUnit();
		}
		return testResultUnits;
	}

	public static String getFirstIdString(List<IDType> ids) {
		String firstIdString = null;
		if (ids != null && ids.size() > 0) {
			firstIdString = ids.get(0).getID();
		}
		return firstIdString;
	}

	public static Gender getGender(CodedDescriptionType genderCode) {
		String genderText = getDescriptionText(genderCode);
		return BaseUtils.getGenderFromText(genderText);
	}
	
	public static TelephoneType getTelephoneType(CodedDescriptionType telephoneCode){
		String telephoneTypeText = getDescriptionText(telephoneCode);
		TelephoneType teleType = null;
		if(telephoneTypeText !=null){
			if (telephoneTypeText.equalsIgnoreCase("home") || telephoneTypeText.equalsIgnoreCase("home phone"))
				teleType = TelephoneType.HOME_PHONE;
			else if (telephoneTypeText.equalsIgnoreCase("work") || telephoneTypeText.equalsIgnoreCase("office") || telephoneTypeText.equalsIgnoreCase("work phone"))
				teleType = TelephoneType.WORK_PHONE;
			else if (telephoneTypeText.equalsIgnoreCase("cell") || telephoneTypeText.equalsIgnoreCase("mobile"))
				teleType = TelephoneType.CELL_PHONE;
		}
		
		return teleType;
	}

	public static boolean hasListItemContaining(List<String> stringList, String string) 
	{
		boolean contains = false;
		if (stringList != null)
		{
			for (String item : stringList)
			{
				if (item.toUpperCase().contains(string.toUpperCase()))
				{
					contains = true;
					break;
				}
			}		
		}
		
		return contains;
	}

	public static List<com.apixio.model.patient.Address> getAddressListFromAddressTypes(List<Address> addressList){
		List<com.apixio.model.patient.Address> primaryAddressList = new ArrayList<com.apixio.model.patient.Address>();
		if(addressList!=null){
			for(Address ccrAdd : addressList){				
				com.apixio.model.patient.Address primaryAddress = getAddressFromAddressTypes(ccrAdd);
								
				primaryAddressList.add(primaryAddress);
			}
		}
		return primaryAddressList;
	}


	public static com.apixio.model.patient.Address getFirstAddressFromAddressTypes(List<Address> addressList) {
		com.apixio.model.patient.Address firstAddress = null;
		if (addressList != null && addressList.size() > 0)
		{			
			firstAddress = getAddressFromAddressTypes(addressList.get(0));
		}

		return firstAddress;
	}
	
	
	public static com.apixio.model.patient.Address getAddressFromAddressTypes(Address ccrAdd) {
		com.apixio.model.patient.Address primaryAddress = new com.apixio.model.patient.Address();
		primaryAddress.setCity(ccrAdd.getCity());
		primaryAddress.setCountry(ccrAdd.getCountry());
		primaryAddress.setCounty(ccrAdd.getCounty());
		primaryAddress.setState(ccrAdd.getState());
		primaryAddress.setZip(ccrAdd.getPostalCode());
		
		//street address
		List<String> streetAddList = new ArrayList<String>();
		streetAddList.add(ccrAdd.getLine1());
		streetAddList.add(ccrAdd.getLine2());
		primaryAddress.setStreetAddresses(streetAddList);
		return primaryAddress;
	}

	public static List<String> getEmailsFromCommunicationType(List<CommunicationType> communicationList){
		List<String> emailList = new ArrayList<String>();
		if(communicationList != null){
			for(CommunicationType c : communicationList){
				emailList.add(c.getValue());
			}
		}
		return emailList;
	}
	
	public static List<TelephoneNumber> getTelephoneFromCommunicationType(List<CommunicationType> communicationList){
		List<TelephoneNumber> phoneList = new ArrayList<TelephoneNumber>();
		if(communicationList != null){
			for(CommunicationType c : communicationList){
				TelephoneNumber phone = new TelephoneNumber();
				phone.setPhoneNumber(c.getValue());				
				phone.setPhoneType(CCRUtils.getTelephoneType(c.getType()));
				phoneList.add(phone);
			}
		}
		return phoneList;
	}

	public static String getRouteText(List<Directions> directions) {
		String route = null;
		Direction direction = getFirstDirection(directions);
		if (direction != null) {
			route = CCRUtils.getRouteValue(direction.getRoute());
		}
		return route;
	}

	public static List<ClinicalCode> getIndicationsAsCodeList(List<Indications> indicationLists) {
		List<ClinicalCode> clinicalCodes = new LinkedList<ClinicalCode>();
		if (indicationLists != null) {
			for (Indications indicationList : indicationLists) {
				if (indicationList != null && indicationList.getIndication() != null) {
					for (IndicationType indication : indicationList.getIndication()) {
						if (indication != null && indication.getDescription() != null) {
							for (CodedDescriptionType indicationDescription : indication.getDescription()) {
								clinicalCodes.add(getClinicalCode(indicationDescription));								
							}
						}
					}
				}
			}			
		}
		return clinicalCodes;
	}

	public static Integer getSequenceNumber(NormalResult normalResult) {
		Integer sequenceNumber = null;
		if (normalResult != null && normalResult.getNormal() != null && normalResult.getNormal().size() > 0) {
			NormalType normal = normalResult.getNormal().get(0);
			if (normal.getValueSequencePosition() != null) {
				sequenceNumber = normal.getValueSequencePosition().intValue();				
			}
		}
		return sequenceNumber;
	}
}
