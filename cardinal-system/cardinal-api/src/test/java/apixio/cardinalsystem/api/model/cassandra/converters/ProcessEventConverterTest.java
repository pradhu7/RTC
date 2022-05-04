package apixio.cardinalsystem.api.model.cassandra.converters;

import apixio.cardinalsystem.api.model.cassandra.processevent.ProcessEventById;
import apixio.cardinalsystem.api.model.cassandra.processevent.ProcessEventByOrg;
import apixio.cardinalsystem.api.model.cassandra.processevent.ProcessEventFromXuuid;
import apixio.cardinalsystem.api.model.cassandra.processevent.ProcessEventToXuuid;
import apixio.cardinalsystem.test.TestUtils;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.EventCase;
import junit.framework.TestCase;

import java.util.Objects;

public class ProcessEventConverterTest extends TestCase {

    public TrackingEventRecord getRandomProcessEvent() {
        return TestUtils.generateRandomTrackingEvent(EventCase.PROCESS_EVENT, null, null);
    }

    public void testGetProcessEventById() {
        ProcessEventById processEventById = new ProcessEventConverter(getRandomProcessEvent()).getProcessEventById();
        assertTrue(processEventById instanceof ProcessEventById);
        assertTrue(Objects.nonNull(processEventById.getOrgXuuid()));
        assertTrue(Objects.nonNull(processEventById.getEventType()));
        assertTrue(Objects.nonNull(processEventById.getEventMetadata()));
        assertTrue(Objects.nonNull(processEventById.getEventSubtype()));
        assertTrue(Objects.nonNull(processEventById.getEventXuuid()));
        assertTrue(Objects.nonNull(processEventById.getCodeVersion()));
        assertTrue(Objects.nonNull(processEventById.getExecuteDuration()));
        assertTrue(Objects.nonNull(processEventById.getExecuteHost()));
        assertTrue(Objects.nonNull(processEventById.getStatus()));
        assertTrue(Objects.nonNull(processEventById.getStatusDetailed()));
        assertTrue(Objects.nonNull(processEventById.getFromXuuid()));
        assertTrue(Objects.nonNull(processEventById.getToXuuid()));
        assertTrue(Objects.nonNull(processEventById.getLastModifiedTimestamp()));
    }

    public void testGetProcessEventByOrg() {
        ProcessEventByOrg processEventByOrg = new ProcessEventConverter(getRandomProcessEvent()).getProcessEventByOrg();
        assertTrue(processEventByOrg instanceof ProcessEventByOrg);
        assertTrue(Objects.nonNull(processEventByOrg.getOrgXuuid()));
        assertTrue(Objects.nonNull(processEventByOrg.getPartitionId()));
        assertTrue(Objects.nonNull(processEventByOrg.getLastModifiedTimestamp()));
        assertTrue(Objects.nonNull(processEventByOrg.getEventType()));
        assertTrue(Objects.nonNull(processEventByOrg.getState()));
        assertTrue(Objects.nonNull(processEventByOrg.getEventXuuid()));
    }

    public void testGetProcessEventFromXuuid() {
        ProcessEventFromXuuid processEventFromXuuid = new ProcessEventConverter(getRandomProcessEvent()).getProcessEventFromXuuid();
        assertTrue(processEventFromXuuid instanceof ProcessEventFromXuuid);
        assertTrue(Objects.nonNull(processEventFromXuuid.getEventXuuid()));
        assertTrue(Objects.nonNull(processEventFromXuuid.getTimestamp()));
        assertTrue(Objects.nonNull(processEventFromXuuid.getFromXuuid()));
        assertTrue(Objects.nonNull(processEventFromXuuid.getToXuuid()));
    }

    public void testGetProcessEventToXuuid() {
        ProcessEventToXuuid processEventToXuuid = new ProcessEventConverter(getRandomProcessEvent()).getProcessEventToXuuid();
        assertTrue(processEventToXuuid instanceof ProcessEventToXuuid);
        assertTrue(Objects.nonNull(processEventToXuuid.getEventXuuid()));
        assertTrue(Objects.nonNull(processEventToXuuid.getTimestamp()));
        assertTrue(Objects.nonNull(processEventToXuuid.getFromXuuid()));
        assertTrue(Objects.nonNull(processEventToXuuid.getToXuuid()));
    }
}