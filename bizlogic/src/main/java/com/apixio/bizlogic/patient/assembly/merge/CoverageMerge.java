package com.apixio.bizlogic.patient.assembly.merge;

import com.apixio.model.patient.Coverage;
import com.apixio.model.patient.ExternalID;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Source;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.joda.time.LocalDate;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Created by dyee on 5/1/17.
 */
public class CoverageMerge
{
    private static final String HEALTH_PLAN_META_KEY = "HEALTH_PLAN";

    public static void mergeCoverage(PatientSet patientSet, boolean ignoreNonHealthPlanSources)
    {
        Patient mergedPatient = CoverageMerge.merge(patientSet.basePatient, patientSet.additionalPatient, ignoreNonHealthPlanSources);

        for (Coverage mergedCoverage : mergedPatient.getCoverage())
        {
            patientSet.mergedPatient.addCoverage(mergedCoverage);
        }

        for (Source mergedSource : mergedPatient.getSources())
        {
            patientSet.mergedPatient.addSource(mergedSource);
        }
    }

    private static Patient merge(Patient p1, Patient p2, boolean ignoreNonHealthPlanSources)
    {
        //
        // If P1 or P2 has coverage information, we must make sure the sources are populated with
        // the health care information
        //

        if (hasCoverage(p1))
        {
            p1 = ensureSourcesHaveHealthPlanInformation(p1);
        }

        if (hasCoverage(p2))
        {
            p2 = ensureSourcesHaveHealthPlanInformation(p2);
        }

        Map<String, Map<Source, List<Coverage>>> coverageByHealthPlan = dedupeSourcesAndCoverage(ignoreNonHealthPlanSources, p1, p2);

        for (Map<Source, List<Coverage>> coverageBySource : coverageByHealthPlan.values())
        {
            mergeHealthPlanData(coverageBySource);
        }

        return restoreSources(assembleSummaryPatient(coverageByHealthPlan.values(), p1, p2), p1, p2);
    }

    private static String getPartID(Coverage coverage)
    {
        return coverage.getHealthPlanName();
    }

