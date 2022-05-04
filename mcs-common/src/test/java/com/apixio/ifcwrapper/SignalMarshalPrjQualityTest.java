package com.apixio.ifcwrapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.apixio.ensemble.ifc.StringLocation;
import com.apixio.ensemble.ifc.transport.Signals;
import com.apixio.ifcwrapper.signal.SignalDataWrapper;
import com.apixio.ifcwrapper.util.TestUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SignalMarshalPrjQualityTest {

    private static String docSigJson;

    @BeforeAll
    public static void beforeAll() throws Exception {
        docSigJson = TestUtil.resourceToString("signals/v1/dateIsAssumedV1.json");
    }

    @Test
    public void testStringLocationSerializationDeserialization() throws Exception {
        Signals.Signal signalWithoutLocation = SignalMarshal.fromJson(docSigJson).getProto();
        Signals.DocumentSource docSource = signalWithoutLocation.getDocumentSource();
        assertThat(docSource.hasStringLocation()).isFalse();

        Signals.DocumentSource docSourceWithLocation = docSource.toBuilder()
            .setStringLocation(
                Signals.StringLocation.newBuilder().setLocDescriptor("foobar").build())
            .build();

        SignalDataWrapper signalWithLoc = new SignalDataWrapper(
            signalWithoutLocation.toBuilder().setDocumentSource(docSourceWithLocation).build());

        assertThat(signalWithLoc.getSource().getLocation() instanceof StringLocation).isTrue();
        assertThat(((StringLocation) signalWithLoc.getSource().getLocation()).getDescriptor())
            .isEqualTo("foobar");

        SignalDataWrapper roundTripBytes =
            SignalMarshal.fromProtoBytes(SignalMarshal.toProtoBytes(signalWithLoc, true));

        assertThat(roundTripBytes.getSource().getLocation() instanceof StringLocation).isTrue();
        assertThat(((StringLocation) roundTripBytes.getSource().getLocation()).getDescriptor())
            .isEqualTo("foobar");
    }

    @Test
    public void testStringLocationNullSerializationDeserialization() throws Exception {
        Signals.Signal signalWithoutLocation = SignalMarshal.fromJson(docSigJson).getProto();
        Signals.DocumentSource docSource = signalWithoutLocation.getDocumentSource();
        assertThat(docSource.hasStringLocation()).isFalse();

        SignalDataWrapper signalWithoutLoc = new SignalDataWrapper(
            signalWithoutLocation.toBuilder().setDocumentSource(docSource).build());

        assertThat(signalWithoutLoc.getSource().getLocation()).isNull();

        SignalDataWrapper roundTripBytes =
            SignalMarshal.fromProtoBytes(SignalMarshal.toProtoBytes(signalWithoutLoc, true));

        assertThat(roundTripBytes.getSource().getLocation()).isNull();
    }
}
