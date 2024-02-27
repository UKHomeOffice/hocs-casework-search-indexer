package uk.gov.digital.ho.hocs.hocscaseworksearchindexer.domain.casework.model;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import jakarta.persistence.*;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Convert(attributeName = "jsonb", converter = JsonBinaryType.class)
@NoArgsConstructor
@Entity
@Table(name = "somu_item")
@Getter
@Setter
@SuppressWarnings("JpaAttributeTypeInspection")
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", columnDefinition = "jsonb")
    private Map<String, Object> data = new HashMap<>(0);

    public boolean isDeleted() {
        return (data == null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}
        SomuItem that = (SomuItem) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

}