    /**
     * Merges health plan coverage information in place.
     * <p>
     * Removes irrelevant sources with their corresponding coverage spans.
     * Trims other relevant sources so that they are not overlapping.
     * Trims coverage spans to fit into the corresponding source's DCI.
     * <p>
     * This method has package-private access for testing purposes.
     *
     * @param coverageBySource Coverage grouped by source for a single health plan.
     */
    public static void mergeHealthPlanData(Map<Source, List<Coverage>> coverageBySource)
    {
        // Sort sources by date (descending).
        TreeSet<Source> sourcesSortedByDate = new TreeSet<>(new Comparator<Source>()
        {
            @Override
            public int compare(Source s1, Source s2)
            {
                int result = s2.getCreationDate().compareTo(s1.getCreationDate());
                // order doesn't matter when creation dates are equal.
                return result == 0 ? 1 : result;
            }
        });

        sourcesSortedByDate.addAll(coverageBySource.keySet());

        // Binary Search Tree which contains DCI start dates for newer (overriding) sources.
        // After a newer source is processed its DCI start date is added to this BST so older sources which conflict can find this one later.
        TreeMap<LocalDate, Source> dciStartBST = new TreeMap<>();

        // Binary Search Tree which contains DCI end dates for newer (overriding) sources.
        // After a newer source is processed its DCI end date is added to this BST so older sources which conflict can find this one later.
        TreeMap<LocalDate, Source> dciEndBST = new TreeMap<>();

        for (Source source : sourcesSortedByDate)
        {
            boolean executeDciIntervalChange = true;

            LocalDate dciStart = source.getDciStart();
            LocalDate dciEnd = source.getDciEnd();

            // find sources which relevance starts too late.
            Set<Source> irrelevantSources = new HashSet<>();
            for (Source irrelevant : dciStartBST.tailMap(dciEnd).values())
            {
                irrelevantSources.add(irrelevant);
            }

            // find sources which relevance ends not too early.
            Set<Source> overridingSources = new HashSet<>();
            for (Source maybeRelevant : dciEndBST.tailMap(dciStart).values())
            {

                // populate relevant sources
                if (!irrelevantSources.contains(maybeRelevant))
                {
                    overridingSources.add(maybeRelevant);
                }
            }

            for (Source newerSource : overridingSources)
            {
                LocalDate newerDciStart = newerSource.getDciStart();
                LocalDate newerDciEnd = newerSource.getDciEnd();

                // filter out corner case
                if (source.getCreationDate().isEqual(newerSource.getCreationDate())
                        && dciStart.isEqual(newerDciStart)
                        && dciEnd.isEqual(newerDciEnd))
                {
                    //System.out.println("case 0");
                    continue;
                }

                //
                // Case 1: Overriding DCI contains (or equal to) this DCI
                //
                // overriding DCI |----------------------|
                // this DCI                |----|
                //
                // overriding DCI |----------------------|
                // this DCI       |----------------------|
                //
                // overriding DCI |----------------------|
                // this DCI       |-----------|
                //
                // overriding DCI |----------------------|
                // this DCI                  |-----------|
                //
                if (!dciStart.isBefore(newerDciStart) && !dciEnd.isAfter(newerDciEnd))
                {
                    //System.out.println("case 1");
                    coverageBySource.remove(source);
                    executeDciIntervalChange = false;
                    break;
                }

                //
                // Case 2: This DCI *strictly* contains overriding DCI
                //
                // overriding DCI          |----|
                // this DCI       |----------------------|
                //
                // Since excluding overlapping period from thid DCI would required splitting the source object and regrouping its coverage spans
                // we are cheating here by keeping the DCI unchanged but merely filtering out coverage spans which are overriden.
                //
                if (dciStart.isBefore(newerDciStart) && dciEnd.isAfter(newerDciEnd))
                {
                    //System.out.println("case 2");
                    coverageBySource.put(source, excludeIntervalFromCoverage(coverageBySource.get(source), newerDciStart, newerDciEnd));
                    continue;
                }

                //
                // Case 3: This DCI partially overlaps with overriding DCI on the left
                //
                // overriding DCI |--------|
                // this DCI            |--------|
                //
                // overriding DCI |--------|
                // this DCI       |-------------|
                //
                if (newerDciEnd.isBefore(dciEnd))
                {
                    //System.out.println("case 3");
                    dciStart = newerDciEnd.plusDays(1);
                    continue;
                }

                //
                // Case 4: This DCI partially overlaps with overriding DCI on the right
                //
                // overriding DCI      |--------|
                // this DCI       |--------|
                //
                // overriding DCI      |--------|
                // this DCI       |-------------|
                //
                if (newerDciStart.isAfter(dciStart))
                {
                    //System.out.println("case 4");
                    dciEnd = newerDciStart.minusDays(1);
                    continue;
                }
            }

            // DCI interval has changed
            if (executeDciIntervalChange)
            {
                if (!source.getDciStart().isEqual(dciStart) || !source.getDciEnd().isEqual(dciEnd))
                {
                    // We need to reinsert source because its hash code will change after adjusting DCI
                    List<Coverage> spans = coverageBySource.remove(source);
                    Source clone = cloneWithDci(source, dciStart, dciEnd);

                    List<Coverage> restrictedSpans = restrictCoverageToInterval(spans, dciStart, dciEnd);

                    for (Coverage span : restrictedSpans)
                    {
                        span.setSourceId(clone.getInternalUUID());
                    }

                    // EN-10404: Sometimes when we change the DCI dates, it causes the map to have conflicting keys
                    // This is because we exlude internalUUID from our hash and equals functions
                    if (coverageBySource.containsKey(clone)) {
                        List<Coverage> other = coverageBySource.remove(clone);
                        for (Coverage span : other)
                        {
                            span.setSourceId(clone.getInternalUUID());
                        }
                        restrictedSpans.addAll(other);
                    }

                    coverageBySource.put(clone, restrictedSpans);
                    dciStartBST.put(dciStart, clone);
                    dciEndBST.put(dciEnd, clone);
                } else
                {
                    dciStartBST.put(dciStart, source);
                    dciEndBST.put(dciEnd, source);
                }
            }
        }
    }

