package uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain.casework.model;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.*;
import org.hibernate.annotations.Type;

import jakarta.persistence.*;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Convert(attributeName = "jsonb", converter = JsonBinaryType.class)
@NoArgsConstructor
@Entity
@Table(name = "case_data")
@Getter
@Setter
public class CaseData implements Serializable {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Id
    @Column(name = "uuid", columnDefinition = "uuid")
    private UUID uuid;

    @Column(name = "created")
    private LocalDateTime created = LocalDateTime.now();

    @Column(name = "type")
    private String type;

    @Column(name = "reference")
    private String reference;

    @Column(name = "deleted")
    private boolean deleted;

    @Type(value = UserType.class)
    @Column(name = "data", columnDefinition = "jsonb")
    private Map<String, String> dataMap = new HashMap<>(0);

    @Column(name = "primary_topic_uuid")
    private UUID primaryTopicUUID;

    @Column(name = "primary_correspondent_uuid")
    private UUID primaryCorrespondentUUID;

    @Column(name = "case_deadline")
    private LocalDate caseDeadline;

    @Column(name = "case_deadline_warning")
    private LocalDate caseDeadlineWarning;

    @Column(name = "date_received")
    private LocalDate dateReceived;

    @Column(name = "completed")
    private boolean completed;

    @Column(name = "migrated_reference")
    private String migratedReference;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "caseUUID", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Correspondent> correspondents = new LinkedHashSet<>();

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "caseUUID", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Topic> topics = new LinkedHashSet<>();

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "caseUuid", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SomuItem> somuItems = new LinkedHashSet<>();

    public Set<Correspondent> getCurrentCorrespondents() {
        return this.correspondents.stream().filter(c -> !c.isDeleted()).collect(Collectors.toSet());
    }

    public Set<Topic> getCurrentTopics() {
        return this.topics.stream().filter(t -> !t.isDeleted()).collect(Collectors.toSet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        CaseData that = (CaseData) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

}
