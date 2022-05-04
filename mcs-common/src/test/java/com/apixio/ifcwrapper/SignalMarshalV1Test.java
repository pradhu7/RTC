package com.apixio.ifcwrapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.apixio.XUUID;
import com.apixio.ensemble.ifc.DocumentSource;
import com.apixio.ensemble.ifc.Generator;
import com.apixio.ensemble.ifc.PageSource;
import com.apixio.ensemble.ifc.PageWindowSource;
import com.apixio.ensemble.ifc.SignalType;
import com.apixio.ensemble.ifc.Source;
import com.apixio.ifcwrapper.signal.SignalDataWrapper;
import com.apixio.ifcwrapper.util.JsonUtil;
import com.apixio.ifcwrapper.util.TestUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * This class tests that we can deserialize from jackson created SignalDataWrapper (v1) into
 * protobuf backed SignalDataWrapper.
 */
public class SignalMarshalV1Test {

    static String docSigJson;
    static String pageWindowSigJson;
    static String pageSignalJson;
    static String numericSignalJson;
    static String v5SignalJson;

    @BeforeAll
    public static void beforeAll() throws Exception {
        docSigJson = TestUtil.resourceToString("signals/v1/dateIsAssumedV1.json");
        pageWindowSigJson = TestUtil
            .resourceToString("signals/v1/faceToFaceV1.json");
        pageSignalJson = TestUtil
            .resourceToString("signals/v1/positionalTermSignalV1.json");
        numericSignalJson = TestUtil.resourceToString("signals/v1/wordCountV1.json");
        v5SignalJson = TestUtil.resourceToString("signals/v1/V5PageSignalV1.json");
    }

    @Test
    public void docSignalV1Test() throws Exception {
        SignalDataWrapper docSignal = SignalMarshal.fromJson(docSigJson);

        assertThat(docSignal.getType()).isEqualTo(SignalType.CATEGORY);
        assertThat(docSignal.getValue()).isEqualTo("true");
        assertThat(docSignal.getSource().getLocation()).isNull();

        Source source = docSignal.getSource();

        assertThat(source instanceof DocumentSource).isTrue();

        DocumentSource documentSource = (DocumentSource) source;
        assertThat(documentSource.getDocumentId().toString())
            .isEqualTo("DOC_642bb4b2-139a-4c01-9160-47213df2a72b");
        assertThat(documentSource.getPatientId().toString())
            .isEqualTo("PAT_0070c50b-2466-4bfb-a797-121a34afffe8");
        assertThat(documentSource.getNumPages()).isEqualTo(20);

        Generator generator = docSignal.getGenerator();
        assertThat(generator.className()).isEqualTo("com.apixio.signalmanager.PlainGenerator");
        assertThat(generator.jarVersion()).isEqualTo("1.4.0");
        assertThat(generator.getVersion()).isEqualTo("1.0.0");
        assertThat(generator.getName()).isEqualTo("DateIsAssumed");

        SignalDataWrapper roundTripBytes =
            SignalMarshal.fromProtoBytes(SignalMarshal.toProtoBytes(docSignal, true));

        assertThat(roundTripBytes).isEqualTo(docSignal);

        SignalDataWrapper roundTripJson =
            SignalMarshal.fromProtoJson(SignalMarshal.toProtoJson(docSignal, false, true));

        assertThat(roundTripJson).isEqualTo(docSignal);
    }

    @Test
    public void pageWindowSignalV1Test() throws Exception {

        SignalDataWrapper pageWindowSignal = SignalMarshal.fromJson(pageWindowSigJson);

        assertThat(pageWindowSignal.getType()).isEqualTo(SignalType.CATEGORY);
        assertThat(pageWindowSignal.getValue()).isEqualTo("true");
        assertThat(pageWindowSignal.getName()).isEqualTo("LogitFaceToFace-SUCCESS");
        assertThat(pageWindowSignal.getSource().getLocation()).isNull();

        Source source = pageWindowSignal.getSource();
        assertThat(source instanceof PageWindowSource).isTrue();
        PageWindowSource pageWindowSource = (PageWindowSource) source;
        assertThat(pageWindowSource.getPatientId().toString())
            .isEqualTo("PAT_0070c50b-2466-4bfb-a797-121a34afffe8");
        assertThat(pageWindowSource.getDocumentId().toString())
            .isEqualTo("DOC_000b3191-72ca-4bb6-ae72-22e35d292a95");
        assertThat(pageWindowSource.getStartPage()).isEqualTo(1);
        assertThat(pageWindowSource.getEndPage()).isEqualTo(4);
        assertThat(pageWindowSource.getCentroid()).isEqualTo(2);

        Generator generator = pageWindowSignal.getGenerator();
        assertThat(generator.className())
            .isEqualTo("com.apixio.ensemble.impl.generators.page.LogitFaceToFace");
        assertThat(generator.jarVersion()).isEqualTo("1.4.0");
        assertThat(generator.getVersion()).isEqualTo("1.0.0");
        assertThat(generator.getName()).isEqualTo("LogitFaceToFace");

        SignalDataWrapper roundTripBytes =
            SignalMarshal.fromProtoBytes(SignalMarshal.toProtoBytes(pageWindowSignal, true));

        assertThat(roundTripBytes).isEqualTo(pageWindowSignal);

        SignalDataWrapper roundTripJson =
            SignalMarshal.fromProtoJson(SignalMarshal.toProtoJson(pageWindowSignal, false, true));

        assertThat(roundTripJson).isEqualTo(pageWindowSignal);
    }


