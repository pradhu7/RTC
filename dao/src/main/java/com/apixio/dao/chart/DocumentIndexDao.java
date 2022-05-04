package com.apixio.dao.chart;

import com.apixio.datasource.elasticSearch.ElasticCrud;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentIndexDao {

    private static final Logger logger = LoggerFactory.getLogger(DocumentIndexDao.class);

    public static final String documentIndexType = "document";

    private ElasticCrud elasticCrud;

    public DocumentIndexDao(ElasticCrud elasticCrud) {
        this.elasticCrud = elasticCrud;
    }

    public void createIndex(String pds, Map<String, Object> settings, Map<String, Object> mapping, String alias) throws Exception {
        String indexName = pds + "_" + documentIndexType;
        elasticCrud.createIndexSync(indexName = indexName,
                    settings = settings, mapping = mapping, alias = alias);
    }


}
