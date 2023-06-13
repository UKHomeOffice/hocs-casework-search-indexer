package uk.gov.digital.ho.hocs.hocscaseworksearchindexer.integration;

import net.javacrumbs.jsonunit.core.Option;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain.CaseTypeComponent;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain.services.EtlService;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain.services.IndexService;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlConfig.TransactionMode.ISOLATED;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = { "app.mode=SINGULAR" })
@ActiveProfiles({ "localstack", "test" })
@Sql(scripts = "classpath:case/beforeTest.sql", config = @SqlConfig(transactionMode = ISOLATED))
@Sql(scripts = "classpath:case/afterTest.sql",
     config = @SqlConfig(transactionMode = ISOLATED),
     executionPhase = AFTER_TEST_METHOD)
class MigrateCaseDataTest {

    @Autowired
    private RestHighLevelClient highLevelClient;

    @Autowired
    private EtlService etlService;

    @Autowired
    private IndexService indexService;

    @MockBean
    private CaseTypeComponent caseTypeComponent;

    @BeforeAll
    static void setup(@Autowired IndexService indexService) throws IOException {
        indexService.createIndexes();
    }

    @BeforeEach
    void setup() {
        when(caseTypeComponent.getTypes()).thenReturn(Set.of("TEST"));
    }

    @Test
    void createsCaseDocumentFromCaseData() throws IOException {
        String expectedJSON =  Files.readString(new ClassPathResource("elastic-search/case-data.json").getFile().toPath());

        etlService.migrate();

        var getRequest = new GetRequest(indexService.getIndexName(null), "14915b78-6977-42db-b343-0915a7f412a1");
        var getResponse = highLevelClient.get(getRequest, RequestOptions.DEFAULT);
        var responseJson = getResponse.getSourceAsString();

        assertThatJson(responseJson).when(Option.IGNORING_ARRAY_ORDER).isEqualTo(expectedJSON);
    }
}
