package uk.gov.digital.ho.hocs.hocscaseworksearchindexer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CommandLineAppStartupRunner implements CommandLineRunner {

    private final ETLService ETLService;

    @Autowired
    public CommandLineAppStartupRunner(ETLService ETLService) {
        this.ETLService = ETLService;
    }

    @Override
    public void run(String...args) throws Exception {
           log.info("Application started with command-line arguments: {}", args);
           ETLService.migrate();
    }
}