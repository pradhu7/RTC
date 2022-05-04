package com.apixio.model.external;

import org.junit.Test;

import static org.junit.Assert.*;

public class CodingSystemTest {

    @Test
    public void coding_system_resolves_by_oid() {
        assertEquals(CodingSystem.ICD_9CM_DIAGNOSIS_CODES, CodingSystem.byOid("2.16.840.1.113883.6.103"));
    }

    @Test
    public void returns_null_when_coding_system_does_not_resolve_by_oid() {
        assertNull(CodingSystem.byOid("3.16.840.1.113883.6.103"));
    }
}