    /**
     * Trims coverage spans to fit inside inclusion interval.
     * <p>
     * This method has package-private access for testing purposes.
     */
    public static List<Coverage> restrictCoverageToInterval(List<Coverage> spans, LocalDate includeFrom, LocalDate includeTo)
    {
        List<Coverage> result = new ArrayList<>();
        for (Coverage span : spans)
        {
            LocalDate spanStart = span.getStartDate();
            LocalDate spanEnd = span.getEndDate();

            // if coverage span is open-ended set its end date to DCI end date.
            if (spanEnd == null)
            {
                spanEnd = includeTo;
                span.setEndDate(spanEnd);
            }

            if (spanStart.isAfter(includeTo) || spanEnd.isBefore(includeFrom))
            {
                continue;
            }

            // trim coverage spans to fit inside DCI on the left
            if (spanStart.isBefore(includeFrom))
            {
                span.setStartDate(includeFrom);
            }

            // trim coverage spans to fit inside DCI on the right
            if (spanEnd.isAfter(includeTo))
            {
                span.setEndDate(includeTo);
            }

            result.add(span);
        }

        return result;
    }

    /**
     * Excludes interval from health plan coverage information.
     * <p>
     * This method has package-private access for testing purposes.
     *
     * @param spans       Source coverage span list.
     * @param excludeFrom
     * @param excludeTo
     * @return New spliced coverage span list to replace the original.
     * @precondition open-ended coverage spans were already truncated to their DCI.
     */
    private static List<Coverage> excludeIntervalFromCoverage(List<Coverage> spans, LocalDate excludeFrom, LocalDate excludeTo)
    {
        List<Coverage> result = new ArrayList<>();

        for (Coverage span : spans)
        {
            LocalDate spanStart = span.getStartDate();
            LocalDate spanEnd = span.getEndDate();

            //
            // Case 0: Exclusion interval doesn't overlap with a coverage span
            //
            // exclusion interval  |------|
            // coverage span                |------|
            //
            // exclusion interval           |------|
            // coverage span       |------|
            //
            if (spanStart.isAfter(excludeTo) || spanEnd.isBefore(excludeFrom))
            {
                result.add(span);
                continue;
            }

            //
            // Case 1: Exclusion interval contains (or equal to) a coverage span
            //
            // exclusion interval  |----------------------|
            // coverage span                |----|
            //
            // exclusion interval  |----------------------|
            // coverage span       |----------------------|
            //
            // exclusion interval  |----------------------|
            // coverage span       |-----------|
            //
            // exclusion interval  |----------------------|
            // coverage span                  |-----------|
            //
            if (!spanStart.isBefore(excludeFrom) && !spanEnd.isAfter(excludeTo))
            {
                // don't add to result
                continue;
            }

            //
            // Case 2: Coverage span *strictly* contains exclusion interval (coverage span has to be split).
            //
            // exclusion interval           |----|
            // coverage span       |----------------------|
            //
            if (spanStart.isBefore(excludeFrom) && spanEnd.isAfter(excludeTo))
            {

                Coverage left = new Coverage(span);
                left.setEndDate(excludeFrom.minusDays(1));
                result.add(left);

                Coverage right = new Coverage(span);
                right.setStartDate(excludeTo.plusDays(1));
                result.add(right);
                continue;
            }

            //
            // Case 3: Exclusion interval partially overlaps with coverage span on the left
            //
            // exclusion interval  |--------|
            // coverage span            |--------|
            //
            // exclusion interval  |--------|
            // coverage span       |-------------|
            //
            //
            if (spanEnd.isAfter(excludeTo))
            {
                span.setStartDate(excludeTo.plusDays(1));
                result.add(span);
                continue;
            }

            //
            // Case 4: Exclusion interval partially overlaps with coverage span on the right
            //
            // exclusion interval       |--------|
            // coverage span       |--------|
            //
            // exclusion interval       |--------|
            // coverage span       |-------------|
            //
            //
            if (spanStart.isBefore(excludeFrom))
            {
                span.setEndDate(excludeFrom.minusDays(1));
                result.add(span);
                continue;
            }
        }

        return result;
    }

