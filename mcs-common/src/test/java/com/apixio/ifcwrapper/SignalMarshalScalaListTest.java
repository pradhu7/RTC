package com.apixio.ifcwrapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.apixio.ifcwrapper.signal.SignalDataWrapper;
import com.apixio.ifcwrapper.util.TestUtil;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SignalMarshalScalaListTest {

    static String scalaSignalListJson;

    @BeforeAll
    public static void beforeAll() throws Exception {
        scalaSignalListJson = TestUtil.resourceToString("signals/legacy/generatorList.json");
    }

    @Test
    public void scalaListDeserializationTest() throws Exception {
        List<SignalDataWrapper> signalList = SignalMarshal.fromJsonList(scalaSignalListJson);
        assertThat(signalList.size()).isEqualTo(2);
        assertThat(signalList.get(0).getName()).isEqualTo("V5-V22_70");
        assertThat(signalList.get(1).getName()).isEqualTo("V5-V22_166");
    }
}
