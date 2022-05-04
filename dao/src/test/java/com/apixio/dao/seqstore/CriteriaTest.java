package com.apixio.dao.seqstore;

import com.apixio.dao.seqstore.utility.Criteria.TagCriteria;
import com.apixio.dao.seqstore.utility.Criteria.TagTypeCriteria;
import com.apixio.dao.seqstore.utility.Range;
import com.apixio.model.event.ReferenceType;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

/**
 * Created by dyee on 12/6/16.
 */
public class CriteriaTest {

    @Test
    public void testTagCriteria() {
        ReferenceType subject = new ReferenceType();
        String orgId = "testOrgId";
        String addressId = "testAddressId";
        String path = "testPath";
        String tag = "testTag";
        String value = "testValue";

        Range range = Range.DEFAULT_RANGE;


        TagCriteria criteria = new TagCriteria.Builder().setSubject(subject)
                .setOrgId(orgId)
                .setAddressId(addressId)
                .setPath(path)
                .setRange(range)
                .setTags(Collections.singletonList(tag))
                .setValue(value)
                .setRange(range).build();

        Assert.assertEquals(subject, criteria.getSubject());
        Assert.assertEquals(orgId, criteria.getOrgId());
        Assert.assertEquals(addressId, criteria.getAddressId());
        Assert.assertEquals(path, criteria.getPath());
        Assert.assertEquals(tag, criteria.getTags().get(0));
        Assert.assertEquals(value, criteria.getValue());
    }

    @Test
    public void testTagTypeCriteria() {
        ReferenceType subject = new ReferenceType();
        String orgId = "testOrgId";
        String addressId = "testAddressId";
        String path = "testPath";
        String value = "testValue";

        SeqStoreDAO.TagType tagType = SeqStoreDAO.TagType.Inferred;

        Range range = Range.DEFAULT_RANGE;


        TagTypeCriteria criteria = new TagTypeCriteria.Builder().setSubject(subject)
                .setOrgId(orgId)
                .setAddressId(addressId)
                .setPath(path)
                .setRange(range)
                .setTagType(tagType)
                .setValue(value)
                .setRange(range).build();

        Assert.assertEquals(subject, criteria.getSubject());
        Assert.assertEquals(orgId, criteria.getOrgId());
        Assert.assertEquals(addressId, criteria.getAddressId());
        Assert.assertEquals(path, criteria.getPath());
        Assert.assertEquals(tagType, criteria.getTagType());
        Assert.assertEquals(value, criteria.getValue());
    }
}
