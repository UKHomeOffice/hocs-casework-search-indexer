package uk.gov.digital.ho.hocs.hocscaseworksearchindexer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterators;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.casework.model.Correspondent;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.casework.model.SomuItem;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.casework.model.Topic;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.casework.repository.CaseRepository;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.casework.model.CaseData;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Slf4j
@Service
public class ETLService {

    private IndexMode indexMode;

    private final String prefix;

    private final int batchSize;

    private final boolean newIndex;

    private final int batchInterval;

    private final CaseRepository caseRepository;

    private final RestHighLevelClient client;

    private ObjectMapper objectMapper;

    private final String liveIndex;

    private DateFormat dateFormat = new SimpleDateFormat("yyyyMMddhhmmss");

    private final  String databaseName;


    @PersistenceContext
    protected EntityManager entityManager;

    private final String timestamp;

    public ETLService(@Value("${batch.size}") int batchSize,
                      RestHighLevelClient client,
                      ObjectMapper objectMapper,
                      @Value("${aws.es.index-prefix}") String prefix,
                      @Value("${mode}") IndexMode indexMode,
                      @Value("${new-index}") boolean newIndex,
                      CaseRepository caseRepository,
                      @Value("${batch.interval}") int batchInterval,
                      @Value("${db.name:unset}") String databaseName) {
        this.batchSize = batchSize;
        this.newIndex = newIndex;
        this.caseRepository = caseRepository;
        this.client = client;
        this.prefix = prefix;
        this.objectMapper = objectMapper;
        this.liveIndex = String.format("%s-%s", prefix, "case");
        this.indexMode = indexMode;
        this.batchInterval = batchInterval;
        this.databaseName = databaseName;
        this.timestamp = dateFormat.format(Date.from(Instant.now()));
    }

    @Transactional(readOnly = true)
    public void migrate() throws IOException, InterruptedException {

        log.info("Indexing Mode: {}", indexMode);
        log.info("Create New Index: {}", newIndex);
        log.info("Batch Size: {}", batchSize);
        log.info("Batch Interval: {}", batchInterval);
        log.info("Indexing cases from {} to index with prefix {}", databaseName, prefix);
        Thread.sleep(6000);

        if (newIndex) {
            createNewIndexes();
        }

        AtomicInteger count = new AtomicInteger(0);
        try (Stream<CaseData> cases = caseRepository.getAllCasesAndCollections()) {
            Iterators.partition(cases.iterator(), batchSize).forEachRemaining(batch -> {
                BulkRequest bulkRequest = new BulkRequest();
                batch.forEach(caseData -> {
                    count.incrementAndGet();
                    log.info("Indexing case {} for UUID {}", count, caseData.getUuid());
                    entityManager.detach(caseData);

                    bulkRequest.add(new UpdateRequest(getMigrationIndex(caseData.getType()), caseData.getUuid().toString())
                        .docAsUpsert(true)
                        .doc(mapCaseData(caseData)));
                });

                try {
                    client.bulk(bulkRequest, RequestOptions.DEFAULT);
                    Thread.sleep(batchInterval);
                } catch (IOException | InterruptedException e) {
                    log.error("Error indexing batch ", e.getMessage());
                    throw new ElasticSearchFailureException("Failed to index batch", e);
                }
            });
        }
    }

    private String getMigrationIndex(String type) {
        if (indexMode == IndexMode.SINGULAR) {
            if (newIndex) {
                return String.format("%s-%s-%s", prefix, "case", timestamp);
            } else {
                return liveIndex;
            }
        } else {
            if(newIndex) {
                return String.format("%s-%s-%s", prefix, type.toLowerCase(), timestamp);
            } else {
                return String.format("%s-%s", prefix, type.toLowerCase());
            }
        }
    }

