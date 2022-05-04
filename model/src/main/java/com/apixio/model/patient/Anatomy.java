package com.apixio.model.patient;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Anatomy extends CodedBaseObject {
    private String anatomicalStructureName;

    public String getAnatomicalStructureName() {
        return anatomicalStructureName;
    }

    public void setAnatomicalStructureName(String anatomicalStructureName) {
        this.anatomicalStructureName = anatomicalStructureName;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(anatomicalStructureName).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj instanceof Anatomy) {
            return new EqualsBuilder().append(this.anatomicalStructureName, ((Anatomy) obj).anatomicalStructureName).isEquals();
        }
        return false;
    }


}
