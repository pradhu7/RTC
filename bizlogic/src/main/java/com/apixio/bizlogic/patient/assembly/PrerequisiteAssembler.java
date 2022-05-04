package com.apixio.bizlogic.patient.assembly;

import com.apixio.bizlogic.patient.assembly.merge.PatientSet;
import com.apixio.model.assembly.Assembler;

/**
 * Created by dyee on 1/23/18.
 */
public interface PrerequisiteAssembler<A, P> extends Assembler<A, P>
{
    /**
     * This implements the actual merge logic for the individual Assembler.
     *
     * @param patientSet
     */
    void actualMerge(PatientSet patientSet);

    /**
     *
     * The superclass must override this implementation. Any merges required to setup any MERGE_MAPS
     * must be placed here. Each superclass may have different implementation of this.
     *
     * @param patientSet
     */
    void prerequisiteMerges(PatientSet patientSet);
}
