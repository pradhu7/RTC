package com.apixio.datasource.elasticSearch;

import com.google.common.base.Strings;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Cancellable;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.*;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.IOException;
import java.util.Map;

public class ElasticCrud {
    private ElasticConnector elasticConnector;
    private RestHighLevelClient restHighLevelClient;

    public void setElasticConnector(ElasticConnector e)
    {
        elasticConnector = e;
        restHighLevelClient = elasticConnector.getHighLevelClient();
    }

    // index related

    /**
     *
     * @param indexName
     * @param settings
     * @param mapping
     * @param alias
     * @throws Exception
     */
    public void createIndexSync(String indexName, Map<String, Object> settings,
                                               Map<String, Object> mapping, String alias) throws Exception {
        if (Strings.isNullOrEmpty(indexName)) {
            throw new ElasticsearchException("Error: index name has to be defined");
        }
        if (mapping == null || mapping.isEmpty()) {
            throw new ElasticsearchException("Error: mapping has to be defined to create index");
        }
        CreateIndexRequest request = new CreateIndexRequest(indexName);
        request.mapping(mapping);

        if (settings != null && !settings.isEmpty()) {
            request.settings(settings);
        }
        if (!Strings.isNullOrEmpty(alias)) {
            request.alias(new Alias(alias));
        }

        CreateIndexResponse response = restHighLevelClient.indices().create(request, RequestOptions.DEFAULT);
        if (!response.isAcknowledged()) {
            throw new ElasticsearchException(String.format("create index failed to get acknowledged for indexName %s", indexName));
        } else if (!response.isShardsAcknowledged()) {
            throw new ElasticsearchException(String.format("create index failed to get acknowledged by shards for indexName %s", indexName));
        }
    }

    /**
     *
     * @param indexName
     * @return
     * @throws Exception
     */
    public Map<String, Object> getIndexMappingSync(String indexName) throws Exception {
        GetMappingsRequest request = new GetMappingsRequest();
        request.indices(indexName);
        GetMappingsResponse getMappingResponse = restHighLevelClient.indices().getMapping(request, RequestOptions.DEFAULT);
        return getMappingResponse.mappings().get(indexName).sourceAsMap();
    }

    /**
     *
     * @param indexName
     * @param mapping
     * @throws Exception
     */
    public void updateMappingSync(String indexName, Map<String, Object> mapping) throws Exception {
        PutMappingRequest request = new PutMappingRequest(indexName);
        request.source(mapping);
        AcknowledgedResponse acknowledgedResponse = restHighLevelClient.indices().putMapping(request, RequestOptions.DEFAULT);
        if (!acknowledgedResponse.isAcknowledged()) {
            throw new ElasticsearchException(String.format("update mapping failed to get acknowledged for indexName %s", indexName));
        }
    }

    /**
     *
     * @param indexName
     * @throws Exception
     */
    public void deleteIndexSync(String indexName) throws Exception {
        DeleteIndexRequest request = new DeleteIndexRequest(indexName);
        AcknowledgedResponse acknowledgedResponse = restHighLevelClient.indices().delete(request, RequestOptions.DEFAULT);
        if (!acknowledgedResponse.isAcknowledged()) {
            throw new ElasticsearchException(String.format("delete index failed to get acknowledged for indexName %s", indexName));
        }
    }

    // document related

    /**
     *
     * @param indexName
     * @param documentId
     * @param documentJsonStr
     * @throws IOException
     */
    public void indexDocumentSync(String indexName, String documentId, String documentJsonStr) throws IOException {
        IndexRequest request = new IndexRequest(indexName);
        request.id(documentId);
        request.source(documentJsonStr, XContentType.JSON);
        IndexResponse indexResponse = restHighLevelClient.index(request, RequestOptions.DEFAULT);
        DocWriteResponse.Result responseResult = indexResponse.getResult();
        if (responseResult != DocWriteResponse.Result.CREATED && responseResult != DocWriteResponse.Result.UPDATED) {
            throw new ElasticsearchException(String.format("unknown response result %s when indexing document %s for indexName %s",
                    responseResult.toString(), documentId, indexName));
        }
        ReplicationResponse.ShardInfo shardInfo = indexResponse.getShardInfo();
        if (shardInfo.getFailed() > 0) {
            StringBuilder failureReasonBuilder = new StringBuilder();
            for (ReplicationResponse.ShardInfo.Failure failure : shardInfo.getFailures()) {
                failureReasonBuilder.append(failure.reason());
            }
            throw new ElasticsearchException(String.format("replication failed when indexing document %s for indexName %s , error %s",
                    documentId, indexName, failureReasonBuilder.toString()));
        }
    }

    /**
     *
     * @param indexName
     * @param documentId
     * @param documentJsonStr
     * @param listener
     * @return
     */
    public Cancellable indexDocumentAsync(String indexName, String documentId, String documentJsonStr, ActionListener<IndexResponse> listener) {
        IndexRequest request = new IndexRequest(indexName);
        request.id(documentId);
        request.source(documentJsonStr, XContentType.JSON);
        return restHighLevelClient.indexAsync(request, RequestOptions.DEFAULT, listener);
    }

    /**
     *
     * @param indexName
     * @param documentId
     * @return
     * @throws Exception
     */
    public Map<String, Object> getDocumentSync(String indexName, String documentId) throws Exception {
        GetRequest getRequest = new GetRequest(indexName, documentId);
        GetResponse getResponse = restHighLevelClient.get(getRequest, RequestOptions.DEFAULT);
        return getResponse.getSourceAsMap();
    }

    /**
     *
     * @param indexName
     * @param documentId
     * @param partialDocumentJsonStr
     * @throws Exception
     */
    public void updateDocumentSync(String indexName, String documentId, String partialDocumentJsonStr) throws Exception {
        UpdateRequest request = new UpdateRequest(indexName, documentId);
        request.doc(partialDocumentJsonStr, XContentType.JSON);
        UpdateResponse updateResponse = restHighLevelClient.update(request, RequestOptions.DEFAULT);
        ReplicationResponse.ShardInfo shardInfo = updateResponse.getShardInfo();
        if (shardInfo.getFailed() > 0) {
            StringBuilder failureReasonBuilder = new StringBuilder();
            for (ReplicationResponse.ShardInfo.Failure failure :
                    shardInfo.getFailures()) {
                failureReasonBuilder.append(failure.reason());
            }
            throw new ElasticsearchException(String.format("replication failed when indexing document %s for indexName %s , error %s",
                    documentId, indexName, failureReasonBuilder.toString()));
        }
    }

    /**
     *
     * @param indexName
     * @param documentId
     * @param partialDocumentJsonStr
     * @param listener
     * @return
     */
    public Cancellable updateDocumentAsync(String indexName, String documentId, String partialDocumentJsonStr, ActionListener<UpdateResponse> listener) {
        UpdateRequest request = new UpdateRequest(indexName, documentId);
        request.doc(partialDocumentJsonStr, XContentType.JSON);
        return restHighLevelClient.updateAsync(request, RequestOptions.DEFAULT, listener);
    }

    // TODO: add bulk processor when we have a use case


}
