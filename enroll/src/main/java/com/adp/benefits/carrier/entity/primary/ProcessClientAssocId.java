package com.adp.benefits.carrier.entity.primary;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ProcessClientAssocId implements Serializable {

    @Column(name = "process_id", nullable = false)
    private String processId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;
}
