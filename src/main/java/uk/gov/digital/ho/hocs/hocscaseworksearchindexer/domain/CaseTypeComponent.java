package uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CaseTypeComponent {

    private final Set<String> types;

    public CaseTypeComponent(@Value("${app.migrate.types}") Set<String> types) {
        if (!types.isEmpty()) {
            this.types = Arrays.stream(CaseType.values()).map(
                CaseType::name).filter(types::contains).collect(
                Collectors.toSet());
        } else {
            this.types = Arrays.stream(CaseType.values()).map(
                CaseType::name).collect(Collectors.toSet());
        }

        log.info("Requested Types: {}", types);
        log.info("Matched Types: {}", this.types);
    }

    public Set<String> getTypes() {
        return types;
    }

    private enum CaseType {
        MIN,
        TRO,
        DTEN,
        POGR,
        POGR2,
        MTS,
        MPAM,
        WCS,
        COMP,
        COMP2,
        IEDET,
        SMC,
        BF,
        TO,
        BF2,
        FOI
    }
}
