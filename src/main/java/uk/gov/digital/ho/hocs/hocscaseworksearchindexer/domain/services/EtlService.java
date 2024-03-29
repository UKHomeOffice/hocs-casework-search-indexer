package uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterators;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain.CaseTypeComponent;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain.exceptions.ElasticSearchFailureException;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain.casework.model.Correspondent;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain.casework.model.SomuItem;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain.casework.model.Topic;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain.casework.repository.CaseRepository;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain.casework.model.CaseData;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Slf4j
@Service
public class EtlService {

    private final RestHighLevelClient client;

    private final ObjectMapper objectMapper;

    private final CaseRepository caseRepository;

    private final IndexService indexService;

    private final CaseTypeComponent caseTypeComponent;

    private final int batchSize;

    private final int batchInterval;

    private final LocalDateTime startDate;

    private final LocalDateTime endDate;

    private final int startingOffset;

    @PersistenceContext
    protected EntityManager entityManager;

    public EtlService(RestHighLevelClient client,
                      ObjectMapper objectMapper,
                      CaseRepository caseRepository,
                      IndexService indexService,
                      CaseTypeComponent caseTypeComponent,
                      @Value("${app.migrate.batch.size}") int batchSize,
                      @Value("${app.migrate.batch.interval}") int batchInterval,
                      @Value("${app.migrate.startDate}") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
                      @Value("${app.migrate.endDate}") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
                      @Value("${app.migrate.offset}") int startingOffset) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.caseRepository = caseRepository;
        this.indexService = indexService;
        this.caseTypeComponent = caseTypeComponent;
        this.batchSize = batchSize;
        this.batchInterval = batchInterval;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startingOffset = startingOffset;

        log.info("Batch Size: {}", batchSize);
        log.info("Batch Interval: {}", batchInterval);
        log.info("Start Date: {}", startDate);
        log.info("End Date: {}", endDate);
        log.info("Starting Offset: {}", startingOffset);
    }

    @Transactional(readOnly = true)
    public void migrate() {
        AtomicInteger count = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);

        Set<String> caseTypes = caseTypeComponent.getTypes();
        int offset = caseTypes.size() == 1 ? startingOffset : 0;

        try (Stream<CaseData> cases = getCaseDataStream(caseTypes).skip(offset)) {
            Iterators.partition(cases.iterator(), batchSize).forEachRemaining(batch -> {
                BulkRequest bulkRequest = new BulkRequest();
                batch.forEach(caseData -> {
                    count.incrementAndGet();
                    successCount.incrementAndGet();

                    log.debug("Indexing case {} for UUID {}", count, caseData.getUuid());
                    entityManager.detach(caseData);

                    bulkRequest.add(new UpdateRequest(indexService.getIndexName(caseData.getType()), caseData.getUuid().toString())
                        .docAsUpsert(true)
                        .doc(mapCaseData(caseData)));
                });

                try {
                    BulkResponse bulkItemResponses = client.bulk(bulkRequest, RequestOptions.DEFAULT);

                    if (bulkItemResponses.hasFailures()) {
                        Arrays.stream(bulkItemResponses.getItems())
                            .filter(BulkItemResponse::isFailed)
                            .forEach(i-> {
                                successCount.getAndDecrement();
                                log.error("Failed to index case {} with error {} in index {}", i.getId(), i.getFailureMessage(), i.getIndex());
                            });
                    }

                    Thread.sleep(batchInterval);
                } catch (IOException | InterruptedException e) {
                    log.error("Error indexing batch {}", e.getMessage());
                    throw new ElasticSearchFailureException("Failed to index batch", e);
                }

                log.info("Indexed {} cases", count);
            });

            assert successCount.get() == count.get();
        }
    }

    private Stream<CaseData> getCaseDataStream(Set<String> caseTypes) {
        if (startDate != null && StringUtils.hasText(startDate.toString())) {
            if (endDate != null && StringUtils.hasText(endDate.toString())) {
                return caseRepository.getAllCasesAndCollectionsBetween(startDate, endDate, caseTypes);
            } else {
                return caseRepository.getAllCasesAndCollectionsAfter(startDate, caseTypes);
            }
        } else {
            if (endDate != null && StringUtils.hasText(endDate.toString())) {
                return caseRepository.getAllCasesAndCollectionsBefore(endDate, caseTypes);
            } else {
                return caseRepository.getAllCasesAndCollections(caseTypes);
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
        if (caseData.getMigratedReference() != null) {
            indexMap.put("migratedReference", caseData.getMigratedReference());
        }

        indexMap.put("deleted", caseData.isDeleted());
        indexMap.put("completed", caseData.isCompleted());
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
