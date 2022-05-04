package apixio.cardinalsystem.api.model.cassandra.converters;

import apixio.cardinalsystem.api.model.CardinalModelBase;
import apixio.cardinalsystem.api.model.cassandra.fileinfo.FileInfoByChecksum;
import apixio.cardinalsystem.api.model.cassandra.fileinfo.FileInfoById;
import apixio.cardinalsystem.api.model.cassandra.fileinfo.FileInfoByOrg;
import com.apixio.messages.MessageMetadata.XUUID;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.FileInformation;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

public class FileInfoConverter {
    private FileInformation event;
    private XUUID orgXuuid;
    private XUUID xuuid;
    private Long timestamp;
    private Map<String, Integer> orgPartitionsMap;

    public FileInfoConverter(TrackingEventRecord trackingEventRecord) {
        this(trackingEventRecord, new LinkedHashMap<>());
    }

    public FileInfoConverter(TrackingEventRecord trackingEventRecord, final Map<String, Integer> orgPartitionsMap) {
        switch (trackingEventRecord.getEventCase()) {
            case PROCESS_EVENT:
                this.event = trackingEventRecord.getProcessEvent().getSubject().getFileInfo();
                this.xuuid = trackingEventRecord.getProcessEvent().getSubject().getXuuid();
                break;
            case TRANSFER_EVENT:
                this.event = trackingEventRecord.getTransferEvent().getSubject().getFileInfo();
                this.xuuid = trackingEventRecord.getTransferEvent().getSubject().getXuuid();
                break;
            default:
                this.event = FileInformation.getDefaultInstance();
                break;
        }
        this.orgXuuid = trackingEventRecord.getOrgXuuid();
        this.timestamp = trackingEventRecord.getEventTimestamp();
        this.orgPartitionsMap = orgPartitionsMap;
    }

    public FileInfoByChecksum getFileInfoByChecksum() {
        FileInfoByChecksum fileInfoByChecksum = new FileInfoByChecksum();
        fileInfoByChecksum.setFileChecksum(event.getSha512());
        fileInfoByChecksum.setFileinfoXuuid(CardinalModelBase.getXUUIDString(xuuid));
        fileInfoByChecksum.setLastModifiedTimestamp(timestamp);
        return fileInfoByChecksum;
    }

    public FileInfoById getFileInfoById() {
        ObjectMapper objectMapper = CardinalModelBase.objectMapper();
        Map<String, String> filePathInfo = objectMapper.convertValue(event.getFilePath(), new TypeReference<Map<String, String>>() {
        });
        FileInfoById fileInfoById = new FileInfoById();
        fileInfoById.setFileinfoXuuid(CardinalModelBase.getXUUIDString(xuuid));
        fileInfoById.setFileChecksum(event.getSha512());
        fileInfoById.setIsArchive(event.getIsArchive());
        fileInfoById.setFileSize(event.getFileSize());
        fileInfoById.setMimeType(event.getMimeType());
        fileInfoById.setFilePath(filePathInfo);
        fileInfoById.setOrgXuuid(CardinalModelBase.getXUUIDString(orgXuuid));
        fileInfoById.setLastModifiedTimestamp(timestamp);
        return fileInfoById;
    }

    public FileInfoByOrg getFileInfoByOrg() {
        FileInfoByOrg fileInfoByOrg = new FileInfoByOrg();
        fileInfoByOrg.setFileinfoXuuid(CardinalModelBase.getXUUIDString(xuuid));
        fileInfoByOrg.setOrgXuuid(CardinalModelBase.getXUUIDString(orgXuuid));
        fileInfoByOrg.setLastModifiedTimestamp(timestamp);
        if (orgPartitionsMap.containsKey(fileInfoByOrg.getOrgXuuid())) {
            fileInfoByOrg.setPartitionId(orgPartitionsMap.get(fileInfoByOrg.getOrgXuuid()));
        }
        return fileInfoByOrg;
    }
}
