package com.adp.benefits.carrier.entity.primary;

import jakarta.persistence.*;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "attribute", schema = "bcpm_primary_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"attributeLists", "procStepAttrAssociations"})
public class Attribute {

    @Id
    @Column(name = "attribute_id", nullable = false)
    private String attributeId;

    @Column(name = "collection_in", nullable = false)
    private Boolean collection;

    @OneToMany(
            mappedBy = "attribute",
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER,
            orphanRemoval = true)
    private List<AttributeList> attributeLists;

    @OneToMany(
            mappedBy = "attribute",
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER,
            orphanRemoval = true)
    private List<ProcStepAttrAssoc> procStepAttrAssociations;
}
