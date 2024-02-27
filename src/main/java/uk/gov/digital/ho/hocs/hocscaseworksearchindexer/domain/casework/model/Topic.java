package uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain.casework.model;

import lombok.*;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@NoArgsConstructor
@Entity
@Table(name = "topic")
@Getter
@Setter
public class Topic implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid")
    private UUID uuid;

    @Column(name = "created")
    private LocalDateTime created;

    @Column(name = "case_uuid")
    private UUID caseUUID;

    @Column(name = "text")
    private String text;

    @Column(name = "text_uuid")
    private UUID textUUID;

    @Column(name = "deleted")
    private boolean deleted;

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        Topic that = (Topic) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

}
