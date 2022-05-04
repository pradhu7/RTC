package apixio.cardinalsystem.api.model.cassandra.converters;

import apixio.cardinalsystem.api.model.cassandra.transferevent.TransferEventByContentXuuid;
import apixio.cardinalsystem.api.model.cassandra.transferevent.TransferEventById;
import apixio.cardinalsystem.api.model.cassandra.transferevent.TransferEventByOrg;
import apixio.cardinalsystem.api.model.cassandra.transferevent.TransferEventFromXuuid;
import apixio.cardinalsystem.api.model.cassandra.transferevent.TransferEventToXuuid;
import apixio.cardinalsystem.test.TestUtils;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.EventCase;
import junit.framework.TestCase;

import java.util.Objects;

public class TransferEventConverterTest extends TestCase {

    public TrackingEventRecord getRandomTransferEvent() {
        return TestUtils.generateRandomTrackingEvent(EventCase.PROCESS_EVENT, null, null);
    }

    public void testGetTransferEventById() {
        TransferEventById transferEventById = new TransferEventConverter(getRandomTransferEvent()).getTransferEventById();
        assertTrue(transferEventById instanceof TransferEventById);
        assertTrue(Objects.nonNull(transferEventById.getOrgXuuid()));
        assertTrue(Objects.nonNull(transferEventById.getEventMetadata()));
        assertTrue(Objects.nonNull(transferEventById.getEventXuuid()));
        assertTrue(Objects.nonNull(transferEventById.getStatus()));
        assertTrue(Objects.nonNull(transferEventById.getStatusDetailed()));
        assertTrue(Objects.nonNull(transferEventById.getFromXuuid()));
        assertTrue(Objects.nonNull(transferEventById.getToXuuid()));
        assertTrue(Objects.nonNull(transferEventById.getContentXuuid()));
        assertTrue(Objects.nonNull(transferEventById.getLastModifiedTimestamp()));
    }

    public void testGetTransferEventByOrg() {
        TransferEventByOrg transferEventByOrg = new TransferEventConverter(getRandomTransferEvent()).getTransferEventByOrg();
        assertTrue(transferEventByOrg instanceof TransferEventByOrg);
        assertTrue(Objects.nonNull(transferEventByOrg.getOrgXuuid()));
        assertTrue(Objects.nonNull(transferEventByOrg.getPartitionId()));
        assertTrue(Objects.nonNull(transferEventByOrg.getLastModifiedTimestamp()));
        assertTrue(Objects.nonNull(transferEventByOrg.getState()));
        assertTrue(Objects.nonNull(transferEventByOrg.getEventXuuid()));
    }

    public void testGetTransferEventFromXuuid() {
        TransferEventFromXuuid transferEventFromXuuid = new TransferEventConverter(getRandomTransferEvent()).getTransferEventFromXuuid();
        assertTrue(transferEventFromXuuid instanceof TransferEventFromXuuid);
        assertTrue(Objects.nonNull(transferEventFromXuuid.getEventXuuid()));
        assertTrue(Objects.nonNull(transferEventFromXuuid.getTimestamp()));
        assertTrue(Objects.nonNull(transferEventFromXuuid.getFromXuuid()));
        assertTrue(Objects.nonNull(transferEventFromXuuid.getToXuuid()));
    }

    public void testGetTransferEventToXuuid() {
        TransferEventToXuuid transferEventToXuuid = new TransferEventConverter(getRandomTransferEvent()).getTransferEventToXuuid();
        assertTrue(transferEventToXuuid instanceof TransferEventToXuuid);
        assertTrue(Objects.nonNull(transferEventToXuuid.getEventXuuid()));
        assertTrue(Objects.nonNull(transferEventToXuuid.getTimestamp()));
        assertTrue(Objects.nonNull(transferEventToXuuid.getFromXuuid()));
        assertTrue(Objects.nonNull(transferEventToXuuid.getToXuuid()));
    }

    public void testGetTransferEventByContentXuuid() {
        TransferEventByContentXuuid transferEventByContentXuuid = new TransferEventConverter(getRandomTransferEvent()).getTransferEventByContentXuuid();
        assertTrue(transferEventByContentXuuid instanceof TransferEventByContentXuuid);
        assertTrue(Objects.nonNull(transferEventByContentXuuid.getContentXuuid()));
        assertTrue(Objects.nonNull(transferEventByContentXuuid.getEventXuuid()));
        assertTrue(Objects.nonNull(transferEventByContentXuuid.getFromXuuid()));
        assertTrue(Objects.nonNull(transferEventByContentXuuid.getToXuuid()));
        assertTrue(Objects.nonNull(transferEventByContentXuuid.getTimestamp()));
    }
}