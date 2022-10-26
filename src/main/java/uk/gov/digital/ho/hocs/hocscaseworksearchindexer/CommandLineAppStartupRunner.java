package uk.gov.digital.ho.hocs.hocscaseworksearchindexer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@Profile("!test")
public class CommandLineAppStartupRunner implements CommandLineRunner {

    private final ETLService ETLService;
    public CommandLineAppStartupRunner(ETLService ETLService) {
        this.ETLService = ETLService;
    }

    @Override
    public void run(String... args) throws IOException, InterruptedException {
        log.info("Application started");
        ETLService.migrate();
        log.info("Migration completed successfully, exiting");
        System.exit(0);
    }

}