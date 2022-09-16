package uk.gov.digital.ho.hocs.hocscaseworksearchindexer.casework.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.casework.model.CaseData;

import javax.persistence.QueryHint;
import java.util.stream.Stream;

import static org.hibernate.jpa.QueryHints.*;

@Repository
public interface CaseRepository extends JpaRepository<CaseData, Integer> {

    @Query("select distinct cd from CaseData cd " +
            "left join fetch cd.correspondents " +
            "left join fetch cd.topics " +
            "left join fetch cd.somuItems " +
            "where cd.deleted = false " +
            "order by cd.uuid")
    @QueryHints(value = {
            @QueryHint(name = HINT_FETCH_SIZE, value = "500"),
            @QueryHint(name = HINT_CACHEABLE, value = "false"),
            @QueryHint(name = HINT_READONLY, value = "true"),
            @QueryHint(name = HINT_PASS_DISTINCT_THROUGH, value = "false")
    })
    Stream<CaseData> getAllCasesAndCollections();

    }