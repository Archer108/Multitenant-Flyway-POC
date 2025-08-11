package com.adp.benefits.carrier.entity.client.repository;

import com.adp.benefits.carrier.entity.client.PlanTypeEnrollInfoAssoc;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanTypeEnrollInfoAssocRepository
        extends JpaRepository<
                PlanTypeEnrollInfoAssoc, PlanTypeEnrollInfoAssoc.PlanTypeEnrollInfoAssocId> {

    @Query(value = """
        SELECT pso.process_step_outcome_id, pt.plan_name
        FROM bcpm_client_schema.process_step_outcome pso
        JOIN bcpm_client_schema.enrollment_info ei
          ON ei.process_step_outcome_id = pso.process_step_outcome_id
        JOIN bcpm_client_schema.plan_type_enroll_info_assoc pe
          ON pe.enroll_info_id = ei.enroll_info_id
        JOIN bcpm_client_schema.plan_type pt
          ON pt.plan_type_id = pe.plan_type_id
        WHERE pso.process_step_outcome_id IN (:psoIds)
        """, nativeQuery = true)
    List<Object[]> findPlanNamesByPsoIds(@Param("psoIds") Collection<String> psoIds);
}