    /**
     * Groups patient health plan coverage information into a form convenient for merging.
     * <p>
     * Groups coverage spans by their health plans and sources.
     * Only sources which have corresponding coverage spans are included.
     * Clones source objects - one for each original source and health plan.
     * <p>
     * If a patient has multiple equal sources their corresponding coverage spans get merged under a single source.
     */
    public static Map<String, Map<Source, List<Coverage>>> groupByHealthPlanAndSourceInitial(Patient patient)
    {
        Map<String, Map<Source, List<Coverage>>> result = new HashMap<>();
        Map<Source, Map<String, Source>> originalToClones = new HashMap<>();

        for (Coverage coverage : patient.getCoverage())
        {
            Source source = patient.getSourceById(coverage.getSourceId());
            String healthPlan = getPartID(coverage);
            if (StringUtils.isEmpty(healthPlan)) {
                //health plan is mandatoy and it is also used as partId
                continue;
            }
            Map<Source, List<Coverage>> coverageBySource = result.get(healthPlan);
            if (coverageBySource == null)
            {
                coverageBySource = new HashMap<>();
                result.put(healthPlan, coverageBySource);
            }

            Map<String, Source> clonesByHealthPlan = originalToClones.get(source);
            if (clonesByHealthPlan == null)
            {
                clonesByHealthPlan = new HashMap<>();
                originalToClones.put(source, clonesByHealthPlan);
            }

            Source clone = clonesByHealthPlan.get(healthPlan);
            if (clone == null)
            {
                clone = cloneWithHealthPlan(source, healthPlan);
                clonesByHealthPlan.put(healthPlan, clone);
            }

            List<Coverage> spans = coverageBySource.get(clone);
            if (spans == null)
            {
                spans = new ArrayList<>();
                coverageBySource.put(clone, spans);
            }

            spans.add(coverage);
            coverage.setSourceId(clone.getInternalUUID());
        }

        return result;
    }

    public static Patient restoreSources(Patient patient, Patient... APOs)
    {
        //
        // ensureSourcesHaveHealthPlanInformation, removed all coverage sources, and the merge
        // added back the merged source, this will add back the non coverage sources...
        //

        Map<String, String> seenMap = new HashMap<>();

        //
        // The patient already had sources from the coverage merge, pre-populate the
        // seenMap.
        //
        for(Source source: patient.getSources())
        {
            String partId = getPartID(source);
            seenMap.put(partId, partId);
        }

        for(Patient apo: APOs)
        {
            Iterable<Source> apoSources = apo.getSources();
            for(Source source : apoSources)
            {
                String partId = getPartID(source);
                if (!seenMap.containsKey(partId))
                {
                    patient.addSource(source);
                    seenMap.put(partId, partId);
                }
            }
        }

        return patient;
    }
    /**
     * Wraps merged eligibility information in a Summary APO.
     *
     * @param coverageBySourceMaps Merged coverage information for a single health plan.
     * @param APOs                 Patients to copy external identifiers from.
     */
    public static Patient assembleSummaryPatient(Iterable<Map<Source, List<Coverage>>> coverageBySourceMaps, Patient... APOs)
    {
        Patient summary = new Patient();
        copyIdentifiers(summary, APOs);

        for (Map<Source, List<Coverage>> coverageBySource : coverageBySourceMaps)
        {
            for (Source source : coverageBySource.keySet())
            {
                List<Coverage> spans = coverageBySource.get(source);

                //coverage information is for a single health plan, so only need to set it once.
                String healthPlan = null;
                for (Coverage coverage : spans)
                {
                    if(healthPlan == null) healthPlan = coverage.getHealthPlanName();
                    summary.addCoverage(coverage);
                }

                if(source.getMetaTag(HEALTH_PLAN_META_KEY) == null)
                {
                    source.setMetaTag(HEALTH_PLAN_META_KEY, healthPlan);
                }

                summary.addSource(source);
            }
        }
        return summary;
    }

    /**
     * Copies external identifiers from {from} to {to}.
     * <p>
     * Assumes all {from} patients have the same primary external identifier.
     */
    private static void copyIdentifiers(Patient to, Patient... from)
    {
        to.setPrimaryExternalID(from[0].getPrimaryExternalID());

        for (Patient patient : from)
        {
            for (ExternalID externalId : patient.getExternalIDs())
            {
                to.addExternalId(externalId);
            }
        }
    }

