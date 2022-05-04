package apixio.cardinalsystem.api.model.cassandra.converters;

import apixio.cardinalsystem.api.model.CardinalModelBase;
import apixio.cardinalsystem.api.model.cassandra.docinfo.DocInfoByChecksum;
import apixio.cardinalsystem.api.model.cassandra.docinfo.DocInfoById;
import apixio.cardinalsystem.api.model.cassandra.docinfo.DocInfoByOrg;
import apixio.cardinalsystem.api.model.cassandra.docinfo.DocInfoByOrgPatientId;
import apixio.cardinalsystem.api.model.cassandra.docinfo.DocInfoByOrgPds;
import com.apixio.messages.MessageMetadata.XUUID;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.DocumentInformation;

import java.util.LinkedHashMap;
import java.util.Map;

public class DocInfoConverter {
    private DocumentInformation event;
    private XUUID orgXuuid;
    private XUUID xuuid;
    private Long timestamp;
    private Map<String, Integer> orgPartitionsMap;

    public DocInfoConverter(TrackingEventRecord trackingEventRecord) {
        this(trackingEventRecord, new LinkedHashMap<>());
    }

    public DocInfoConverter(TrackingEventRecord trackingEventRecord, final Map<String, Integer> orgPartitionsMap) {
        switch (trackingEventRecord.getEventCase()) {
            case PROCESS_EVENT:
                this.event = trackingEventRecord.getProcessEvent().getSubject().getDocInfo();
                this.xuuid = trackingEventRecord.getProcessEvent().getSubject().getXuuid();
                break;
            case TRANSFER_EVENT:
                this.event = trackingEventRecord.getTransferEvent().getSubject().getDocInfo();
                this.xuuid = trackingEventRecord.getTransferEvent().getSubject().getXuuid();
                break;
            default:
                this.event = DocumentInformation.getDefaultInstance();
                break;
        }
        this.orgXuuid = trackingEventRecord.getOrgXuuid();
        this.timestamp = trackingEventRecord.getEventTimestamp();
        this.orgPartitionsMap = orgPartitionsMap;
    }

    public DocInfoByChecksum getDocInfoByChecksum() {
        DocInfoByChecksum docInfoByChecksum = new DocInfoByChecksum();
        docInfoByChecksum.setDocChecksum(event.getSha512());
        docInfoByChecksum.setDocinfoXuuid(CardinalModelBase.getXUUIDString(xuuid));
        docInfoByChecksum.setLastModifiedTimestamp(timestamp);
        return docInfoByChecksum;
    }

    public DocInfoById getDocInfoById() {
        DocInfoById docInfoById = new DocInfoById();
        docInfoById.setPatientId(event.getPatientId());
        docInfoById.setDocChecksum(event.getSha512());
        docInfoById.setDocinfoXuuid(CardinalModelBase.getXUUIDString(xuuid));
        docInfoById.setPdsXuuid(event.getPdsId());
        docInfoById.setContentType(event.getContentType());
        docInfoById.setOrgXuuid(CardinalModelBase.getXUUIDString(orgXuuid));
        docInfoById.setLastModifiedTimestamp(timestamp);
        return docInfoById;
    }

    public DocInfoByOrg getDocInfoByOrg() {
        DocInfoByOrg docInfoByOrg = new DocInfoByOrg();
        docInfoByOrg.setDocinfoXuuid(CardinalModelBase.getXUUIDString(xuuid));
        docInfoByOrg.setOrgXuuid(CardinalModelBase.getXUUIDString(orgXuuid));
        docInfoByOrg.setLastModifiedTimestamp(timestamp);
        if (orgPartitionsMap.containsKey(docInfoByOrg.getOrgXuuid())) {
            docInfoByOrg.setPartitionId(orgPartitionsMap.get(docInfoByOrg.getOrgXuuid()));
        }
        return docInfoByOrg;
    }

    public DocInfoByOrgPatientId getDocInfoByOrgPatientId() {
        DocInfoByOrgPatientId docInfoByOrgPatientId = new DocInfoByOrgPatientId();
        docInfoByOrgPatientId.setPatientId(event.getPatientId());
        docInfoByOrgPatientId.setOrgXuuid(CardinalModelBase.getXUUIDString(orgXuuid));
        docInfoByOrgPatientId.setDocinfoXuuid(CardinalModelBase.getXUUIDString(orgXuuid));
        docInfoByOrgPatientId.setLastModifiedTimestamp(timestamp);
        if (orgPartitionsMap.containsKey(docInfoByOrgPatientId.getOrgXuuid())) {
            docInfoByOrgPatientId.setPartitionId(orgPartitionsMap.get(docInfoByOrgPatientId.getOrgXuuid()));
        }
        return docInfoByOrgPatientId;
    }

    public DocInfoByOrgPds getDocInfoByOrgPds() {
        DocInfoByOrgPds docInfoByOrgPds = new DocInfoByOrgPds();
        docInfoByOrgPds.setPdsId(event.getPdsId());
        docInfoByOrgPds.setOrgXuuid(CardinalModelBase.getXUUIDString(orgXuuid));
        docInfoByOrgPds.setDocinfoXuuid(CardinalModelBase.getXUUIDString(orgXuuid));
        docInfoByOrgPds.setLastModifiedTimestamp(timestamp);
        if (orgPartitionsMap.containsKey(docInfoByOrgPds.getOrgXuuid())) {
            docInfoByOrgPds.setPartitionId(orgPartitionsMap.get(docInfoByOrgPds.getOrgXuuid()));
        }
        return docInfoByOrgPds;
    }
}
