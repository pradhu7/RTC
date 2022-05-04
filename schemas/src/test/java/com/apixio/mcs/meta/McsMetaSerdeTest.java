package com.apixio.mcs.meta;

import static org.assertj.core.api.Assertions.assertThat;

import com.apixio.mcs.meta.McMeta.ExecutorType;
import com.apixio.mcs.meta.McMeta.FullMeta;
import com.apixio.mcs.meta.McMeta.PartMeta;
import com.apixio.mcs.meta.McMeta.PythonServiceConfig;
import com.apixio.messages.LambdaMessages.AlgorithmType;
import com.google.protobuf.util.JsonFormat;
import java.io.File;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * This file contains unit tests for serialization and deserialization of MCS REST responses
 * containing model combination metadata to model combination protobufs
 */
public class McsMetaSerdeTest {

    private static String EXAMPLE_COMBINER_MC_META;
    private static String MOCK_COMBINER_MC_META;

    @BeforeAll
    public static void setupAll() throws Exception {
        ClassLoader classLoader = McsMetaSerdeTest.class.getClassLoader();
        EXAMPLE_COMBINER_MC_META = FileUtils.readFileToString(new File(
            classLoader.getResource("mcs/combiner_mc.json").getFile()));
        MOCK_COMBINER_MC_META = FileUtils.readFileToString(new File(
            classLoader.getResource("mcs/mock_mc.json").getFile()));
    }

    @Test
    public void testV22CombinerDeserialization() throws Exception {
        McMeta.FullMeta.Builder builder = McMeta.FullMeta.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(EXAMPLE_COMBINER_MC_META, builder);
        McMeta.FullMeta mcMeta = builder.build();
        assertThat(mcMeta.getId()).isEqualTo("B_1c73857a-c261-4f83-9517-c586b2c4a451");
        assertThat(mcMeta.getCore().getAlgorithm().getType()).isEqualTo(AlgorithmType.ENSEMBLE);
        assertThat(mcMeta.getCore().getAlgorithm().getType()).isEqualTo(AlgorithmType.ENSEMBLE);
    }

    @Test
    public void testExecutorDeserialization() throws Exception {
        McMeta.FullMeta.Builder builder = McMeta.FullMeta.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(EXAMPLE_COMBINER_MC_META, builder);
        McMeta.FullMeta mcMeta = builder.build();

        assertThat(mcMeta.getCore().getSupportedExecutorsList().get(0).getExecutorType()
            == ExecutorType.AUTO);
        assertThat(mcMeta.getCore().getSupportedExecutorsList().get(0).getExecutorVersion()
            .equals("0.0.1-SNAPSHOT"));
    }

    @Test
    public void testGitMetaDeserialization() throws Exception {
        McMeta.FullMeta.Builder builder = McMeta.FullMeta.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(EXAMPLE_COMBINER_MC_META, builder);
        McMeta.FullMeta mcMeta = builder.build();
        assertThat(mcMeta.getCore().getGit().getHash())
            .isEqualTo("91cbedeb5349f146b722d0dada297975e83747da");
    }

    @Test
    public void testSearchDeserialization() throws Exception {
        McMeta.FullMeta.Builder builder = McMeta.FullMeta.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(EXAMPLE_COMBINER_MC_META, builder);
        McMeta.FullMeta mcMeta = builder.build();
        assertThat(mcMeta.getSearch().getEngine()).isEqualTo("MA");
        assertThat(mcMeta.getSearch().getVariant()).isEqualTo("V22");
        assertThat(mcMeta.getSearch().getTagsList().size()).isEqualTo(3);
        assertThat(mcMeta.getSearch().getTagsList().contains("COMBINER")).isTrue();
    }


    @Test
    public void testPartsDeserialization() throws Exception {
        McMeta.FullMeta.Builder builder = McMeta.FullMeta.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(EXAMPLE_COMBINER_MC_META, builder);
        McMeta.FullMeta mcMeta = builder.build();
        List<PartMeta> parts = mcMeta.getPartsList();

        assertThat(parts.size()).isEqualTo(3);

        assertThat(parts.get(0).getS3Path()).isEqualTo(
            "s3://apixio-smasmodels/modelblobs/B_1c73857a-c261-4f83-9517-c586b2c4a451/config.yaml");
        assertThat(parts.get(0).getName()).isEqualTo("config.yaml");

        assertThat(parts.get(1).getS3Path()).isEqualTo(
            "s3://apixio-smasmodels/modelblobs/B_729f81a3-d26b-4245-bde9-32a803a42d2d/model.zip");
        assertThat(parts.get(1).getName()).isEqualTo("model.zip");
        assertThat(parts.get(1).getCore().getUri())
            .isEqualTo(
                "https://repos.apixio.com/artifactory/models/models/production_model/9.0.2/production_model_9_0_2.zip");

        assertThat(parts.get(2).getS3Path()).isEqualTo(
            "s3://apixio-smasmodels/modelblobs/B_eee97abf-0e09-43ac-8efc-f0ba289d609f/implementation.jar");
        assertThat(parts.get(2).getName()).isEqualTo("implementation.jar");
    }


    @Test
    public void testMockMcDeserialization() throws Exception {
        McMeta.FullMeta.Builder builder = McMeta.FullMeta.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(MOCK_COMBINER_MC_META, builder);
        McMeta.FullMeta mockMeta = builder.build();
        assertThat(mockMeta.getCore().getAlgorithm().getJavaClass())
            .isEqualTo("com.apixio.ensemble.impl.combiners.MockV22EventTypeCombiner");
        assertThat(mockMeta.getCore().getAlgorithm().getMock()).isTrue();
    }

    @Test
    public void testExternalServiceDeserialization() throws Exception {
        PythonServiceConfig pythonServiceConfig = PythonServiceConfig.newBuilder()
            .setPackageName("python-apxensemble:1.14.0")
            .setEntryPoint("har2service ${har2_saved_model}")
            .setApiEndpoint("http://localhost:5000")
            .build();
        McMeta.FullMeta.Builder builder = McMeta.FullMeta.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(MOCK_COMBINER_MC_META, builder);

        builder.getCoreBuilder().getAlgorithmBuilder().addPythonServiceConfig(pythonServiceConfig);

        FullMeta meta = builder.build();

        assertThat(
            meta.getCore().getAlgorithm().getPythonServiceConfigList().get(0))
            .isEqualTo(pythonServiceConfig);
    }
}
