package com.adp.benefits.carrier.entity.primary;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "process_client_assoc", schema = "bcpm_primary_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"processEntity", "client"})
public class ProcessClientAssoc {

    @EmbeddedId private ProcessClientAssocId id;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("processId")
    @JoinColumn(name = "process_id", nullable = false)
    private ProcessEntity processEntity;

    @ManyToOne(fetch = FetchType.EAGER)
    @MapsId("clientId")
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
}
