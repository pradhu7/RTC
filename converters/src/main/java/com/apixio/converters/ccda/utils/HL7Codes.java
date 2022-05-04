package com.apixio.converters.ccda.utils;

import java.util.HashMap;
import java.util.Map;

public class HL7Codes {

	static Map<String, String> TimingEvent;
	static
	{
		TimingEvent = new HashMap<String, String>();
		TimingEvent.put("AC", "Before meal");
		TimingEvent.put("ACM", "Before breakfast");
		TimingEvent.put("ACT", "Before lunch");
		TimingEvent.put("ACV", "Before dinner");
		TimingEvent.put("HS", "The hour of sleep");
		TimingEvent.put("IC", "Between meals");
		TimingEvent.put("ICD", "Between lunch and dinner");
		TimingEvent.put("ICM", "Between breakfast and lunch");
		TimingEvent.put("ICV", "Between dinner and the hour of sleep");
		TimingEvent.put("PC", "After meal");
		TimingEvent.put("PCD", "After lunch");
		TimingEvent.put("PCM", "After breakfast");
		TimingEvent.put("PCV", "After dinner");
	}
	
	static Map<String, String> ObservationInterpretation;
	static
	{
		ObservationInterpretation = new HashMap<String, String>();
		ObservationInterpretation.put("A", "Abnormal");
		ObservationInterpretation.put("HX", "above high threshold");
		ObservationInterpretation.put("LX", "below low threshold");
		ObservationInterpretation.put("B", "better");
		ObservationInterpretation.put("Carrier", "Carrier");
		ObservationInterpretation.put("D", "decreased");
		ObservationInterpretation.put("U", "increased");
		ObservationInterpretation.put("IND", "Indeterminate");
		ObservationInterpretation.put("I", "intermediate");
		ObservationInterpretation.put("MS", "moderately susceptible");
		ObservationInterpretation.put("NEG", "Negative");
		ObservationInterpretation.put("N", "Normal");
		ObservationInterpretation.put("POS", "Positive");
		ObservationInterpretation.put("R", "resistent");
		ObservationInterpretation.put("S", "susceptible");
	}
}
