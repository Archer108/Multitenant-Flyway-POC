package com.adp.benefits.carrier.entity.client;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "enrollment_info", schema = "bcpm_client_schema")
@EqualsAndHashCode(exclude = {"planTypeEnrollInfoAssocs", "processStepOutcome"})
public class EnrollmentInfo {

    @Id
    @Column(name = "enroll_info_id")
    private String enrollInfoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_step_outcome_id", nullable = false, unique = true)
    private ProcessStepOutcome processStepOutcome;

    @Column(name = "carrier_id", nullable = false)
    private String carrierId;

    @OneToMany(mappedBy = "enrollmentInfo")
    @Builder.Default
    @ToString.Exclude
    private Set<PlanTypeEnrollInfoAssoc> planTypeEnrollInfoAssocs = new HashSet<>();
}
