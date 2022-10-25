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
    public void run(String... args) throws IOException {
        log.info("Application started");

        for (int i = 0; i < args.length; ++i) {
           log.info("args[{}]: {}", i, args[i]);
        }
        ETLService.migrate();
        log.info("Migration completed successfully");
        System.exit(0);
    }

}