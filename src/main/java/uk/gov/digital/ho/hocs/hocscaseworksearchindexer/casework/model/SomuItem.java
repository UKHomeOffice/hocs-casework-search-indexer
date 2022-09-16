package uk.gov.digital.ho.hocs.hocscaseworksearchindexer.casework.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
@NoArgsConstructor
@Entity
@Table(name = "somu_item")
@Getter
@Setter
public class SomuItem implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid")
    private UUID uuid;

    @Column(name = "case_uuid")
    private UUID caseUuid;

    @Column(name = "somu_uuid")
    private UUID somuUuid;

    @Type(type = "jsonb")
    @Column(name = "data", columnDefinition = "jsonb")
    private Map<String, Object> data = new HashMap<>(0);

    public boolean isDeleted() {
        return (data == null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SomuItem that = (SomuItem) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

}