    //comvience wrapper
    public static boolean hasNoCoverage(Patient patient)
    {
        return !hasCoverage(patient);
    }

    public static boolean hasCoverage(Patient patient)
    {
        return patient.getCoverage().iterator().hasNext();
    }

    private static Source cloneWithDci(Source original, LocalDate dciStart, LocalDate dciEnd)
    {
        Source source = new Source();

        source.setDciStart(dciStart);
        source.setDciEnd(dciEnd);
        source.setCreationDate(original.getCreationDate());
        source.setSourceSystem(original.getSourceSystem());
        source.setSourceType(original.getSourceType());
        source.setSourceId(original.getSourceId());
        source.setMetadata(original.getMetadata());

        return source;
    }

    /**
     * Clones a source object and sets health plan information on it.
     */
    private static Source cloneWithHealthPlan(Source original, String healthPlan)
    {
        Source source = new Source();

        source.setDciStart(original.getDciStart());
        source.setDciEnd(original.getDciEnd());
        source.setCreationDate(original.getCreationDate());
        source.setSourceSystem(original.getSourceSystem());
        source.setSourceType(original.getSourceType());
        source.setSourceId(original.getSourceId());
        source.setMetaTag(HEALTH_PLAN_META_KEY, healthPlan);

        return source;
    }

    /**
     * Groups pre-merged eligibility information.
     *
     * If patients have multiple equals sources their corresponding coverage spans a merge to be under a single source.
     *
     * @Precondition All sources refer (or referred in the past) to a coverage object.
     * @Precondition All sources referenced from Coverage objects are present on the Patient object.
     */
    static private Map<String, Map<Source, List<Coverage>>> dedupeSourcesAndCoverage(boolean ignoreNonHealthPlanSources, Patient... patients)
    {
        // Map source object to themselves.
        // Equal source object may still have different internalUUID and we need to know to remap Coverage spans
        Map<String, Map<Source, Source>> seenByHealthPlan = new HashMap<>();

        Map<String, Map<Source, Set<Coverage>>> result = new HashMap<>();

        // prepopulate sources to not miss empty sources stored for DCI purposes.
        for (Patient patient : patients)
        {
            for (Source source : patient.getSources())
            {
                // Actually knowing health plan information is only needed for null-keyed summaries.
                String healthPlan = source.getMetaTag(HEALTH_PLAN_META_KEY);

                if(ignoreNonHealthPlanSources && healthPlan == null) continue;
                else Objects.requireNonNull(healthPlan, "Sources must be annotated with health plans.");

                Map<Source, Set<Coverage>> coverageBySource = result.get(healthPlan);
                if (coverageBySource == null)
                {
                    coverageBySource = new HashMap<>();
                    result.put(healthPlan, coverageBySource);
                }

                Map<Source, Source> seenBySource = seenByHealthPlan.get(healthPlan);
                if (seenBySource == null)
                {
                    seenBySource = new HashMap<>();
                    seenByHealthPlan.put(healthPlan, seenBySource);
                }

                if (!seenBySource.containsKey(source))
                {
                    seenBySource.put(source, source);
                    coverageBySource.put(source, new HashSet<Coverage>());
                }
            }
        }

        for (Patient patient : patients)
        {
            for (Coverage coverage : patient.getCoverage())
            {
                coverage = new Coverage(coverage);
                String healthPlan = coverage.getHealthPlanName();
                Map<Source, Set<Coverage>> coverageBySource = result.get(healthPlan);
                Objects.requireNonNull(coverageBySource,
                        "Invalid patient objects where supplied as arguments. Sources for some health plans are missing.");
                Map<Source, Source> seenSources = seenByHealthPlan.get(healthPlan);

                Source source = patient.getSourceById(coverage.getSourceId());

                Source seen = seenSources.get(source);
                Objects.requireNonNull(seen, "Each coverage span must have an associated source in the APO.");
                coverage.setSourceId(seen.getInternalUUID());

                Set<Coverage> spans = coverageBySource.get(source);
                spans.add(coverage);
            }
        }

        // convert set of coverages to list of coverages to satisfy interface requirements.
        Map<String, Map<Source, List<Coverage>>> asList = new HashMap<>();
        for (String healthPlan : result.keySet())
        {
            Map<Source, List<Coverage>> bySource = new HashMap<>();
            asList.put(healthPlan, bySource);
            for (Source source : result.get(healthPlan).keySet())
            {
                bySource.put(source, new ArrayList<>(result.get(healthPlan).get(source)));
            }
        }
        return asList;
    }

