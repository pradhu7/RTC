package apixio.cardinalsystem.api.model.cassandra;

import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.DataType;
import com.datastax.driver.core.schemabuilder.Create;
import com.datastax.driver.core.schemabuilder.Create.Options;
import com.datastax.driver.core.schemabuilder.SchemaBuilder;
import com.datastax.driver.core.schemabuilder.SchemaBuilder.Caching;
import com.datastax.driver.core.schemabuilder.SchemaBuilder.Direction;
import com.datastax.driver.core.schemabuilder.TableOptions.CompactionOptions;
import com.datastax.driver.core.schemabuilder.TableOptions.CompressionOptions;
import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class CassandraPojoToCreate {

    private List<String> partitionKeys;
    private List<String> clusteringKeys;
    private List<String> columnKeys;
    private Map<String, DataType> fields;
    private Class klass;
    private Set<String> rejectedColumns;
    private Table tableAnnotation;
    private CompactionOptions compactionOptions;
    private CompressionOptions compressionOptions;
    private Map<String, Direction> clusteringOrder;
    private Caching caching;
    private String description;
    private Double bloomFilterFPChance;
    private Double readRepairChance;
    private Double dcLocalReadRepairChance;
    private int gcGraceSeconds;
    private ConsistencyLevel createTableConsistencyLevel;

    public CassandraPojoToCreate(Class klass) {
        partitionKeys = new ArrayList<>();
        clusteringKeys = new ArrayList<>();
        columnKeys = new ArrayList<>();
        fields = new LinkedHashMap<>();
        rejectedColumns = new LinkedHashSet<>();
        this.klass = klass;
        compactionOptions = SchemaBuilder.leveledStrategy()
                .ssTableSizeInMB(160)
                .tombstoneCompactionIntervalInDay(10)
                .tombstoneThreshold(0.2);
        compressionOptions = SchemaBuilder.lz4().withChunkLengthInKb(4);
        clusteringOrder = new LinkedHashMap<>();
        caching = Caching.KEYS_ONLY;
        description = String.format("automatically generated from %s", klass.getName());
        bloomFilterFPChance = 0.1;
        readRepairChance = 0.0;
        dcLocalReadRepairChance = 0.1;
        gcGraceSeconds = 864000;
        createTableConsistencyLevel = ConsistencyLevel.ALL;
    }


    public Create createTableBuilder() {
        Create create = SchemaBuilder.createTable(tableAnnotation.keyspace(), tableAnnotation.name());
        create.setConsistencyLevel(ConsistencyLevel.ALL);
        for (String partitionKey : partitionKeys) {
            create.addPartitionKey(partitionKey, fields.get(partitionKey));
        }
        for (String clusteringKey : clusteringKeys) {
            create.addClusteringColumn(clusteringKey, fields.get(clusteringKey));
        }
        for (String columnKey : columnKeys) {
            create.addColumn(columnKey, fields.get(columnKey));
        }
        create.setConsistencyLevel(createTableConsistencyLevel);
        create.ifNotExists();
        return create;
    }

    public Options createTableWithOptions() {
        Create create = createTableBuilder();
        Options createWithOptions = create.withOptions()
                .compactionOptions(compactionOptions)
                .compressionOptions(compressionOptions)
                .comment(description)
                .caching(caching)
                .bloomFilterFPChance(bloomFilterFPChance)
                .readRepairChance(readRepairChance)
                .dcLocalReadRepairChance(dcLocalReadRepairChance)
                .gcGraceSeconds(gcGraceSeconds);
        createWithOptions.setConsistencyLevel(createTableConsistencyLevel);

        for (Entry<String, Direction> sortOrder : clusteringOrder.entrySet()) {
            if (clusteringKeys.contains(sortOrder.getKey())) {
                throw new IllegalArgumentException(String.format("%s key is not a valid clustering column for table %s", sortOrder.getKey(), tableAnnotation.name()));
            }
            createWithOptions.clusteringOrder(sortOrder.getKey(), sortOrder.getValue());
        }
        return createWithOptions;
    }

    public void parseFields() {
        Annotation annotation = klass.getDeclaredAnnotation(Table.class);
        if (Objects.isNull(annotation)) {
            throw new IllegalArgumentException(String.format("%s has no table annotation", klass.getName()));
        }
        Table table = (Table) annotation;
        this.tableAnnotation = table;
        Map<Integer, String> partitionKeyOrderMap = new HashMap<>();
        Map<Integer, String> clusteringKeyOrderMap = new HashMap<>();
        for (Field field : klass.getDeclaredFields()) {
            Column column = field.getAnnotation(Column.class);
            if (Objects.isNull(column) || Objects.isNull(column.name()) || column.name().isEmpty()) {
                rejectedColumns.add(field.getName());
                continue;
            }
            fields.put(column.name(), getCsqlDataType(field.getGenericType()));
            PartitionKey partitionKey = field.getAnnotation(PartitionKey.class);
            if (Objects.nonNull(partitionKey)) {
                partitionKeyOrderMap.put(partitionKey.value(), column.name());
                //partitionKeys.add(partitionKey.value(), column.name());
                continue;
            }
            ClusteringColumn clusteringColumn = field.getAnnotation(ClusteringColumn.class);
            if (Objects.nonNull(clusteringColumn)) {
                clusteringKeyOrderMap.put(clusteringColumn.value(), column.name());
                //clusteringKeys.add(clusteringColumn.value(), column.name());
                continue;
            }
            columnKeys.add(column.name());
        }
        partitionKeys = partitionKeyOrderMap.entrySet().stream().sorted(Comparator.comparing(Entry::getKey)).map(e -> e.getValue()).collect(Collectors.toList());
        clusteringKeys = clusteringKeyOrderMap.entrySet().stream().sorted(Comparator.comparing(Entry::getKey)).map(e -> e.getValue()).collect(Collectors.toList());
    }

    /**
     * incomplete conversion from java types -> csql types
     * reference: https://docs.datastax.com/en/developer/java-driver/3.1/manual/#cql-to-java-type-mapping
     *
     * @param obj the type object representing the field
     * @return a csql DataType Object. toString will output the csql for the data type
     */
    public DataType getCsqlDataType(Type obj) {
        DataType type;
        Type klassType;
        // handle cases where the class has a parameter. IE list/map
        if (obj instanceof ParameterizedType) {
            klassType = ((ParameterizedType) obj).getRawType();
        } else {
            klassType = obj;
        }
        if (klassType == String.class) {
            type = DataType.text();
        } else if (klassType == int.class || klassType == Integer.class) {
            type = DataType.cint();
        } else if (klassType == Long.class) {
            type = DataType.bigint();
        } else if (klassType == Map.class) {
            ParameterizedType ptype = (ParameterizedType) obj;
            DataType key = this.getCsqlDataType(ptype.getActualTypeArguments()[0]);
            DataType value = this.getCsqlDataType(ptype.getActualTypeArguments()[1]);
            type = DataType.map(key, value);
        } else if (klassType == List.class) {
            ParameterizedType ptype = (ParameterizedType) obj;
            DataType value = this.getCsqlDataType(ptype.getActualTypeArguments()[0]);
            type = DataType.list(value);
        } else if (klassType == Boolean.class) {
            type = DataType.cboolean();
        } else {
            throw new IllegalAccessError(String.format("Unimplemented/unknown type to convert %s", obj.getTypeName()));
        }

        return type;

    }

    public Set<String> getRejectedColumns() {
        return rejectedColumns;
    }

    public Table getTableAnnotation() {
        return tableAnnotation;
    }

    public List<String> getPartitionKeys() {
        return partitionKeys;
    }

    public List<String> getClusteringKeys() {
        return clusteringKeys;
    }

    public Map<String, DataType> getFields() {
        return fields;
    }

    public void setCompactionOptions(CompactionOptions compactionOptions) {
        this.compactionOptions = compactionOptions;
    }

    public void setCompressionOptions(CompressionOptions compressionOptions) {
        this.compressionOptions = compressionOptions;
    }

    public void setClusteringOrder(Map<String, Direction> clusteringOrder) {
        this.clusteringOrder = clusteringOrder;
    }

    public void setCaching(Caching caching) {
        this.caching = caching;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setBloomFilterFPChance(Double bloomFilterFPChance) {
        this.bloomFilterFPChance = bloomFilterFPChance;
    }

    public void setReadRepairChance(Double readRepairChance) {
        this.readRepairChance = readRepairChance;
    }

    public void setDcLocalReadRepairChance(Double dcLocalReadRepairChance) {
        this.dcLocalReadRepairChance = dcLocalReadRepairChance;
    }

    public void setGcGraceSeconds(int gcGraceSeconds) {
        this.gcGraceSeconds = gcGraceSeconds;
    }

    public void setCreateTableConsistencyLevel(ConsistencyLevel createTableConsistencyLevel) {
        this.createTableConsistencyLevel = createTableConsistencyLevel;
    }

    public String getTableName() {
        return tableAnnotation.name();
    }
}
