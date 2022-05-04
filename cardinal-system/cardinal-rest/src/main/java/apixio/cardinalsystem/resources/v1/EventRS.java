package apixio.cardinalsystem.resources.v1;

import apixio.cardinalsystem.CardinalRestConfiguration;
import apixio.cardinalsystem.api.model.EventRequest;
import apixio.cardinalsystem.api.model.EventResponse;
import apixio.cardinalsystem.clients.KafkaProducerClient;
import apixio.cardinalsystem.core.CardinalSysServices;
import com.apixio.restbase.web.BaseRS;
import io.dropwizard.jersey.params.BooleanParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

@Path("/api/v1/cardinal")
public class EventRS extends BaseRS {
    private Executor kafkaExecutor;
    private KafkaProducerClient producerClient;
    private String kafkaTopic;
    private Logger logger = Logger.getLogger(EventRequest.class.getName());

    public EventRS(CardinalRestConfiguration config, CardinalSysServices sysServices) {
        super(config, sysServices, EventRS.class.getName());
        kafkaExecutor = sysServices.getKafkaExecutor();
        producerClient = sysServices.getProducerClient();
        kafkaTopic = sysServices.getKafkaTopic();
    }

    @POST
    @Path("/event")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public void postEvent(
            @Suspended AsyncResponse asyncResponse,
            @QueryParam("sync") @DefaultValue("false") BooleanParam sync,
            final EventRequest request
    ) {
        CompletableFuture.supplyAsync(() -> {
                    Boolean producerResult = false;
                    if (sync.get()) {
                        producerResult = producerClient.sendSync(request.getItems(), kafkaTopic);
                    } else {
                        producerResult = producerClient.sendAsync(request.getItems(), kafkaTopic);
                    }
                    if (!producerResult) {
                        throw new RuntimeException("unable to produce kafka message. see server logs for details");
                    }
                    EventResponse response = new EventResponse();
                    response.setCode(200);
                    response.setEvent_ids(request.getEventIds());
                    response.setMessage(String.format("Sent %d records", request.getItems().size()));
                    response.setTypes(request.getEventTypes());
                    return Response.ok(response).build();
                }, kafkaExecutor)
                .thenApply(r -> asyncResponse.resume(r))
                .exceptionally(e -> {
                    EventResponse eventResponse = new EventResponse();
                    eventResponse.setCode(500);
                    eventResponse.setMessage(
                            Arrays.stream(
                                    e.getStackTrace()
                            ).map(
                                    m -> m.toString()
                            ).reduce("", String::concat)
                    );
                    Response response = Response.serverError().status(500).entity(eventResponse).build();
                    return asyncResponse.resume(response);
                });
    }
}
