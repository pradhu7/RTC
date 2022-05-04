package com.apixio.mcs.meta;

import static org.assertj.core.api.Assertions.assertThat;

import com.apixio.mcs.meta.McMeta.PartMeta;
import com.apixio.mcs.meta.McTestMeta.TestDataType;
import com.apixio.mcs.meta.McTestMeta.TestScope;
import com.google.protobuf.util.JsonFormat;
import java.io.File;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * This file contains unit tests for serialization and deserialization of MCS REST responses
 * containing model combination unit and integration tests to model combination test protobufs
 */
public class McsMetaTestSerdeTest {

    private static String COMBINER_UNIT_TEST_META;

    @BeforeAll
    public static void setupAll() throws Exception {
        ClassLoader classLoader = McsMetaSerdeTest.class.getClassLoader();
        COMBINER_UNIT_TEST_META = FileUtils.readFileToString(new File(
            classLoader.getResource("mcs/combiner_unit_test.json").getFile()));
    }

    @Test
    public void testUnitTestDefinitionDeserialization() throws Exception {
        McMeta.FullMeta.Builder builder = McMeta.FullMeta.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(COMBINER_UNIT_TEST_META, builder);
        McMeta.FullMeta testDataMcMeta = builder.build();
        assertThat(testDataMcMeta.getCore().getModelTest().getScope())
            .isEqualTo(TestScope.COMBINER_UNIT);
    }


    @Test
    public void testPartsDeserialization() throws Exception {
        McMeta.FullMeta.Builder builder = McMeta.FullMeta.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(COMBINER_UNIT_TEST_META, builder);
        McMeta.FullMeta mcMeta = builder.build();
        List<PartMeta> parts = mcMeta.getPartsList();

        assertThat(parts.size()).isEqualTo(2);

        assertThat(parts.get(0).getS3Path()).isEqualTo(
            "s3://apixio-smasmodels/modelblobs/signal_file_1.json.gz");
        assertThat(parts.get(0).getName()).isEqualTo("DOC_1_SIGNALS");
        assertThat(parts.get(0).getCore().getTestPartMeta().getDataType())
            .isEqualTo(TestDataType.SIGNALS);

        assertThat(parts.get(1).getS3Path()).isEqualTo(
            "s3://apixio-smasmodels/modelblobs/signal_file_2.json.gz");
        assertThat(parts.get(1).getName()).isEqualTo("DOC_2_SIGNALS");
        assertThat(parts.get(1).getCore().getTestPartMeta().getDataType())
            .isEqualTo(TestDataType.SIGNALS);
    }
}
