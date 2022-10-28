package uk.gov.digital.ho.hocs.hocscaseworksearchindexer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeExceptionMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

@Configuration
@Slf4j
@Profile("!test")
public class CommandLineAppStartupRunner implements CommandLineRunner {

    private final ETLService ETLService;

    private final ApplicationContext applicationContext;

    public CommandLineAppStartupRunner(ETLService ETLService, ApplicationContext applicationContext) {
        this.ETLService = ETLService;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) throws IOException, InterruptedException {
        log.info("Application started");
        ETLService.migrate();
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