package com.adp.benefits.carrier.entity.primary;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "system", schema = "bcpm_primary_schema")
@EqualsAndHashCode(exclude = {"processEntities"})
public class SystemEntity {

    @Id
    @Column(name = "system_id", nullable = false)
    private String systemId;

    @Column(name = "name_tx", nullable = false)
    @NotNull(message = "System name cannot be null")
    private String name;

    @Column(name = "system_type_tx", nullable = false)
    @NotNull(message = "System Type is required")
    private String systemType;

    @OneToMany(
            mappedBy = "systemEntity",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    private List<ProcessEntity> processEntities;
}
