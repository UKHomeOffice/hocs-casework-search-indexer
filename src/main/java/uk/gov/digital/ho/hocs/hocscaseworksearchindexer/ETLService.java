package uk.gov.digital.ho.hocs.hocscaseworksearchindexer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterators;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.casework.repository.CaseRepository;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.casework.model.CaseData;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Slf4j
@Service
public class ETLService {

    private final CaseRepository caseRepository;

    private final RestHighLevelClient client;

    private ObjectMapper objectMapper;

    private final String index;

    @PersistenceContext
    protected EntityManager entityManager;

    public ETLService(CaseRepository caseRepository, RestHighLevelClient client, ObjectMapper objectMapper, @Value("${aws.es.index-prefix}") String prefix) {
        this.caseRepository = caseRepository;
        this.client = client;
        this.objectMapper = objectMapper;
        this.index = String.format("%s-%s", prefix, "case");
    }

    @Transactional(readOnly = true)
    public void migrate() {

        AtomicInteger count = new AtomicInteger(0);
        try (Stream<CaseData> cases = caseRepository.getAllCasesAndCollections()) {
            Iterators.partition(cases.iterator(), 200).forEachRemaining(batch -> {
                BulkRequest bulkRequest = new BulkRequest();
                batch.forEach(caseData -> {
                    count.incrementAndGet();
                    log.info("Indexing case {} for UUID {}", count, caseData.getUuid());
                    entityManager.detach(caseData);
                    bulkRequest.add(
                            new IndexRequest(index)
                                    .id(caseData.getUuid().toString())
                                    .source(mapCaseData(caseData)));
                });

                try {
                    client.bulk(bulkRequest, RequestOptions.DEFAULT);
                } catch (IOException e) {
                    log.error("Error indexing batch", e);
                }
            });
        }
    }

    private Map<String, Object> mapCaseData(CaseData caseData) {
        Map<String, Object> indexMap = new HashMap<>();

        indexMap.put("uuid", caseData.getUuid());

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
        indexMap.put("completed", caseData.isCompleted());
        indexMap.put("data", caseData.getDataMap());
        indexMap.put("currentCorrespondents", caseData.getCurrentCorrespondents());
        indexMap.put("allCorrespondents", caseData.getCorrespondents());
        indexMap.put("currentTopics", caseData.getCurrentTopics());
        indexMap.put("allTopics", caseData.getTopics());
        indexMap.put("allSomuItems", caseData.getSomuItems());

        return objectMapper.convertValue(indexMap, Map.class);
    }


}
