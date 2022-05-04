package com.apixio.dao.seqstore.utility;

import com.apixio.dao.seqstore.SeqStoreDAO;
import com.apixio.model.event.ReferenceType;

import java.util.List;

/**
 * Created by dyee on 12/6/16.
 */
public abstract class Criteria {
    private ReferenceType subject;
    private String orgId;
    private Range range;
    private String path;
    private String value;
    private String addressId;
    private boolean allowConcurrentQuery;
    private boolean allowMultipleUUIDCorrection = true;

    public Criteria(ReferenceType subject, String orgId, String path, String value, String addressId, Range range, boolean allowConcurrentQuery, boolean allowMultipleUUIDCorrection)
    {
        this.subject = subject;
        this.orgId = orgId;
        this.range = range;
        this.value = value;
        this.path = path;
        this.addressId = addressId;
        this.allowConcurrentQuery = allowConcurrentQuery;
        this.allowMultipleUUIDCorrection = allowMultipleUUIDCorrection;
    }

    public ReferenceType getSubject() {
        return subject;
    }

    public String getOrgId() {
        return orgId;
    }

    public Range getRange() {
        return range;
    }

    public String getPath() {
        return path;
    }

    public String getValue() {
        return value;
    }

    public String getAddressId() {
        return addressId;
    }

    public boolean isAllowConcurrentQuery()
    {
        return allowConcurrentQuery;
    }

    public boolean isAllowMultipleUUIDCorrection()
    {
        return allowMultipleUUIDCorrection;
    }

    /**
     *  Tag Criteria
     */
    public static class TagCriteria extends Criteria {
        private List<String> tags;

        public TagCriteria(ReferenceType subject, String orgId, String path, String value, String addressId, Range range, List<String> tags, boolean allowConcurrentQuery, boolean allowMultipleUUIDCorrectio) {
            super(subject, orgId, path, value, addressId, range, allowConcurrentQuery, allowMultipleUUIDCorrectio);
            this.tags = tags;
        }

        public List<String> getTags() {
            return tags;
        }

        public static class Builder {
            private List<String> tags;
            private ReferenceType subject;
            private String orgId;

            private String path;
            private String value;
            private String addressId;

            private Range range = Range.DEFAULT_RANGE;

            private boolean allowConcurrentQuery = false;
            private boolean allowMultipleUUIDCorrection = true;

            public Builder setAddressId(String addressId) {
                this.addressId = addressId;
                return this;
            }

            public Builder setPath(String path) {
                this.path = path;
                return this;
            }

            public Builder setSubject(ReferenceType subject) {
                this.subject = subject;
                return this;
            }

            public Builder setOrgId(String orgId) {
                this.orgId = orgId;
                return this;
            }

            public Builder setRange(Range range) {
                this.range = range;
                return this;
            }

            public Builder setTags(List<String> tags) {
                this.tags = tags;
                return this;
            }

            public Builder setValue(String value) {
                this.value = value;
                return this;
            }

            public Builder setAllowConcurrentQuery(boolean allowConcurrentQuery)
            {
                this.allowConcurrentQuery = allowConcurrentQuery;
                return this;
            }

            public Builder setAllowMultipleUUIDCorrection(boolean allowMultipleUUIDCorrection)
            {
                this.allowMultipleUUIDCorrection = allowMultipleUUIDCorrection;
                return this;
            }

            public TagCriteria build() {
                return new Criteria.TagCriteria(subject, orgId, path, value, addressId, range, tags, allowConcurrentQuery, allowMultipleUUIDCorrection);
            }
        }
    }

    /**
     * TagType Criteria
     */
    public static class TagTypeCriteria extends Criteria {
        private SeqStoreDAO.TagType tagType;

        public TagTypeCriteria(ReferenceType subject, String orgId, String path, String value, String addressId, Range range, SeqStoreDAO.TagType tagType, boolean allowConcurrentQuery, boolean allowMultipleUUIDCorrection) {
            super(subject, orgId, path, value, addressId, range, allowConcurrentQuery, allowMultipleUUIDCorrection);
            this.tagType = tagType;
        }

        public SeqStoreDAO.TagType getTagType() {
            return tagType;
        }

        public static class Builder {
            private SeqStoreDAO.TagType tagType;

            private ReferenceType subject;
            private String orgId;
            private String path;
            private String value;
            private Range range = Range.DEFAULT_RANGE;
            private boolean allowMultipleUUIDCorrection = true;

            private String addressId;

            private boolean allowConcurrentQuery = false;

            public Builder setAddressId(String addressId) {
                this.addressId = addressId;
                return this;
            }

            public Builder setPath(String path) {
                this.path = path;
                return this;
            }


            public Builder setSubject(ReferenceType subject) {
                this.subject = subject;
                return this;
            }

            public Builder setOrgId(String orgId) {
                this.orgId = orgId;
                return this;
            }

            public Builder setRange(Range range) {
                this.range = range;
                return this;
            }

            public Builder setTagType(SeqStoreDAO.TagType tagType) {
                this.tagType = tagType;
                return this;
            }

            public Builder setValue(String value) {
                this.value = value;
                return this;
            }

            public Builder setAllowMultipleUUIDCorrection(boolean allowMultipleUUIDCorrection)
            {
                this.allowMultipleUUIDCorrection = allowMultipleUUIDCorrection;
                return this;
            }

            public Builder setAllowConcurrentQuery(boolean allowConcurrentQuery)
            {
                this.allowConcurrentQuery = allowConcurrentQuery;
                return this;
            }

            public TagTypeCriteria build() {
                return new TagTypeCriteria(subject, orgId, path, value, addressId, range, tagType, allowConcurrentQuery, allowMultipleUUIDCorrection);
            }
        }
    }
}
