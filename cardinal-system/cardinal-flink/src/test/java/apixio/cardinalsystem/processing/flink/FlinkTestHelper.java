package apixio.cardinalsystem.processing.flink;

import apixio.cardinalsystem.api.model.cassandra.CassandraPojoToCreate;
import com.datastax.driver.core.Session;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FlinkTestHelper {
    public static void createCassandraTables(Session cqlSession) {
        List<String> createCqls = new ArrayList<>();
        for (String packageName : Arrays.asList("docinfo", "fileinfo", "processevent", "transferevent")) {
            new Reflections(String.format("apixio.cardinalsystem.api.model.cassandra.%s", packageName), Scanners.SubTypes.filterResultsBy(c -> true))
                    .getSubTypesOf(Object.class)
                    .stream()
                    .forEach(klass -> {
                        CassandraPojoToCreate create = new CassandraPojoToCreate(klass);
                        create.parseFields();
                        createCqls.add(create.createTableWithOptions().getQueryString());
                    });
        }

        for (String createCQL : createCqls) {
            System.out.println(String.format("Running create table %s", createCQL));
            cqlSession.execute(String.format("%s;", createCQL));
        }
    }
}
