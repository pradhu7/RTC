package com.apixio.bizlogic.patient.assembly.merge;

import com.apixio.model.patient.BaseObject;
import java.util.LinkedHashMap;
import java.util.Map;

import com.apixio.model.patient.EditType;
import org.junit.Assert;
import org.junit.Test;

public class MergeBaseTest {

    @Test
    public void testConsistentOrderMetaData() {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("blah", "sdfjskdlafjsadf");
        metadata.put("ppppp", "daksjfklasdjfasdklfjadskf");
        metadata.put("bmw", "porsche");

        Map<String, String> metadata2 = new LinkedHashMap<>();
        metadata2.put("bmw", "porsche");
        metadata2.put("ppppp", "daksjfklasdjfasdklfjadskf");
        metadata2.put("blah", "sdfjskdlafjsadf");

        Assert.assertNotEquals(metadata.toString(), metadata2.toString());

        Assert.assertEquals(MergeBase.getConsistentOrderedMetaData(metadata),
                MergeBase.getConsistentOrderedMetaData(metadata2));
    }

    @Test
    public void testNullIdentityObject() {
        MergeBase mergeBase = new MergeBase() {
            @Override
            protected String getIdentity(BaseObject baseObject) {
                return null;
            }
        };

        BaseObject baseObject = new BaseObject() {
        };

        Assert.assertNull(mergeBase.getIdentity(baseObject));

        Assert.assertTrue(mergeBase.shouldProcess(baseObject).isEmpty());
        Assert.assertTrue(mergeBase.shouldProcess(null).isEmpty());
    }


    @Test
    public void testIdentityObject() {
        MergeBase mergeBase = new MergeBase() {
            @Override
            protected String getIdentity(BaseObject baseObject) {
                return "My-Identity";
            }
        };

        BaseObject baseObject = new BaseObject() {
        };

        String identity = mergeBase.getIdentity(baseObject);

        Assert.assertNotNull(identity);
        Assert.assertFalse(mergeBase.shouldProcess(baseObject).isEmpty());
        Assert.assertNotNull(mergeBase.getIdentityHash(baseObject));
    }

    @Test
    public void testInactiveMergeObject() {
        MergeBase mergeBase = new MergeBase() {
            @Override
            protected String getIdentity(BaseObject baseObject) {
                return "My-Identity";
            }
        };

        BaseObject baseObject = new BaseObject() {};
        baseObject.setEditType(EditType.ARCHIVE);

        String identity = mergeBase.getIdentity(baseObject);

        Assert.assertEquals("My-Identity", identity);
        Assert.assertEquals("bd5b5131baeab49bed0e7474d63736de46646f6e", mergeBase.getIdentityHash(baseObject));
        Assert.assertTrue(mergeBase.shouldProcess(baseObject).isEmpty());
    }

    @Test
    public void testAlreadyHasInactiveMergeObject() {
        MergeBase mergeBase = new MergeBase() {
            @Override
            protected String getIdentity(BaseObject baseObject) {
                return "My-Identity";
            }
        };

        BaseObject firstObject = new BaseObject() {};
        firstObject.setEditType(EditType.ARCHIVE); //set EditType as Archive or Delete (Inactive)
        String firstHash = mergeBase.getIdentityHash(firstObject);
        String firstKey = mergeBase.shouldProcess(firstObject);

        BaseObject secondObject = new BaseObject() {}; //Default EditType is Active
        String secondHash = mergeBase.getIdentityHash(secondObject);
        String secondKey = mergeBase.shouldProcess(secondObject);

        Assert.assertEquals("bd5b5131baeab49bed0e7474d63736de46646f6e", firstHash);
        Assert.assertEquals("bd5b5131baeab49bed0e7474d63736de46646f6e", secondHash);

        Assert.assertTrue(firstKey.isEmpty());
        Assert.assertTrue(secondKey.isEmpty());
    }

    @Test
    public void testTwoActiveMergeObjects() {
        MergeBase mergeBase = new MergeBase() {
            @Override
            protected String getIdentity(BaseObject baseObject) {
                return "My-Identity";
            }
        };

        BaseObject firstObject = new BaseObject() {}; //Default EditType is Active
        String firstHash = mergeBase.getIdentityHash(firstObject);
        String firstKey = mergeBase.shouldProcess(firstObject);

        BaseObject secondObject = new BaseObject() {}; //Default EditType is Active
        String secondHash = mergeBase.getIdentityHash(secondObject);
        String secondKey = mergeBase.shouldProcess(secondObject);

        Assert.assertEquals("bd5b5131baeab49bed0e7474d63736de46646f6e", firstHash);
        Assert.assertEquals("bd5b5131baeab49bed0e7474d63736de46646f6e", secondHash);

        Assert.assertEquals("bd5b5131baeab49bed0e7474d63736de46646f6e", firstKey);
        Assert.assertEquals("bd5b5131baeab49bed0e7474d63736de46646f6e", secondKey);
    }
}
