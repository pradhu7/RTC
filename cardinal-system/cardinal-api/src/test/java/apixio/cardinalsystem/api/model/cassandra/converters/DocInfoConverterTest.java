package apixio.cardinalsystem.api.model.cassandra.converters;

import apixio.cardinalsystem.api.model.cassandra.docinfo.DocInfoByChecksum;
import apixio.cardinalsystem.api.model.cassandra.docinfo.DocInfoById;
import apixio.cardinalsystem.api.model.cassandra.docinfo.DocInfoByOrg;
import apixio.cardinalsystem.api.model.cassandra.docinfo.DocInfoByOrgPatientId;
import apixio.cardinalsystem.api.model.cassandra.docinfo.DocInfoByOrgPds;
import apixio.cardinalsystem.test.TestUtils;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.Contents.DataCase;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.EventCase;
import junit.framework.TestCase;

import java.util.Objects;

public class DocInfoConverterTest extends TestCase {

    public TrackingEventRecord getRandomDocinfoEvent() {
        return TestUtils.generateRandomTrackingEvent(EventCase.PROCESS_EVENT, DataCase.DOC_INFO, null);
    }

    public void testGetDocInfoByChecksum() {
        DocInfoByChecksum docInfoByChecksum = new DocInfoConverter(getRandomDocinfoEvent()).getDocInfoByChecksum();
        assertTrue(docInfoByChecksum instanceof DocInfoByChecksum);
        assertTrue(Objects.nonNull(docInfoByChecksum.getDocChecksum()));
        assertTrue(Objects.nonNull(docInfoByChecksum.getDocinfoXuuid()));
        assertTrue(Objects.nonNull(docInfoByChecksum.getLastModifiedTimestamp()));
    }

    public void testGetDocInfoById() {
        DocInfoById docInfoById = new DocInfoConverter(getRandomDocinfoEvent()).getDocInfoById();
        assertTrue(docInfoById instanceof DocInfoById);
        assertTrue(Objects.nonNull(docInfoById.getDocChecksum()));
        assertTrue(Objects.nonNull(docInfoById.getDocinfoXuuid()));
        assertTrue(Objects.nonNull(docInfoById.getContentType()));
        assertTrue(Objects.nonNull(docInfoById.getOrgXuuid()));
        assertTrue(Objects.nonNull(docInfoById.getPatientId()));
        assertTrue(Objects.nonNull(docInfoById.getPdsXuuid()));
        assertTrue(Objects.nonNull(docInfoById.getLastModifiedTimestamp()));
    }

    public void testGetDocInfoByOrg() {
        DocInfoByOrg docInfoByOrg = new DocInfoConverter(getRandomDocinfoEvent()).getDocInfoByOrg();
        assertTrue(docInfoByOrg instanceof DocInfoByOrg);
        assertTrue(Objects.nonNull(docInfoByOrg.getDocinfoXuuid()));
        assertTrue(Objects.nonNull(docInfoByOrg.getOrgXuuid()));
        assertTrue(Objects.nonNull(docInfoByOrg.getPartitionId()));
        assertTrue(Objects.nonNull(docInfoByOrg.getLastModifiedTimestamp()));
    }

    public void testGetDocInfoByOrgPatientId() {
        DocInfoByOrgPatientId docInfoByOrgPatientId = new DocInfoConverter(getRandomDocinfoEvent()).getDocInfoByOrgPatientId();
        assertTrue(docInfoByOrgPatientId instanceof DocInfoByOrgPatientId);
        assertTrue(Objects.nonNull(docInfoByOrgPatientId.getDocinfoXuuid()));
        assertTrue(Objects.nonNull(docInfoByOrgPatientId.getOrgXuuid()));
        assertTrue(Objects.nonNull(docInfoByOrgPatientId.getPartitionId()));
        assertTrue(Objects.nonNull(docInfoByOrgPatientId.getLastModifiedTimestamp()));
        assertTrue(Objects.nonNull(docInfoByOrgPatientId.getPatientId()));
    }

    public void testGetDocInfoByOrgPds() {
        DocInfoByOrgPds docInfoByOrgPds = new DocInfoConverter(getRandomDocinfoEvent()).getDocInfoByOrgPds();
        assertTrue(docInfoByOrgPds instanceof DocInfoByOrgPds);
        assertTrue(Objects.nonNull(docInfoByOrgPds.getDocinfoXuuid()));
        assertTrue(Objects.nonNull(docInfoByOrgPds.getOrgXuuid()));
        assertTrue(Objects.nonNull(docInfoByOrgPds.getPartitionId()));
        assertTrue(Objects.nonNull(docInfoByOrgPds.getLastModifiedTimestamp()));
        assertTrue(Objects.nonNull(docInfoByOrgPds.getPdsId()));
    }
}