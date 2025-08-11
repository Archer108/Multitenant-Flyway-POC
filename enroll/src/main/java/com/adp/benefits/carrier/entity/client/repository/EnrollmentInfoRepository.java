package com.adp.benefits.carrier.entity.client.repository;

import com.adp.benefits.carrier.entity.client.EnrollmentInfo;
import com.adp.benefits.carrier.entity.client.ProcessStepOutcome;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EnrollmentInfoRepository extends JpaRepository<EnrollmentInfo, String> {

    EnrollmentInfo findByProcessStepOutcome(ProcessStepOutcome processStepOutcome);


    @Query(value = """
      SELECT DISTINCT ei.carrier_id
      FROM bcpm_client_schema.process_step_outcome p
      JOIN bcpm_client_schema.enrollment_info ei
        ON ei.process_step_outcome_id = p.process_step_outcome_id
      LEFT JOIN bcpm_client_schema.participant pa
        ON pa.aoid = p.aoid
      WHERE p.created_on_dt >= :fromTs AND p.created_on_dt < :toTs
        AND ( :clientId = '' OR p.client_id = :clientId )
        AND ( :state    = '' OR p.state_tx  = :state )
        AND ( :status   = '' OR p.status_tx = :status )
        AND ( :hasProcessSteps = FALSE OR p.process_step_id IN (:processStepIds) )
        AND ( :associateSearch = '' OR
              p.aoid ILIKE ('%' || :associateSearch || '%') OR
              pa.first_name ILIKE ('%' || :associateSearch || '%') OR
              pa.last_name  ILIKE ('%' || :associateSearch || '%') )
        AND ( :globalSearch = '' OR
              p.aoid        ILIKE ('%' || :globalSearch || '%') OR
              pa.first_name ILIKE ('%' || :globalSearch || '%') OR
              pa.last_name  ILIKE ('%' || :globalSearch || '%') OR
              ei.carrier_id ILIKE ('%' || :globalSearch || '%') OR
              p.state_tx    ILIKE ('%' || :globalSearch || '%') OR
              p.status_tx   ILIKE ('%' || :globalSearch || '%') )
      ORDER BY 1
      """, nativeQuery = true)
    List<String> findDistinctCarriersFiltered(
            @Param("fromTs") Instant fromTs,
            @Param("toTs") Instant toTs,
            @Param("clientId") String clientId,
            @Param("state") String state,
            @Param("status") String status,
            @Param("hasProcessSteps") boolean hasProcessSteps,
            @Param("processStepIds") Collection<String> processStepIds,
            @Param("associateSearch") String associateSearch,
            @Param("globalSearch") String globalSearch
    );


    @Query(value = """
        SELECT ei.*
        FROM bcpm_client_schema.enrollment_info ei
        WHERE ei.process_step_outcome_id IN (:psoIds)
        """, nativeQuery = true)
    List<EnrollmentInfo> findByProcessStepOutcomeIds(@Param("psoIds") Collection<String> psoIds);

    // Distinct carriers (optionally scoped by client for dependent dropdowns)
    @Query(value = """
        SELECT DISTINCT ei.carrier_id
        FROM bcpm_client_schema.enrollment_info ei
        """, nativeQuery = true)
    List<String> findDistinctCarriers();

    @Query(value = """
        SELECT DISTINCT ei.carrier_id
        FROM bcpm_client_schema.enrollment_info ei
        JOIN bcpm_client_schema.process_step_outcome pso
          ON pso.process_step_outcome_id = ei.process_step_outcome_id
        WHERE (:clientId IS NULL OR pso.client_id = :clientId)
        """, nativeQuery = true)
    List<String> findDistinctCarriersByClient(@Param("clientId") String clientId);

    // Distinct clients (optionally scoped by carrier for dependent dropdowns)
    @Query(value = """
        SELECT DISTINCT pso.client_id
        FROM bcpm_client_schema.process_step_outcome pso
        JOIN bcpm_client_schema.enrollment_info ei
          ON ei.process_step_outcome_id = pso.process_step_outcome_id
        WHERE (:carrierId IS NULL OR ei.carrier_id = :carrierId)
        """, nativeQuery = true)
    List<String> findDistinctClientsByCarrier(@Param("carrierId") String carrierId);
}
