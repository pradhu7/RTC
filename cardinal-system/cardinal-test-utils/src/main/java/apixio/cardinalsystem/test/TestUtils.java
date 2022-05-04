package apixio.cardinalsystem.test;

import com.apixio.messages.MessageMetadata.XUUID;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.CDIEvent;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.CDIEventMetadata;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.Contents;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.Contents.DataCase;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.DocumentInformation;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.ETLEvent;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.ETLEventMetadata;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.EventCase;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.FileInformation;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.LoaderEvent;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.LoaderEventMetadata;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.OpportunityEvent;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.OpportunityEventMetadata;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.PathInfo;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.PredictionEvent;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.PredictionEventMetadata;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.ProcessEvent;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.State;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.TransferEvent;
import com.apixio.tracking.TrackingEventRecordOuterClass.TrackingEventRecord.TransferEventMetadata;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class TestUtils {
    public static TrackingEventRecord.EventCase generateRandomTrackingEventCase() {
        List<TrackingEventRecord.EventCase> contentTypes = new ArrayList<>(Arrays.asList(TrackingEventRecord.EventCase.values()));
        contentTypes.remove(EventCase.EVENT_NOT_SET);
        int randomIndex = ThreadLocalRandom.current().nextInt(0, contentTypes.size());
        return contentTypes.get(randomIndex);
    }

    public static TrackingEventRecord generateRandomTrackingEvent() {
        TrackingEventRecord.Builder builder = TrackingEventRecord.newBuilder();
        TrackingEventRecord.EventCase eventCase = generateRandomTrackingEventCase();
        switch (eventCase) {
            case TRANSFER_EVENT:
                builder.setTransferEvent(generateRandomTransferEvent());
                break;
            case PROCESS_EVENT:
                builder.setProcessEvent(generateRandomProcessEvent());
                break;
            default:
                throw new IllegalArgumentException(String.format("Must have valid event case for tracking event. %s", eventCase));
        }
        builder.setOrgXuuid(generateRandomXuuid("O"));
        builder.setEventTimestamp(Instant.now().toEpochMilli());
        return builder.build();
    }

    public static TrackingEventRecord generateRandomTrackingEvent(EventCase eventCase, DataCase contentsData, ProcessEvent.DataCase processDataCase) {
        TrackingEventRecord.Builder builder = TrackingEventRecord.newBuilder();
        if (Objects.isNull(eventCase) || eventCase.equals(EventCase.EVENT_NOT_SET)) {
            eventCase = generateRandomTrackingEventCase();
        }

        switch (eventCase) {
            case PROCESS_EVENT:
                ProcessEvent.Builder tmpPBuilder = generateRandomProcessEventBuilder();
                setRandomProcessEventEvent(tmpPBuilder, processDataCase);
                tmpPBuilder.setSubject(generateRandomContents(contentsData));
                builder.setProcessEvent(tmpPBuilder.build());
                break;
            case TRANSFER_EVENT:
                TransferEvent.Builder tmpTBuilder = generateRandomTransferEventBuilder();
                tmpTBuilder.setSubject(generateRandomContents(contentsData));
                builder.setTransferEvent(tmpTBuilder.build());
                break;
            default:
                throw new RuntimeException("This should never get hit");
        }
        builder.setOrgXuuid(generateRandomXuuid("O"));
        builder.setEventTimestamp(Instant.now().toEpochMilli());

        return builder.build();
    }

    public static TransferEvent.Builder generateRandomTransferEventBuilder() {
        TransferEvent.Builder builder = TransferEvent.newBuilder();
        builder.setState(generateRandomState());
        builder.setInitiatorXuuid(generateRandomXuuid("O"));
        builder.setReceiverXuuid(generateRandomXuuid("O"));
        builder.setSubject(generateRandomContents());
        builder.setXuuid(generateRandomXuuid("TRANSFEREVENT"));
        builder.setMetadata(
                TransferEventMetadata.newBuilder()
                        .setHost(randomString())
                        .setUser(randomString())
                        .setProtocol(randomString())
                        .build()
        );
        return builder;
    }

    public static TransferEvent generateRandomTransferEvent() {
        return generateRandomTransferEventBuilder().build();
    }

    public static ProcessEvent.Builder generateRandomProcessEventBuilder() {
        ProcessEvent.Builder builder = ProcessEvent.newBuilder();
        setRandomProcessEventEvent(builder);
        builder.setXuuid(generateRandomXuuid("PROCESSEVENT"));
        builder.setCodeVersion(randomString());
        builder.setExecuteDuration(ThreadLocalRandom.current().nextInt());
        builder.setExecuteHost(randomString());
        builder.setAttemptNumber(ThreadLocalRandom.current().nextInt());
        builder.setToXuuid(generateRandomXuuid("DOC"));
        builder.setFromXuuid(generateRandomXuuid("DOC"));
        builder.setState(generateRandomState());
        builder.setSubject(generateRandomContents());
        return builder;
    }

    public static ProcessEvent generateRandomProcessEvent() {
        return generateRandomProcessEventBuilder().build();
    }

    public static State generateRandomState() {
        List<Integer> statusNums = new ArrayList<>();

        State.getDefaultInstance().getStatus().getDescriptorForType().getValues()
                .stream()
                .forEach(v -> statusNums.add(v.getNumber()));
        int randomInt = ThreadLocalRandom.current().nextInt(0, statusNums.size());

        return State.newBuilder().setDetails(randomString()).setStatusValue(statusNums.get(randomInt)).build();
    }

    public static Contents.DataCase generateRandomContentsDataCase() {
        List<Contents.DataCase> contentTypes = new ArrayList<>(Arrays.asList(Contents.DataCase.values()));
        contentTypes.remove(DataCase.DATA_NOT_SET);
        int randomIndex = ThreadLocalRandom.current().nextInt(0, contentTypes.size());
        return contentTypes.get(randomIndex);

    }

    public static Contents generateRandomContents() {
        return generateRandomContents(generateRandomContentsDataCase());
    }

    public static Contents generateRandomContents(Contents.DataCase datacase) {
        Contents.Builder builder = Contents.newBuilder();

        if (Objects.isNull(datacase) || datacase.equals(DataCase.DATA_NOT_SET)) {
            datacase = generateRandomContentsDataCase();
        }

        String sha512;
        try {
            sha512 = String.format(
                    "%0128x",
                    new BigInteger(
                            1,
                            MessageDigest.getInstance("SHA-512").digest(RandomUtils.nextBytes(1024))
                    )
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("error getting sha512", e);
        }

        switch (datacase) {
            case FILE_INFO:
                builder.setFileInfo(
                        FileInformation.newBuilder()
                                .setMimeType(randomString())
                                .setIsArchive(RandomUtils.nextBoolean())
                                .setSha512(sha512)
                                .setFilePath(
                                        PathInfo.newBuilder()
                                                .setHost(randomString())
                                                .setScheme("s3")
                                                .setPath(String.format("/%s/%s", randomString(), randomString()))
                                                .build()
                                )
                                .setFileSize(RandomUtils.nextInt())
                                .build()
                );
                break;
            case DOC_INFO:
                builder.setDocInfo(
                        DocumentInformation.newBuilder()
                                .setSha512(sha512)
                                .setContentType(randomString())
                                .setPatientId(randomString())
                                .setPdsId(randomString())
                                .build()
                );
                break;
            default:
                throw new IllegalArgumentException(String.format("Must have a valid content datacase %s", datacase));
        }
        builder.setXuuid(generateRandomXuuid("FILE"));
        return builder.build();
    }

    public static ProcessEvent.DataCase getRandomProcessEventDataCase() {
        List<ProcessEvent.DataCase> eventTypes = new ArrayList<>(Arrays.asList(ProcessEvent.DataCase.values()));
        eventTypes.remove(ProcessEvent.DataCase.DATA_NOT_SET);
        int eventTypeIndex = ThreadLocalRandom.current().nextInt(0, eventTypes.size());
        return eventTypes.get(eventTypeIndex);
    }

    public static void setRandomProcessEventEvent(ProcessEvent.Builder builder) {
        setRandomProcessEventEvent(builder, getRandomProcessEventDataCase());
    }

    public static void setRandomProcessEventEvent(ProcessEvent.Builder builder, ProcessEvent.DataCase datacase) {
        if (Objects.isNull(datacase) || datacase.equals(ProcessEvent.DataCase.DATA_NOT_SET)) {
            datacase = getRandomProcessEventDataCase();
        }
        switch (datacase) {
            case CDI_EVENT:
                builder.setCdiEvent(generateRandomCDIEvent());
                break;
            case ETL_EVENT:
                builder.setEtlEvent(generateRandomETLEvent());
                break;
            case LOADER_EVENT:
                builder.setLoaderEvent(generateRandomLoaderEvent());
                break;
            case PREDICTION_EVENT:
                builder.setPredictionEvent(generateRandomPredictionEvent());
                break;
            case OPPORTUNITY_EVENT:
                builder.setOpportunityEvent(generateRandomOppotunityEvent());
                break;
            default:
                throw new IllegalArgumentException(String.format("Bad random event generated!!!. %s", datacase.toString()));
        }

    }

    public static CDIEvent generateRandomCDIEvent() {
        CDIEvent.Builder builder = CDIEvent.newBuilder();
        builder.setSubtypeValue(getRandomSubtypes(ProcessEvent.DataCase.CDI_EVENT));
        CDIEventMetadata randomMetadata = CDIEventMetadata.newBuilder()
                .setCommand(String.format("test command %s", randomString()))
                .setUser(randomString()).build();
        builder.setMetadata(randomMetadata);
        return builder.build();
    }

    public static OpportunityEvent generateRandomOppotunityEvent() {
        OpportunityEvent.Builder builder = OpportunityEvent.newBuilder();
        builder.setSubtypeValue(getRandomSubtypes(ProcessEvent.DataCase.OPPORTUNITY_EVENT));
        OpportunityEventMetadata randomMetadata = OpportunityEventMetadata.newBuilder()
                .setUser(RandomStringUtils.randomAlphanumeric(10))
                .setExecuteHost(String.format("%s.%s", randomString(), randomString()))
                .setWorkItemId(randomString())
                .setWorkUnitId(randomString())
                .build();
        builder.setMetadata(randomMetadata);
        return builder.build();
    }

    public static PredictionEvent generateRandomPredictionEvent() {
        PredictionEvent.Builder builder = PredictionEvent.newBuilder();
        builder.setSubtypeValue(getRandomSubtypes(ProcessEvent.DataCase.OPPORTUNITY_EVENT));
        PredictionEventMetadata randomMetadata = PredictionEventMetadata.newBuilder()
                .setUser(RandomStringUtils.randomAlphanumeric(10))
                .setAlgorithm(randomString())
                .build();
        builder.setMetadata(randomMetadata);
        return builder.build();
    }

    public static LoaderEvent generateRandomLoaderEvent() {
        LoaderEvent.Builder builder = LoaderEvent.newBuilder();
        builder.setSubtypeValue(getRandomSubtypes(ProcessEvent.DataCase.OPPORTUNITY_EVENT));
        LoaderEventMetadata randomMetadata = LoaderEventMetadata.newBuilder()
                .setUser(RandomStringUtils.randomAlphanumeric(10))
                .setJobId(randomString())
                .setPDSID(generateRandomXuuid("PDS"))
                .setBatchId(randomString())
                .build();
        builder.setMetadata(randomMetadata);
        return builder.build();
    }

    public static ETLEvent generateRandomETLEvent() {
        ETLEvent.Builder builder = ETLEvent.newBuilder();
        builder.setSubtypeValue(getRandomSubtypes(ProcessEvent.DataCase.OPPORTUNITY_EVENT));
        ETLEventMetadata randomMetadata = ETLEventMetadata.newBuilder()
                .setUser(RandomStringUtils.randomAlphanumeric(10))
                .setJobId(randomString())
                .build();
        builder.setMetadata(randomMetadata);
        return builder.build();
    }

    public static XUUID generateRandomXuuid(String type) {
        return XUUID.newBuilder().setType(type).setUuid(UUID.randomUUID().toString()).build();
    }

    public static int getRandomSubtypes(ProcessEvent.DataCase event) {
        List<Integer> subtypes = new ArrayList<>();
        switch (event) {
            case OPPORTUNITY_EVENT:
                OpportunityEvent.SubTypes.getDescriptor().getValues().stream().forEach(v -> subtypes.add(v.getNumber()));
                break;
            case PREDICTION_EVENT:
                PredictionEvent.SubTypes.getDescriptor().getValues().stream().forEach(v -> subtypes.add(v.getNumber()));
                break;
            case LOADER_EVENT:
                LoaderEvent.SubTypes.getDescriptor().getValues().stream().forEach(v -> subtypes.add(v.getNumber()));
                break;
            case ETL_EVENT:
                ETLEvent.SubTypes.getDescriptor().getValues().stream().forEach(v -> subtypes.add(v.getNumber()));
                break;
            case CDI_EVENT:
                CDIEvent.SubTypes.getDescriptor().getValues().stream().forEach(v -> subtypes.add(v.getNumber()));
                break;
            case DATA_NOT_SET:
                throw new IllegalArgumentException("Must have datacase specified to get subtypes");
            default:
                throw new IllegalArgumentException("Unknown database specified");
        }

        int subtypeIndex = ThreadLocalRandom.current().nextInt(0, subtypes.size());

        return subtypes.get(subtypeIndex);
    }

    private static String randomString() {
        return RandomStringUtils.randomAlphanumeric(10);
    }

    public static List<TrackingEventRecord> getTestEvents(Integer num) {
        return getTestEvents(num, null, null, null);
    }

    public static List<TrackingEventRecord> getTestEvents(Integer num, EventCase eventCase, DataCase contentsData, ProcessEvent.DataCase processDataCase) {
        List<TrackingEventRecord> events = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            events.add(TestUtils.generateRandomTrackingEvent(eventCase, contentsData, processDataCase));
        }
        return events;
    }
}