    static private Patient ensureSourcesHaveHealthPlanInformation(Patient apo)
    {
        //This will group the sources by health plan
        Map<String, Map<Source, List<Coverage>>> coverageByHealthPlan = groupByHealthPlanAndSourceInitial(apo);

        for (String healthPlan : coverageByHealthPlan.keySet())
        {
            for (Map.Entry<Source, List<Coverage>> entry : coverageByHealthPlan.get(healthPlan).entrySet())
            {
                Source source = entry.getKey();
                CoverageMerge.restrictCoverageToInterval(entry.getValue(), source.getDciStart(), source.getDciEnd());
            }

            CoverageMerge.mergeHealthPlanData(coverageByHealthPlan.get(healthPlan));
        }

        clearPremergeCoverageAndSources(apo);

        //
        // Now add back the merged health plan sources and coverage
        //
        for (String partID : coverageByHealthPlan.keySet())
        {
            for (Map<Source, List<Coverage>> coverageBySource : Arrays.asList(coverageByHealthPlan.get(partID)))
            {
                for (Source source : coverageBySource.keySet())
                {
                    List<Coverage> spans = coverageBySource.get(source);

                    //coverage information is for a single health plan, so only need to set it once.
                    String healthPlan = null;
                    for (Coverage coverage : spans)
                    {
                        if(healthPlan == null) healthPlan = coverage.getHealthPlanName();
                        apo.addCoverage(coverage);
                    }

                    //
                    // CoverageMerge requires us to have HEALTH_PLAN_META_KEY set, or else we get a runtime
                    // exception when trying to merge parts later.
                    //
                    if(source.getMetaTag(HEALTH_PLAN_META_KEY) == null)
                    {
                        source.setMetaTag(HEALTH_PLAN_META_KEY, healthPlan);
                    }

                    apo.addSource(source);
                }
            }
        }

        return apo;
    }

    static private void clearPremergeCoverageAndSources(Patient apo)
    {
        //
        // Build a source map, of all sources in the apo
        //
        Map<UUID, Source> sourceMap = new HashMap<>();

        for(Source source : apo.getSources()) {
            sourceMap.put(source.getInternalUUID(), source);
        }

        //
        // Remove all sources associated with a coverage
        //
        for(Coverage coverage : apo.getCoverage()) {
            sourceMap.remove(coverage.getSourceId());
        }

        //
        // We are going to merge the coverage and sources based on the Coverage Merge logic
        // Reset the lists, and repopulate with merged logic
        //
        apo.setCoverage(new ArrayList<Coverage>());

        //
        //Copy over any sources that are not associated with coverage...
        //
        apo.setSources(new ArrayList<Source>(sourceMap.values()));
    }

    private static String getPartID(Source e)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(getIdentity(e).getBytes(Charset.forName("UTF-8")));
            return Hex.encodeHexString(md.digest());
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new RuntimeException("Could not obtain partId", ex);
        }
    }

    private static String getIdentity(Source e)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("sourceSystem").append(e.getSourceSystem())
                .append("sourceType").append(e.getSourceType());

        if (e.getClinicalActorId() != null)
            sb.append("clinicalActorId").append(e.getClinicalActorId().toString());

        if (e.getOrganization() != null)
            sb.append("organization").append(e.getOrganization().getName());

        if (e.getCreationDate() != null)
            sb.append("creationDate").append(e.getCreationDate().toDate().getTime());

        if (e.getDciStart() != null)
            sb.append("dciStart").append(e.getDciStart().toDate().getTime());

        if (e.getDciEnd() != null)
            sb.append("dciEnd").append(e.getDciEnd().toDate().getTime());

        return sb.toString();
    }
}
