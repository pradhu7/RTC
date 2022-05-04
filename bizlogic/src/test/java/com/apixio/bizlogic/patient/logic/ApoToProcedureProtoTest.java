package com.apixio.bizlogic.patient.logic;

import com.apixio.datacatalog.CodedBaseObjects;
import com.apixio.datacatalog.PatientProto;
import com.apixio.datacatalog.UUIDOuterClass;
import com.apixio.model.nassembly.Combiner;
import com.apixio.model.nassembly.Exchange;
import com.apixio.model.patient.Patient;
import com.apixio.model.patient.Procedure;
import com.apixio.nassembly.ffsclaims.FfsClaimExchange;
import com.apixio.nassembly.locator.AssemblyLocator;
import com.apixio.nassembly.patientffsclaims.PatientFfsClaimsExchange;
import org.junit.*;

import java.util.*;

@Ignore("Integration")
public class ApoToProcedureProtoTest {
    @Before
    public void setUp() throws Exception {
    }

    @After
    public void setAfter() throws Exception {
    }


    @Test
    public void testAssemblies() throws Exception {
        UUID patientId = UUID.randomUUID();

        Exchange ffsExchange = createFFSExchange(patientId);
        Patient apo = ffsExchange.toApo().iterator().next();
        Assert.assertNotNull(apo);
        Procedure apoProcedure = apo.getProcedures().iterator().next();
        // Test normalization of source, parsing details, and clinical actor
        Assert.assertEquals(apoProcedure.getParsingDetailsId(),
                apo.getParsingDetails().iterator().next().getParsingDetailsId());
        Assert.assertEquals(apoProcedure.getSourceId(), apo.getSources().iterator().next().getSourceId());
        Assert.assertEquals(apoProcedure.getPrimaryClinicalActorId(),
                apo.getClinicalActors().iterator().next().getClinicalActorId());


        Exchange wrapperExchange = createWrapperExchange(ffsExchange);
        Iterable<Exchange.ProtoEnvelop> protoEnvelops = wrapperExchange.getProtoEnvelops();
        Assert.assertNotNull(protoEnvelops.iterator().next().getProtoBytes());
        PatientProto.Patient wrapper =
                PatientProto.Patient.parseFrom(wrapperExchange.getProtoEnvelops().iterator().next().getProtoBytes());
        Assert.assertEquals(1, wrapper.getProceduresList());
        CodedBaseObjects.ProcedureCBO procedure = wrapper.getProcedures(0);

        PatientProto.CodedBasePatient base = wrapper.getBase();
        // Test normalization of the Source, ParsingDetails, and ClinicalActor
        UUIDOuterClass.UUID clinicalActorId = procedure.getBase().getPrimaryActorId();
        Assert.assertTrue(base.getClinicalActorsList().stream()
                .anyMatch(c -> c.getInternalId().equals(clinicalActorId)));

        List<UUIDOuterClass.UUID> sourceIdsList = procedure.getBase().getSourceIdsList();
        //TODO: Runtime complexity can be improved?
        Assert.assertTrue(base.getSourcesList().stream().anyMatch(s -> s.getInternalId().
                equals(sourceIdsList.stream())));


        List<UUIDOuterClass.UUID> parsingDetailsIdsList = procedure.getBase().getParsingDetailsIdsList();
        Assert.assertTrue(base.getParsingDetailsList().stream()
                .anyMatch(s -> s.getInternalId().equals(parsingDetailsIdsList.stream())));
    }

    private Exchange createFFSExchange(UUID patientId) {
        APOToProcedureProto apoToProcedureProto = new APOToProcedureProto(patientId);
        Exchange exchange = AssemblyLocator.getExchange(FfsClaimExchange.dataTypeName());
        List<byte[]> bytes = new ArrayList<>();
        bytes.add(apoToProcedureProto.procedureProto.toByteArray());
        exchange.fromProto(bytes);

        return exchange;
    }

    private Exchange createWrapperExchange(Exchange exchange) {
        Combiner<Exchange> combiner =
                AssemblyLocator.getCombine(PatientFfsClaimsExchange.dataTypeName());
        Map<String, Exchange[]> exchangeMap = new HashMap<>();
        Map<String, Object> metaMap = new HashMap<>();
        Exchange[] exchanges = new Exchange[1];
        exchanges[0] = exchange;
        exchangeMap.put("ffs", exchanges);

        Combiner.CombinerInput input = new Combiner.CombinerInput(exchangeMap, metaMap);

        return combiner.combine(input).iterator().next();
    }
}

