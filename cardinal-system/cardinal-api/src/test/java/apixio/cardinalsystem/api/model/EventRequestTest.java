package apixio.cardinalsystem.api.model;

import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.EventCase;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.ProcessEvent.DataCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.testing.ResourceHelpers;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class EventRequestTest extends TestCase {
    @Test
    public void testMapper() throws IOException {
        String resourcePath = ResourceHelpers.resourceFilePath("fixtures/event_example_process_event.json");
        File resourceFile = new File(resourcePath);
        String resourceContents = String.join("\n", Files.readAllLines(resourceFile.toPath()));
        ObjectMapper objectMapper = CardinalModelBase.objectMapper();
        EventRequest test = objectMapper.readValue(resourceContents, EventRequest.class);
        String testJson = objectMapper.writeValueAsString(test.getItems().get(0));
        TrackingEventRecord trackingEventRecord = objectMapper.readValue(testJson, TrackingEventRecord.class);
        assertTrue(trackingEventRecord.getEventCase().equals(EventCase.PROCESS_EVENT));
        assertTrue(trackingEventRecord.getProcessEvent().getDataCase().equals(DataCase.CDI_EVENT));
        assertTrue(trackingEventRecord.getEventTimestamp() > 0);
        //protobufMapper.writer(TrackingEventRecord.)
        //TrackingEventRecord.Builder builder = TrackingEventRecord.newBuilder();
        //JsonFormat.parser().ignoringUnknownFields().merge(resourceContents, builder);
        //TrackingEventRecord event = builder.build();
        //System.out.println(event.toString());
    }
}