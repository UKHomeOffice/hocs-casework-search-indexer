package uk.gov.digital.ho.hocs.hocscaseworksearchindexer.casework.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@NoArgsConstructor
@Entity
@Table(name = "correspondent")
@Getter
@Setter
public class Correspondent implements Serializable {

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column(name = "uuid")
    protected UUID uuid;

    @Column(name = "created")
    protected LocalDateTime created;

    @Column(name = "type")
    protected String correspondentType;

    @Transient
    protected String correspondentTypeName;

    @Getter
    @Column(name = "case_uuid")
    protected UUID caseUUID;

    @Column(name = "fullname")
    protected String fullName;

    @Column(name = "organisation")
    protected String organisation;

    @Column(name = "postcode")
    protected String postcode;

    @Column(name = "address1")
    protected String address1;

    @Column(name = "address2")
    protected String address2;

    @Column(name = "address3")
    protected String address3;

    @Column(name = "country")
    protected String country;

    @Column(name = "telephone")
    protected String telephone;

    @Column(name = "email")
    protected String email;

    @Column(name = "external_key")
    protected String externalKey;

    @Column(name = "reference")
    protected String reference;

    @Column(name = "deleted")
    protected boolean deleted;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Correspondent that = (Correspondent) o;
        return uuid.equals(that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
}
