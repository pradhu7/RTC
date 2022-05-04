package apixio.cardinalsystem.tasks;

import apixio.cardinalsystem.api.model.cassandra.CassandraClient;
import apixio.cardinalsystem.api.model.cassandra.CassandraConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMultimap;
import io.dropwizard.servlets.tasks.PostBodyTask;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CqlCreateTables extends PostBodyTask {

    private ObjectMapper objectMapper;

    public CqlCreateTables(ObjectMapper objectMapper) {
        super("cqlcreatetables");
        this.objectMapper = objectMapper;
    }

    @Override
    public void execute(ImmutableMultimap<String, String> parameters, String postBody, PrintWriter output) throws Exception {
        Map<String, Object> response = new HashMap<>();
        CassandraConfig cassandraConfig = objectMapper.readValue(postBody, CassandraConfig.class);
        CassandraClient cassandraClient = new CassandraClient(cassandraConfig);
        List<String> tables = cassandraClient.createCassandraTables();
        response.put("message", String.format("created %d tables on %s", tables.size(), cassandraConfig.getHost()));
        response.put("tables", tables);
        output.write("completed create table");
        output.write(objectMapper.writeValueAsString(response));
        output.flush();
    }
}
