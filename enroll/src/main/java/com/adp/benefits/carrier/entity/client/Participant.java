package com.adp.benefits.carrier.entity.client;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "participant", schema = "bcpm_client_schema")
public class Participant {

    @Id
    @Column(name = "participant_id")
    private String participantId;

    @Column(name = "client_id", nullable = false)
    @NotNull(message = "Client ID is required")
    private String clientId;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "ssn")
    private String ssn;

    @Column(name = "job_id")
    private String jobId;

    @Column(name = "participant_type_tx")
    private String participantType;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "aoid")
    private String aoid;
}
