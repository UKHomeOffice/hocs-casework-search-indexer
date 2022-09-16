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
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.casework.model.Correspondent;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.casework.model.SomuItem;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.casework.model.Topic;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.casework.repository.CaseRepository;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.casework.model.CaseData;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class ETLService {

    private int batchSize;

    private final CaseRepository caseRepository;

    private final RestHighLevelClient client;

    private ObjectMapper objectMapper;

    private final String index;

    @PersistenceContext
    protected EntityManager entityManager;

    public ETLService(@Value("${batch-size}") int batchSize,
                      CaseRepository caseRepository,
                      RestHighLevelClient client,
                      ObjectMapper objectMapper,
                      @Value("${aws.es.index-prefix}") String prefix) {
        this.batchSize = batchSize;
        this.caseRepository = caseRepository;
        this.client = client;
        this.objectMapper = objectMapper;
        this.index = String.format("%s-%s", prefix, "case");
    }

    @Transactional(readOnly = true)
    public void migrate() {

        AtomicInteger count = new AtomicInteger(0);
        try (Stream<CaseData> cases = caseRepository.getAllCasesAndCollections()) {
            Iterators.partition(cases.iterator(), batchSize).forEachRemaining(batch -> {
                BulkRequest bulkRequest = new BulkRequest();
                batch.forEach(caseData -> {
                    count.incrementAndGet();
                    log.info("Indexing case {} for UUID {}", count, caseData.getUuid());
                    entityManager.detach(caseData);
                    bulkRequest.add(
                        new IndexRequest(index).id(caseData.getUuid().toString()).source(mapCaseData(caseData)));
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
        }).collect(Collectors.toList());
    }

    private List<Map<String,Object>> mapSomuItems(Set<SomuItem> somuItems) {
        return somuItems.stream().map(somuItem -> {
            Map<String, Object> indexMap = new HashMap<>();
            indexMap.put("uuid", somuItem.getUuid());
            indexMap.put("somuTypeUuid", somuItem.getSomuUuid());
            indexMap.put("data", somuItem.getData());
            return indexMap;
        }).collect(Collectors.toList());
    }

    private List<Map<String,Object>> mapTopics(Set<Topic> topics) {
        return topics.stream().map(topic -> {
            Map<String, Object> indexMap = new HashMap<>();
            indexMap.put("uuid", topic.getTextUUID());
            indexMap.put("text", topic.getText());
            return indexMap;
        }).collect(Collectors.toList());

    }
}
