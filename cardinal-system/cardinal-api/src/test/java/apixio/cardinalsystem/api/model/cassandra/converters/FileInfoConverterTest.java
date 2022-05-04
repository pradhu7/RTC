package apixio.cardinalsystem.api.model.cassandra.converters;

import apixio.cardinalsystem.api.model.cassandra.fileinfo.FileInfoByChecksum;
import apixio.cardinalsystem.api.model.cassandra.fileinfo.FileInfoById;
import apixio.cardinalsystem.api.model.cassandra.fileinfo.FileInfoByOrg;
import apixio.cardinalsystem.test.TestUtils;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.Contents.DataCase;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.EventCase;
import junit.framework.TestCase;

import java.util.Objects;

public class FileInfoConverterTest extends TestCase {

    public TrackingEventRecord getRandomFileinfoEvent() {
        return TestUtils.generateRandomTrackingEvent(EventCase.PROCESS_EVENT, DataCase.FILE_INFO, null);
    }

    public void testGetFileInfoByChecksum() {
        FileInfoByChecksum fileInfoByChecksum = new FileInfoConverter(getRandomFileinfoEvent()).getFileInfoByChecksum();
        assertTrue(fileInfoByChecksum instanceof FileInfoByChecksum);
        assertTrue(Objects.nonNull(fileInfoByChecksum.getFileChecksum()));
        assertTrue(Objects.nonNull(fileInfoByChecksum.getFileinfoXuuid()));
        assertTrue(Objects.nonNull(fileInfoByChecksum.getLastModifiedTimestamp()));
    }

    public void testGetFileInfoById() {
        FileInfoById fileInfoById = new FileInfoConverter(getRandomFileinfoEvent()).getFileInfoById();
        assertTrue(fileInfoById instanceof FileInfoById);
        assertTrue(Objects.nonNull(fileInfoById.getFilePath()));
        assertTrue(Objects.nonNull(fileInfoById.getOrgXuuid()));
        assertTrue(Objects.nonNull(fileInfoById.getFileSize()));
        assertTrue(Objects.nonNull(fileInfoById.getLastModifiedTimestamp()));
        assertTrue(Objects.nonNull(fileInfoById.getFileChecksum()));
        assertTrue(Objects.nonNull(fileInfoById.getFileinfoXuuid()));
        assertTrue(Objects.nonNull(fileInfoById.getMimeType()));
        assertTrue(Objects.nonNull(fileInfoById.getIsArchive()));
    }

    public void testGetFileInfoByOrg() {
        FileInfoByOrg fileInfoByOrg = new FileInfoConverter(getRandomFileinfoEvent()).getFileInfoByOrg();
        assertTrue(fileInfoByOrg instanceof FileInfoByOrg);
        assertTrue(Objects.nonNull(fileInfoByOrg.getOrgXuuid()));
        assertTrue(Objects.nonNull(fileInfoByOrg.getPartitionId()));
        assertTrue(Objects.nonNull(fileInfoByOrg.getLastModifiedTimestamp()));
        assertTrue(Objects.nonNull(fileInfoByOrg.getFileinfoXuuid()));
    }
}