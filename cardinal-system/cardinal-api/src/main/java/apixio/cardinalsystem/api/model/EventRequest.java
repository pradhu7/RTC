package apixio.cardinalsystem.api.model;

import apixio.cardinalsystem.api.model.EventRequest.EventRequestBuilder;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;


@JsonDeserialize(builder = EventRequestBuilder.class)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EventRequest extends CardinalModelBase {
    // using this allows for much simpler validation of the basic schema and using all the features of protobuf available
    // to determine the type. Having this as a list allows for sending multiple events in batch.
    @JsonProperty("items")
    private List<TrackingEventRecord> items;

    public List<TrackingEventRecord> getItems() {
        return items;
    }

    public void setItems(List<TrackingEventRecord> items) {
        ArrayList<TrackingEventRecord> addItems = new ArrayList<>();
        for (TrackingEventRecord item : items) {
            addItems.add(validateRecord(item));
        }
        this.items = addItems;
    }

    @JsonIgnore
    public List<String> getEventTypes() {
        return items.stream().map(i -> i.getEventCase().toString()).distinct().collect(Collectors.toList());
    }

    @JsonIgnore
    public List<String> getEventIds() {
        return items.stream().map(item -> {
            String xuuid = "NONE";
            switch (item.getEventCase()) {
                case PROCESS_EVENT:
                    xuuid = CardinalModelBase.getXUUIDString(item.getProcessEvent().getXuuid());
                    break;
                case TRANSFER_EVENT:
                    xuuid = CardinalModelBase.getXUUIDString(item.getTransferEvent().getXuuid());
                    break;
                default:
                    Logger.getLogger(this.getClass().getName()).warning(String.format("Invalid event type found: %s", item.getEventCase()));
                    break;
            }
            return xuuid;
        }).collect(Collectors.toList());
    }


    public static final class EventRequestBuilder {
        // using this allows for much simpler validation of the basic schema and using all the features of protobuf available
        // to determine the type. Having this as a list allows for sending multiple events in batch.
        private List<TrackingEventRecord> items;

        private EventRequestBuilder() {
        }

        public static EventRequestBuilder anEventRequest() {
            return new EventRequestBuilder();
        }

        public EventRequestBuilder withItems(List<TrackingEventRecord> items) {
            this.items = items;
            return this;
        }

        public EventRequest build() {
            EventRequest eventRequest = new EventRequest();
            eventRequest.setItems(items);
            return eventRequest;
        }
    }
}
