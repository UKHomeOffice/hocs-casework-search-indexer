package uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain.casework.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain.casework.model.CaseData;

import javax.persistence.QueryHint;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Stream;

import static org.hibernate.jpa.QueryHints.*;

@Repository
public interface CaseRepository extends JpaRepository<CaseData, Integer> {

    @Query("SELECT DISTINCT cd FROM CaseData cd "
        + "LEFT JOIN FETCH cd.correspondents "
        + "LEFT JOIN FETCH cd.topics "
        + "LEFT JOIN FETCH cd.somuItems "
        + "WHERE cd.deleted = false "
        + "AND cd.created <= :endDate "
        + "AND cd.type IN :types "
        + "ORDER BY cd.type, cd.uuid")
    @QueryHints(value = { @QueryHint(name = HINT_FETCH_SIZE, value = "500"),
        @QueryHint(name = HINT_CACHEABLE, value = "false"), @QueryHint(name = HINT_READONLY, value = "true"),
        @QueryHint(name = HINT_PASS_DISTINCT_THROUGH, value = "false") })
    Stream<CaseData> getAllCasesAndCollectionsBefore(@Param("endDate") LocalDateTime endDate,
                                                     @Param("types") Set<String> types);

    @Query("SELECT DISTINCT cd FROM CaseData cd "
        + "LEFT JOIN FETCH cd.correspondents "
        + "LEFT JOIN FETCH cd.topics "
        + "LEFT JOIN FETCH cd.somuItems "
        + "WHERE cd.deleted = false "
        + "AND cd.type IN :types "
        + "ORDER BY cd.type, cd.uuid")
    @QueryHints(value = { @QueryHint(name = HINT_FETCH_SIZE, value = "500"),
        @QueryHint(name = HINT_CACHEABLE, value = "false"), @QueryHint(name = HINT_READONLY, value = "true"),
        @QueryHint(name = HINT_PASS_DISTINCT_THROUGH, value = "false") })
    Stream<CaseData> getAllCasesAndCollections(@Param("types") Set<String> types);

    @Query("SELECT DISTINCT cd FROM CaseData cd "
        + "LEFT JOIN FETCH cd.correspondents "
        + "LEFT JOIN FETCH cd.topics "
        + "LEFT JOIN FETCH cd.somuItems "
        + "WHERE cd.deleted = false "
        + "AND cd.created BETWEEN :startDate AND :endDate "
        + "AND cd.type IN :types "
        + "ORDER BY cd.type, cd.uuid")
    @QueryHints(value = { @QueryHint(name = HINT_FETCH_SIZE, value = "500"),
        @QueryHint(name = HINT_CACHEABLE, value = "false"), @QueryHint(name = HINT_READONLY, value = "true"),
        @QueryHint(name = HINT_PASS_DISTINCT_THROUGH, value = "false") })
    Stream<CaseData> getAllCasesAndCollectionsBetween(@Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate,
                                                     @Param("types") Set<String> types);

    @Query("SELECT DISTINCT cd FROM CaseData cd "
        + "LEFT JOIN FETCH cd.correspondents "
        + "LEFT JOIN FETCH cd.topics "
        + "LEFT JOIN FETCH cd.somuItems "
        + "WHERE cd.deleted = false "
        + "AND cd.created >= :startDate "
        + "AND cd.type IN :types "
        + "ORDER BY cd.type, cd.uuid")
    @QueryHints(value = { @QueryHint(name = HINT_FETCH_SIZE, value = "500"),
        @QueryHint(name = HINT_CACHEABLE, value = "false"), @QueryHint(name = HINT_READONLY, value = "true"),
        @QueryHint(name = HINT_PASS_DISTINCT_THROUGH, value = "false") })
    Stream<CaseData> getAllCasesAndCollectionsAfter(@Param("startDate") LocalDateTime createdDate,
                                                     @Param("types") Set<String> types);


}
