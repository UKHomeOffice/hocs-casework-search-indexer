package uk.gov.digital.ho.hocs.hocscaseworksearchindexer.integration;

import net.javacrumbs.jsonunit.core.Option;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlConfig.TransactionMode.ISOLATED;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = { "app.mode=MULTIPLE" })
@ActiveProfiles({ "localstack", "test" })
@Sql(scripts = "classpath:case/beforeTest.sql", config = @SqlConfig(transactionMode = ISOLATED))
@Sql(scripts = "classpath:case/afterTest.sql",
     config = @SqlConfig(transactionMode = ISOLATED),
     executionPhase = AFTER_TEST_METHOD)
class MigrateCaseDataMultiIndexTest {

    @Autowired
    private RestHighLevelClient highLevelClient;

    @Autowired
    private IndexService indexService;

    @Autowired
    private EtlService etlService;

    @MockBean
    private CaseTypeComponent caseTypeComponent;

    @BeforeAll
    static void setup(@Autowired IndexService indexService) throws IOException {
        indexService.createIndexes();
    }

    @BeforeEach
    void setup() {
        when(caseTypeComponent.getTypes()).thenReturn(Set.of("TEST", "TESTA", "TESTB"));
    }

    @Test
    void createsCaseDocumentFromCaseData() throws IOException {
        String expectedJSON = Files.readString(
            new ClassPathResource("elastic-search/case-data.json").getFile().toPath());

        etlService.migrate();

        var getRequest = new GetRequest(indexService.getIndexName("test"), "14915b78-6977-42db-b343-0915a7f412a1");
        var getResponse = highLevelClient.get(getRequest, RequestOptions.DEFAULT);
        var responseJson = getResponse.getSourceAsString();

        assertThatJson(responseJson).when(Option.IGNORING_ARRAY_ORDER).isEqualTo(expectedJSON);
    }

    @Test()
    void updatesCaseTypeIndexInMultipleMode() throws IOException {
        etlService.migrate();

        var testIndexResponse = highLevelClient.get(
            new GetRequest(indexService.getIndexName("test"), "14915b78-6977-42db-b343-0915a7f412a1"),
            RequestOptions.DEFAULT);
        assertThat(testIndexResponse.getSourceAsMap()).containsEntry("caseUUID", "14915b78-6977-42db-b343-0915a7f412a1");

        var testAIndexResponse = highLevelClient.get(
            new GetRequest(indexService.getIndexName("testa"), "24915b78-6977-42db-b343-0915a7f412a1"),
            RequestOptions.DEFAULT);
        assertThat(testAIndexResponse.getSourceAsMap()).containsEntry("caseUUID", "24915b78-6977-42db-b343-0915a7f412a1");

        var testBIndexResponse = highLevelClient.get(
            new GetRequest(indexService.getIndexName("testb"), "34915b78-6977-42db-b343-0915a7f412a1"),
            RequestOptions.DEFAULT);
        assertThat(testBIndexResponse.getSourceAsMap()).containsEntry("caseUUID", "34915b78-6977-42db-b343-0915a7f412a1");
    }

}
