package com.adp.benefits.carrier.entity.client;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "plan_type_enroll_info_assoc", schema = "bcpm_client_schema")
@IdClass(PlanTypeEnrollInfoAssoc.PlanTypeEnrollInfoAssocId.class)
public class PlanTypeEnrollInfoAssoc {

    @Id
    @Column(name = "plan_type_id")
    private String planTypeId;

    @Id
    @Column(name = "enroll_info_id")
    private String enrollInfoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "plan_type_id",
            referencedColumnName = "plan_type_id",
            insertable = false,
            updatable = false)
    private PlanType planType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "enroll_info_id",
            referencedColumnName = "enroll_info_id",
            insertable = false,
            updatable = false)
    private EnrollmentInfo enrollmentInfo;

    /** Composite primary key class for PlanTypeEnrollInfoAssoc */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlanTypeEnrollInfoAssocId implements Serializable {
        private static final long serialVersionUID = 1L;

        private String planTypeId;
        private String enrollInfoId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PlanTypeEnrollInfoAssocId that = (PlanTypeEnrollInfoAssocId) o;
            return Objects.equals(planTypeId, that.planTypeId)
                    && Objects.equals(enrollInfoId, that.enrollInfoId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(planTypeId, enrollInfoId);
        }
    }
}
