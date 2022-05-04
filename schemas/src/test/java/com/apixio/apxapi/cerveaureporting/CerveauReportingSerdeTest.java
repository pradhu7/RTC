package com.apixio.apxapi.cerveaureporting;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.util.JsonFormat;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CerveauReportingSerdeTest {

    private static String EXAMPLE_PROJECT_DATASET;

    @BeforeAll
    public static void setupAll() throws Exception {
        ClassLoader classLoader = CerveauReportingSerdeTest.class.getClassLoader();
        File file = new File(
            classLoader.getResource("apxapi/ExampleProjectDataset.json").getFile());
        EXAMPLE_PROJECT_DATASET = FileUtils.readFileToString(file);
    }

    @Test
    public void testDeserializationFromSmasReportingResponse() throws Exception {
        CerveauReporting.ProjectDataSetV2.Builder builder =
            CerveauReporting.ProjectDataSetV2.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(EXAMPLE_PROJECT_DATASET, builder);
        CerveauReporting.ProjectDataSetV2 projectDataSetV2 = builder.build();
        assertThat(projectDataSetV2.getCreatedBy()).isEqualTo("rwenzel@apixio.com");
        assertThat(projectDataSetV2.getModelsOrBuilderList().size()).isEqualTo(2);
        assertThat(projectDataSetV2.getModels(0).getMcid())
            .isEqualTo("B_1c73857a-c261-4f83-9517-c586b2c4a451");
        assertThat(projectDataSetV2.getModels(1).getMcid())
            .isEqualTo("B_a78470d1-38cd-4b71-b5ec-6634ab606dc2");
        assertThat(projectDataSetV2.getPdsUuid().getUuid())
            .isEqualTo("00000000-0000-0000-0000-000010001057");
        assertThat(projectDataSetV2.getProjectDataSetUuid().getUuid())
            .isEqualTo("18d470d4-cdec-41cc-b179-e87426292905");
        assertThat(projectDataSetV2.getProjectDataSetName())
            .isEqualTo("DS_globalhealth_20jan_app_id_b2_2");
        assertThat(projectDataSetV2.getNumberOfItems()).isEqualTo(4618);
    }
}
