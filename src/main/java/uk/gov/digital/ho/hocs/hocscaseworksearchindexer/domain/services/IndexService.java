package uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain.services;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain.CaseType;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain.IndexMode;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
@Slf4j
public class IndexService {

    private final RestHighLevelClient client;

    private final String baselineIndex;

    private final String prefix;

    private final IndexMode indexMode;

    private final String timestamp;

    public IndexService(RestHighLevelClient client,
                        @Value("${app.index.baseline}") String baselineIndex,
                        @Value("${app.index.prefix}") String prefix,
                        @Value("${app.mode}") IndexMode indexMode) {
        this.client = client;
        this.prefix = prefix;
        this.indexMode = indexMode;
        this.baselineIndex = baselineIndex;

        var dateFormat = new SimpleDateFormat("yyyyMMddHHmm");
        this.timestamp = dateFormat.format(Date.from(Instant.now()));

        log.info("Baseline Index: {}", baselineIndex);
        log.info("Indexing Mode: {}", indexMode);
        log.info("Index prefix: {}", prefix);
        log.info("Index timestamp: {}", timestamp);
    }

    public void createIndexes() throws IOException {
        GetIndexResponse getIndexResponse =
            client.indices().get(new GetIndexRequest(baselineIndex),
            RequestOptions.DEFAULT);

        var indexMapping = getIndexResponse.getMappings().get(baselineIndex).sourceAsMap();

        if (indexMode == IndexMode.SINGULAR) {
            createIndex(null, indexMapping, false);
        } else {
            for (CaseType caseType : CaseType.values()) {
                createIndex(caseType.name(), indexMapping, true);
            }
        }
    }

    private void createIndex(String type, Map<String, Object> indexSourceMap, boolean multipleSearchAlias) throws IOException {
        var indexName = getIndexName(type);

        var createIndexRequest = new CreateIndexRequest(indexName)
            .source(indexSourceMap)
            .alias(new Alias(getTypeAliasName(type, true)).writeIndex(true))
            .alias(new Alias(getTypeAliasName(type, false)).writeIndex(false));

        if (multipleSearchAlias) {
            createIndexRequest.alias(new Alias(getGlobalReadAliasName()).writeIndex(false));
        }

        log.info("Creating new index {}.", indexName);
        client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
    }

    public String getIndexName(String type) {
        if (indexMode == IndexMode.SINGULAR) {
            return String.format("%s-%s-case", prefix, timestamp);
        }

        return String.format("%s-%s-%s", prefix, timestamp, type.toLowerCase());
    }

    private String getGlobalReadAliasName() {
        return String.format("%s-%s-read", prefix, timestamp);
    }

    private String getTypeAliasName(String type, boolean write) {
        if (indexMode == IndexMode.SINGULAR) {
            return String.format("%s-%s-case-%s", prefix, timestamp, write ? "write" : "read");
        }

        return String.format("%s-%s-%s-%s", prefix, timestamp, type.toLowerCase(), write ? "write" : "read");
    }

}
