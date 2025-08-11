package com.adp.benefits.carrier.entity.primary;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import lombok.*;

@Entity
@Table(name = "attribute_list", schema = "bcpm_primary_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"attribute"})
public class AttributeList implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attribute_list_id")
    private Integer attributeListId;

    @Column(name = "attribute_name_tx", nullable = false)
    @NotNull(message = "Attribute name cannot be null")
    private String attributeName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "attribute_id", nullable = false)
    private Attribute attribute;
}
