package com.adp.benefits.carrier.entity.client;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "plan_type", schema = "bcpm_client_schema")
public class PlanType {

    @Id
    @Column(name = "plan_type_id")
    private String planTypeId;

    @Column(name = "plan_name", nullable = false)
    private String planName;

    @OneToMany(mappedBy = "planType")
    @Builder.Default
    @ToString.Exclude
    private List<PlanTypeEnrollInfoAssoc> planTypeEnrollInfoAssocs = new ArrayList<>();
}
