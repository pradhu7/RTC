package apixio.cardinalsystem.api.model.cassandra.converters;

import apixio.cardinalsystem.api.model.CardinalModelBase;
import apixio.cardinalsystem.api.model.cassandra.processevent.ProcessEventById;
import apixio.cardinalsystem.api.model.cassandra.processevent.ProcessEventByOrg;
import apixio.cardinalsystem.api.model.cassandra.processevent.ProcessEventFromXuuid;
import apixio.cardinalsystem.api.model.cassandra.processevent.ProcessEventToXuuid;
import com.apixio.messages.MessageMetadata.XUUID;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.ProcessEvent;
import com.google.protobuf.Descriptors.FieldDescriptor;

import java.util.LinkedHashMap;
import java.util.Map;

public class ProcessEventConverter {
    private ProcessEvent event;
    private XUUID orgXuuid;
    private Long timestamp;
    private Map<String, Integer> orgPartitionsMap;

    public ProcessEventConverter(TrackingEventRecord trackingEventRecord) {
        this(trackingEventRecord, new LinkedHashMap<>());
    }

    public ProcessEventConverter(TrackingEventRecord trackingEventRecord, final Map<String, Integer> orgPartitionsMap) {
        this.event = trackingEventRecord.getProcessEvent();
        this.orgXuuid = trackingEventRecord.getOrgXuuid();
        this.timestamp = trackingEventRecord.getEventTimestamp();
        this.orgPartitionsMap = orgPartitionsMap;
    }

    public ProcessEventById getProcessEventById() {
        ProcessEventById processEvent = new ProcessEventById();
        processEvent.setEventMetadata(getMetadata());
        processEvent.setFromXuuid(CardinalModelBase.getXUUIDString(event.getFromXuuid()));
        processEvent.setToXuuid(CardinalModelBase.getXUUIDString(event.getToXuuid()));
        processEvent.setCodeVersion(event.getCodeVersion());
        processEvent.setEventType(event.getDataCase().name());
        processEvent.setExecuteDuration(event.getExecuteDuration());
        processEvent.setExecuteHost(event.getExecuteHost());
        processEvent.setStatusDetailed(event.getState().getDetails());
        processEvent.setStatus(event.getState().getStatus().name());
        processEvent.setEventSubtype(getSubtype());
        processEvent.setEventXuuid(CardinalModelBase.getXUUIDString(event.getXuuid()));
        processEvent.setOrgXuuid(CardinalModelBase.getXUUIDString(orgXuuid));
        processEvent.setLastModifiedTimestamp(timestamp);
        return processEvent;
    }

    public ProcessEventByOrg getProcessEventByOrg() {
        ProcessEventByOrg processEventByOrg = new ProcessEventByOrg();
        processEventByOrg.setEventType(event.getDataCase().name());
        processEventByOrg.setEventXuuid(CardinalModelBase.getXUUIDString(event.getXuuid()));
        processEventByOrg.setState(event.getState().getStatus().name());
        processEventByOrg.setOrgXuuid(CardinalModelBase.getXUUIDString(orgXuuid));
        processEventByOrg.setLastModifiedTimestamp(timestamp);
        if (orgPartitionsMap.containsKey(processEventByOrg.getOrgXuuid())) {
            processEventByOrg.setPartitionId(orgPartitionsMap.get(processEventByOrg.getOrgXuuid()));
        }

        return processEventByOrg;
    }

    public ProcessEventFromXuuid getProcessEventFromXuuid() {
        ProcessEventFromXuuid processEventFromXuuid = new ProcessEventFromXuuid();

        processEventFromXuuid.setFromXuuid(CardinalModelBase.getXUUIDString(event.getFromXuuid()));
        processEventFromXuuid.setToXuuid(CardinalModelBase.getXUUIDString(event.getToXuuid()));
        processEventFromXuuid.setEventXuuid(CardinalModelBase.getXUUIDString(event.getXuuid()));
        processEventFromXuuid.setTimestamp(timestamp);

        return processEventFromXuuid;
    }

    public ProcessEventToXuuid getProcessEventToXuuid() {
        ProcessEventToXuuid processEventToXuuid = new ProcessEventToXuuid();

        processEventToXuuid.setFromXuuid(CardinalModelBase.getXUUIDString(event.getFromXuuid()));
        processEventToXuuid.setToXuuid(CardinalModelBase.getXUUIDString(event.getToXuuid()));
        processEventToXuuid.setEventXuuid(CardinalModelBase.getXUUIDString(event.getXuuid()));
        processEventToXuuid.setTimestamp(timestamp);
        return processEventToXuuid;
    }

    private Map<String, String> getMetadata() {
        switch (event.getDataCase()) {
            case OPPORTUNITY_EVENT:
                return convertMetadata(event.getOpportunityEvent().getMetadata().getAllFields());
            case PREDICTION_EVENT:
                return convertMetadata(event.getPredictionEvent().getMetadata().getAllFields());
            case LOADER_EVENT:
                return convertMetadata(event.getLoaderEvent().getMetadata().getAllFields());
            case ETL_EVENT:
                return convertMetadata(event.getEtlEvent().getMetadata().getAllFields());
            case CDI_EVENT:
                return convertMetadata(event.getCdiEvent().getMetadata().getAllFields());
            default:
                return new LinkedHashMap<>();
        }
    }

    private Map<String, String> convertMetadata(Map<FieldDescriptor, Object> metadataFields) {
        Map<String, String> convertedMetadata = new LinkedHashMap<>();
        for (Map.Entry<FieldDescriptor, Object> field : metadataFields.entrySet()) {
            if (field.getValue().getClass().equals(XUUID.class)) {
                convertedMetadata.put(
                        field.getKey().getName().toLowerCase(),
                        CardinalModelBase.getXUUIDString((XUUID) field.getValue())
                );
            } else if (field.getValue().getClass().equals(String.class)) {
                convertedMetadata.put(
                        field.getKey().getName().toLowerCase(),
                        (String) field.getValue()
                );
            } else {
                throw new RuntimeException(String.format("Unknown message type %s. Unable to convert to string", field.getValue().getClass()));
            }
        }
        return convertedMetadata;
    }

    private String getSubtype() {
        switch (event.getDataCase()) {
            case OPPORTUNITY_EVENT:
                return event.getOpportunityEvent().getSubtype().name();
            case PREDICTION_EVENT:
                return event.getPredictionEvent().getSubtype().name();
            case LOADER_EVENT:
                return event.getLoaderEvent().getSubtype().name();
            case ETL_EVENT:
                return event.getEtlEvent().getSubtype().name();
            case CDI_EVENT:
                return event.getCdiEvent().getSubtype().name();
            default:
                return "NONE";
        }
    }
}
