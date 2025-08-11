package com.adp.benefits.carrier.entity.primary;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class ClientStepOverrideId implements Serializable {

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "override_step_id", nullable = false)
    private String overrideStepId;
}
