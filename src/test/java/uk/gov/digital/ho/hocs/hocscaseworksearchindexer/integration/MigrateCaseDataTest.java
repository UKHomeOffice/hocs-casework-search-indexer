package uk.gov.digital.ho.hocs.hocscaseworksearchindexer.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.javacrumbs.jsonunit.core.Option;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.ETLService;
import java.io.IOException;
import java.nio.file.Files;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.SqlConfig.TransactionMode.ISOLATED;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = { "mode=SINGULAR" })
@ActiveProfiles({ "localstack", "test" })
@Sql(scripts = "classpath:case/beforeTest.sql", config = @SqlConfig(transactionMode = ISOLATED))
@Sql(scripts = "classpath:case/afterTest.sql",
     config = @SqlConfig(transactionMode = ISOLATED),
     executionPhase = AFTER_TEST_METHOD)
class MigrateCaseDataTest {

    @Autowired
    private RestHighLevelClient highLevelClient;

    @Autowired
    private ETLService etlService;

    @Value("${aws.es.index-prefix}")
    private String prefix;
    @Test
    void createsCaseDocumentFromCaseData() throws IOException, InterruptedException {

        String expectedJSON =  Files.readString(new ClassPathResource("elastic-search/case-data.json").getFile().toPath());

        etlService.migrate();

        var getRequest = new GetRequest(String.format("%s-case", prefix), "14915b78-6977-42db-b343-0915a7f412a1");
        var getResponse = highLevelClient.get(getRequest, RequestOptions.DEFAULT);
        var responseJson = getResponse.getSourceAsString();

        assertThatJson(responseJson).when(Option.IGNORING_ARRAY_ORDER).isEqualTo(expectedJSON);
    }
}
