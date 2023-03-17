package uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain.services;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.alias.Alias;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.elasticsearch.common.settings.Settings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain.CaseTypeComponent;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain.IndexMode;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class IndexService {

    private final RestHighLevelClient client;

    private final String baselineIndex;

    private final String prefix;

    private final IndexMode indexMode;

    private final String timestamp;

    private final Set<String> types;

    public IndexService(RestHighLevelClient client,
                        CaseTypeComponent caseTypeComponent,
                        @Value("${app.create.baseline}") String baselineIndex,
                        @Value("${app.create.prefix}") String prefix,
                        @Value("${app.create.timestamp}") String timestamp,
                        @Value("${app.mode}") IndexMode indexMode) {
        this.client = client;
        this.prefix = prefix;
        this.indexMode = indexMode;
        this.baselineIndex = baselineIndex;
        this.types = caseTypeComponent.getTypes();

        var dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        this.timestamp = timestamp != null ? timestamp : dateFormat.format(Date.from(Instant.now()));

        log.info("Baseline Index: {}", baselineIndex);
        log.info("Indexing Mode: {}", indexMode);
        log.info("Index prefix: {}", prefix);
        log.info("Index timestamp: {}", timestamp);
    }

    public void createIndexes() throws IOException {
        GetIndexResponse getIndexResponse = client.indices().get(new GetIndexRequest(baselineIndex),
            RequestOptions.DEFAULT);

        var indexMapping = getIndexResponse.getMappings().get(baselineIndex).sourceAsMap();
        var settings = IndexSettings.getFilteredSettings(getIndexResponse.getSettings().get(baselineIndex));

        if (indexMode == IndexMode.SINGULAR) {
            createIndex(null, indexMapping, settings, false);
        } else {
            for (String caseType : this.types) {
                createIndex(caseType, indexMapping, settings, true);
            }
        }
    }

    private void createIndex(String type,
                             Map<String, Object> indexMapping,
                             Settings settings,
                             boolean multipleSearchAlias) throws IOException {
        var indexName = getIndexName(type);

        var createIndexRequest = new CreateIndexRequest(indexName).alias(
            new Alias(getTypeAliasName(type, true)).writeIndex(true)).alias(
            new Alias(getTypeAliasName(type, false)).writeIndex(false)).mapping(indexMapping).settings(settings);

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

    static class IndexSettings {

        private static final Set<String> excludedSettings = Set.of("index.number_of_shards", "index.number_of_replicas",
            "index.creation_date", "index.provided_name", "index.uuid", "index.version.created",
            "index.version.upgraded");

        private IndexSettings() {}

        public static Settings getFilteredSettings(Settings settings) {
            var value = Settings.builder().put(settings);

            for (String excludedSetting : excludedSettings) {
                value.remove(excludedSetting);
            }

            return value.build();
        }

    }

}
