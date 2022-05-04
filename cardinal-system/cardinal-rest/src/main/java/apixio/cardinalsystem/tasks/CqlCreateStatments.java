package apixio.cardinalsystem.tasks;

import apixio.cardinalsystem.api.model.cassandra.CassandraClient;
import apixio.cardinalsystem.core.CardinalSysServices;
import com.google.common.collect.ImmutableMultimap;
import io.dropwizard.servlets.tasks.Task;

import java.io.PrintWriter;

public class CqlCreateStatments extends Task {
    private CassandraClient cassandraClient;

    public CqlCreateStatments(CardinalSysServices sysServices) {
        super("cqlcreatestatements");
        this.cassandraClient = sysServices.getCassandraClient();
    }

    @Override
    public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
        output.println(cassandraClient.createCqls());
    }
}