    private void createNewIndexes() throws IOException {
        GetIndexResponse getIndexResponse = client.indices().get(new GetIndexRequest(liveIndex),
            RequestOptions.DEFAULT);
        if (indexMode == IndexMode.SINGULAR) {
            var index = getMigrationIndex(null);
            log.info("Creating new index {} for migration", index);
            client.indices().create(
                new CreateIndexRequest(index).mapping(getIndexResponse.getMappings().get(liveIndex).sourceAsMap()),
                RequestOptions.DEFAULT);
        } else {
            for (CaseType caseType : CaseType.values()) {
                var index = getMigrationIndex(caseType.name());
                log.info("Creating new index {} for migration", index);
                client.indices().create(
                    new CreateIndexRequest(index).source(getIndexResponse.getMappings().get(liveIndex).sourceAsMap()),
                    RequestOptions.DEFAULT);
            }
        }
    }

    private Map<String, Object> mapCaseData(CaseData caseData) {
        Map<String, Object> indexMap = new HashMap<>();

        if (caseData.getReference() != null) {
            indexMap.put("reference", caseData.getReference());
        }
        if (caseData.getType() != null) {
            indexMap.put("type", caseData.getType());
        }
        if (caseData.getPrimaryTopicUUID() != null) {
            indexMap.put("primaryTopic", caseData.getPrimaryTopicUUID());
        }
        if (caseData.getPrimaryCorrespondentUUID() != null) {
            indexMap.put("primaryCorrespondent", caseData.getPrimaryCorrespondentUUID());
        }
        if (caseData.getCreated() != null) {
            indexMap.put("created", caseData.getCreated());
        }
        if (caseData.getDateReceived() != null) {
            indexMap.put("dateReceived", caseData.getDateReceived());
        }
        if (caseData.getCaseDeadline() != null) {
            indexMap.put("caseDeadline", caseData.getCaseDeadline());
        }

        indexMap.put("deleted", caseData.isDeleted());
        indexMap.put("caseUUID", caseData.getUuid());
        indexMap.put("data", caseData.getDataMap());
        indexMap.put("currentCorrespondents", mapCorrespondents(caseData.getCurrentCorrespondents()));
        indexMap.put("allCorrespondents", mapCorrespondents(caseData.getCorrespondents()));
        indexMap.put("currentTopics", mapTopics(caseData.getCurrentTopics()));
        indexMap.put("allTopics", mapTopics(caseData.getTopics()));
        indexMap.put("allSomuItems", mapSomuItems(caseData.getSomuItems()));

        return objectMapper.convertValue(indexMap, Map.class);
    }

    private List<Map<String, Object>> mapCorrespondents(Set<Correspondent> correspondents) {
        return correspondents.stream().map(correspondent -> {
            Map<String, Object> indexMap = new HashMap<>();
            indexMap.put("address1", correspondent.getAddress1());
            indexMap.put("address2", correspondent.getAddress2());
            indexMap.put("address3", correspondent.getAddress3());
            indexMap.put("country", correspondent.getCountry());
            indexMap.put("created", correspondent.getCreated());
            indexMap.put("externalKey", correspondent.getExternalKey());
            indexMap.put("fullname", correspondent.getFullName());
            indexMap.put("postcode", correspondent.getPostcode());
            indexMap.put("reference", correspondent.getReference());
            indexMap.put("telephone", correspondent.getTelephone());
            indexMap.put("email", correspondent.getEmail());
            indexMap.put("uuid", correspondent.getUuid());
            indexMap.put("type", correspondent.getCorrespondentType());
            return indexMap;
        }).toList();
    }

    private List<Map<String, Object>> mapSomuItems(Set<SomuItem> somuItems) {
        return somuItems.stream().map(somuItem -> {
            Map<String, Object> indexMap = new HashMap<>();
            indexMap.put("uuid", somuItem.getUuid());
            indexMap.put("somuTypeUuid", somuItem.getSomuUuid());
            indexMap.put("data", somuItem.getData());
            return indexMap;
        }).toList();
    }

    private List<Map<String, Object>> mapTopics(Set<Topic> topics) {
        return topics.stream().map(topic -> {
            Map<String, Object> indexMap = new HashMap<>();
            indexMap.put("uuid", topic.getTextUUID());
            indexMap.put("text", topic.getText());
            return indexMap;
        }).toList();

    }
}
