package com.adp.benefits.carrier.entity.primary;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "client", schema = "bcpm_primary_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"processClientAssocs", "clientStepOverrides"})
public class Client {

    @Id
    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "tenant_id", nullable = false)
    private TenantRegistry tenantRegistry;

    @Column(name = "name_tx", nullable = false)
    @NotNull(message = "Client name cannot be null")
    private String name;

    @Column(name = "oid", nullable = false)
    @NotNull(message = "OID cannot be null")
    private String oid;

    @OneToMany(
            mappedBy = "client",
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER,
            orphanRemoval = true)
    private Set<ProcessClientAssoc> processClientAssocs;

    @OneToMany(
            mappedBy = "client",
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER,
            orphanRemoval = true)
    private Set<ClientStepOverride> clientStepOverrides;
}
