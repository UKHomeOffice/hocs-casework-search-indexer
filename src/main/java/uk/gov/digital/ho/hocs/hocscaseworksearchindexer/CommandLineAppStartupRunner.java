package uk.gov.digital.ho.hocs.hocscaseworksearchindexer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeExceptionMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain.exceptions.ElasticSearchFailureException;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain.services.EtlService;
import uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain.services.IndexService;

import java.io.IOException;

@Configuration
@Slf4j
@Profile("!test")
public class CommandLineAppStartupRunner implements CommandLineRunner {

    private final EtlService etlService;

    private final IndexService indexService;

    private final ApplicationContext applicationContext;

    public CommandLineAppStartupRunner(
        EtlService etlService,
        IndexService indexService,
        ApplicationContext applicationContext) {
        this.etlService = etlService;
        this.indexService = indexService;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) throws IOException, InterruptedException {
        log.warn("Waiting 20 seconds before starting migration (to allow for job to be cancelled if needed)...");
        Thread.sleep(20000);

        log.info("Migration started");
        indexService.createIndexes();
        etlService.migrate();
        log.info("Migration completed successfully, exiting");
        System.exit(SpringApplication.exit(applicationContext, () -> 0));
    }

    @Bean
    public ExitCodeExceptionMapper exceptionBasedExitCode() {
        return exception -> {
            if (exception.getCause() instanceof ElasticSearchFailureException) {
                return 2;
            }
            return 99;
        };
    }
}