    @Test
    public void pageSignalV1Test() throws Exception {
        SignalDataWrapper pageSignal = SignalMarshal.fromJson(pageSignalJson);

        assertThat(pageSignal.getName()).isEqualTo("PositionalTerm-V22_22");
        assertThat(pageSignal.getType()).isEqualTo(SignalType.CATEGORY);
        assertThat(pageSignal.getGenerator().getVersion()).isEqualTo("1.0.0");
        assertThat(pageSignal.getGenerator().jarVersion())
            .isEqualTo("1.6.0-apo-drain-2019-04-15-2-SNAPSHOT");
        assertThat(pageSignal.getGenerator().getName()).isEqualTo("PositionalTerm");
        assertThat(pageSignal.getValue()).isEqualTo("BMI\t50\t-226\t0");

        assertThat(pageSignal.getSource() instanceof PageSource).isTrue();

        PageSource source = (PageSource) pageSignal.getSource();

        assertThat(source.getPage()).isEqualTo(15);
        assertThat(source.getDocumentId())
            .isEqualTo(XUUID.fromString("DOC_4bed93f8-1a30-4a91-8ba8-8b1a38e5002c"));
        assertThat(source.getPatientId())
            .isEqualTo(XUUID.fromString("PAT_ffdc1ab7-a7f8-4be3-a417-2177eb779525"));
    }


    @Test
    public void numericSignalLegacyTest() throws Exception {
        SignalDataWrapper numericSignal = SignalMarshal.fromJson(numericSignalJson);

        assertThat(numericSignal.getValue()).isEqualTo(1685.0f);
        assertThat(numericSignal.getType()).isEqualTo(SignalType.NUMERIC);

        SignalDataWrapper roundTripBytes =
            SignalMarshal.fromProtoBytes(SignalMarshal.toProtoBytes(numericSignal, true));

        assertThat(roundTripBytes).isEqualTo(numericSignal);

        SignalDataWrapper roundTripJson =
            SignalMarshal.fromProtoJson(SignalMarshal.toProtoJson(numericSignal, false, true));

        assertThat(roundTripJson).isEqualTo(numericSignal);
    }

    @Test
    public void v5SignalLegacyTest() throws Exception {
        SignalDataWrapper v5Signal = SignalMarshal.fromJson(v5SignalJson);

        System.out.println(JsonUtil.prettyPrint(v5Signal.getProto()));
    }

    @Test
    public void toProtoJsonShouldOmitDefaultFields() throws Exception {
        SignalDataWrapper numericSignal = SignalMarshal.fromJson(numericSignalJson);

        String briefJson = SignalMarshal.toProtoJson(numericSignal);

        assertThat(briefJson.contains("\"signalType\":\"NUMERIC\"")).isFalse();

        System.out.println(briefJson);

        SignalDataWrapper roundTripTest = SignalMarshal.fromProtoJson(briefJson);

        assertThat(roundTripTest).isEqualTo(numericSignal);
    }

    @Test
    public void toProtoJsonVerboseShouldIncludeDefaultFields() throws Exception {
        SignalDataWrapper numericSignal = SignalMarshal.fromJson(numericSignalJson);

        String verboseJson = SignalMarshal.toProtoJsonVerbose(numericSignal);

        assertThat(verboseJson.contains("\"signalType\":\"NUMERIC\"")).isTrue();

        System.out.println(verboseJson);

        SignalDataWrapper roundTripTest = SignalMarshal.fromProtoJson(verboseJson);

        assertThat(roundTripTest).isEqualTo(numericSignal);
    }
}
