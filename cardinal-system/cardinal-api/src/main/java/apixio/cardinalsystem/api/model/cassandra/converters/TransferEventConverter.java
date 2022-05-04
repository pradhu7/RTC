package apixio.cardinalsystem.api.model.cassandra.converters;

import apixio.cardinalsystem.api.model.CardinalModelBase;
import apixio.cardinalsystem.api.model.cassandra.transferevent.TransferEventByContentXuuid;
import apixio.cardinalsystem.api.model.cassandra.transferevent.TransferEventById;
import apixio.cardinalsystem.api.model.cassandra.transferevent.TransferEventByOrg;
import apixio.cardinalsystem.api.model.cassandra.transferevent.TransferEventFromXuuid;
import apixio.cardinalsystem.api.model.cassandra.transferevent.TransferEventToXuuid;
import com.apixio.messages.MessageMetadata.XUUID;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.TransferEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

public class TransferEventConverter {
    private TransferEvent event;
    private XUUID orgXuuid;
    private Long timestamp;
    private Map<String, Integer> orgPartitionsMap;

    public TransferEventConverter(TrackingEventRecord trackingEventRecord) {
        this(trackingEventRecord, new LinkedHashMap<>());
    }

    public TransferEventConverter(TrackingEventRecord trackingEventRecord, final Map<String, Integer> orgPartitionsMap) {
        this.event = trackingEventRecord.getTransferEvent();
        this.orgXuuid = trackingEventRecord.getOrgXuuid();
        this.timestamp = trackingEventRecord.getEventTimestamp();
        this.orgPartitionsMap = orgPartitionsMap;
    }

    public TransferEventById getTransferEventById() {
        TransferEventById transferEventById = new TransferEventById();
        transferEventById.setEventMetadata(getMetadata());
        transferEventById.setFromXuuid(CardinalModelBase.getXUUIDString(event.getInitiatorXuuid()));
        transferEventById.setToXuuid(CardinalModelBase.getXUUIDString(event.getReceiverXuuid()));
        transferEventById.setEventXuuid(CardinalModelBase.getXUUIDString(event.getXuuid()));
        transferEventById.setStatusDetailed(event.getState().getDetails());
        transferEventById.setStatus(event.getState().getStatus().name());
        transferEventById.setStatusDetailed(event.getState().getDetails());
        transferEventById.setOrgXuuid(CardinalModelBase.getXUUIDString(orgXuuid));
        transferEventById.setContentXuuid(CardinalModelBase.getXUUIDString(event.getSubject().getXuuid()));
        transferEventById.setLastModifiedTimestamp(timestamp);
        return transferEventById;
    }

    public TransferEventByOrg getTransferEventByOrg() {
        TransferEventByOrg transferEventByOrg = new TransferEventByOrg();
        transferEventByOrg.setEventXuuid(CardinalModelBase.getXUUIDString(event.getXuuid()));
        transferEventByOrg.setState(event.getState().getStatus().name());
        transferEventByOrg.setOrgXuuid(CardinalModelBase.getXUUIDString(orgXuuid));
        transferEventByOrg.setLastModifiedTimestamp(timestamp);
        if (orgPartitionsMap.containsKey(transferEventByOrg.getOrgXuuid())) {
            transferEventByOrg.setPartitionId(orgPartitionsMap.get(transferEventByOrg.getOrgXuuid()));
        }

        return transferEventByOrg;
    }

    public TransferEventFromXuuid getTransferEventFromXuuid() {
        TransferEventFromXuuid transferEventFromXuuid = new TransferEventFromXuuid();
        transferEventFromXuuid.setFromXuuid(CardinalModelBase.getXUUIDString(event.getInitiatorXuuid()));
        transferEventFromXuuid.setToXuuid(CardinalModelBase.getXUUIDString(event.getReceiverXuuid()));
        transferEventFromXuuid.setEventXuuid(CardinalModelBase.getXUUIDString(event.getXuuid()));
        transferEventFromXuuid.setTimestamp(timestamp);
        return transferEventFromXuuid;
    }

    public TransferEventToXuuid getTransferEventToXuuid() {
        TransferEventToXuuid transferEventToXuuid = new TransferEventToXuuid();
        transferEventToXuuid.setFromXuuid(CardinalModelBase.getXUUIDString(event.getInitiatorXuuid()));
        transferEventToXuuid.setToXuuid(CardinalModelBase.getXUUIDString(event.getReceiverXuuid()));
        transferEventToXuuid.setEventXuuid(CardinalModelBase.getXUUIDString(event.getXuuid()));
        transferEventToXuuid.setTimestamp(timestamp);
        return transferEventToXuuid;
    }

    public TransferEventByContentXuuid getTransferEventByContentXuuid() {
        TransferEventByContentXuuid transferEventByContentXuuid = new TransferEventByContentXuuid();
        transferEventByContentXuuid.setContentXuuid(CardinalModelBase.getXUUIDString(event.getSubject().getXuuid()));
        transferEventByContentXuuid.setFromXuuid(CardinalModelBase.getXUUIDString(event.getInitiatorXuuid()));
        transferEventByContentXuuid.setToXuuid(CardinalModelBase.getXUUIDString(event.getReceiverXuuid()));
        transferEventByContentXuuid.setEventXuuid(CardinalModelBase.getXUUIDString(event.getXuuid()));
        transferEventByContentXuuid.setTimestamp(timestamp);
        return transferEventByContentXuuid;
    }

    private Map<String, String> getMetadata() {
        ObjectMapper objectMapper = CardinalModelBase.objectMapper();
        return objectMapper.convertValue(event.getMetadata(), new TypeReference<Map<String, String>>() {
        });
    }
}
