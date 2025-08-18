package com.poc.flyway.Multitenant_Flyway_POC.appConfig.apis;

import com.adp.benefits.carrier.entity.client.ProcessStepOutcome;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ProcessStepOutcomeRepository extends JpaRepository<ProcessStepOutcome, String> {

    Optional<ProcessStepOutcome> findByCorrelationIdAndTransmissionId(
            String correlationId, String transmissionId);

    Optional<ProcessStepOutcome> findByTransmissionId(String transmissionId);

    Optional<ProcessStepOutcome> findByCorrelationId(String correlationId);

    @Query(
            value =
                    """
      SELECT DISTINCT p.client_id
      FROM bcpm_client_schema.process_step_outcome p
      LEFT JOIN bcpm_client_schema.enrollment_info ei ON ei.process_step_outcome_id = p.process_step_outcome_id
      LEFT JOIN bcpm_client_schema.participant     pa ON pa.aoid                  = p.aoid
      WHERE p.created_on_dt >= :fromTs AND p.created_on_dt < :toTs
        AND ( :hasCarrierId = FALSE OR ei.carrier_id = :carrierId )
        AND ( :state = '' OR p.state_tx = :state )
        AND ( :uiStatus = '' OR
              CASE
                WHEN p.status_tx = 'CONTINUE' THEN 'SUCCEEDED'
                WHEN p.status_tx = 'ASYNC' AND p.state_tx IN ('ONE_CG_ERROR','CARRIER_ERROR') THEN 'FAILED'
                WHEN p.status_tx = 'ASYNC' THEN 'SUCCEEDED'
                WHEN p.status_tx = 'IN_PROGRESS' THEN 'IN_REVIEW'
                WHEN p.status_tx IN ('STOP','TIMEOUT','INTERNAL_SERVER_ERROR') THEN 'FAILED'
                ELSE p.status_tx
              END = :uiStatus )
        AND ( :hasSystemTypes = FALSE OR p.system_type IN (:systemTypes) )
        AND ( :associateSearch = '' OR
              p.aoid        ILIKE ('%' || :associateSearch || '%') OR
              pa.first_name ILIKE ('%' || :associateSearch || '%') OR
              pa.last_name  ILIKE ('%' || :associateSearch || '%') )
        AND ( :globalSearch = '' OR
              p.aoid        ILIKE ('%' || :globalSearch || '%') OR
              pa.first_name ILIKE ('%' || :globalSearch || '%') OR
              pa.last_name  ILIKE ('%' || :globalSearch || '%') OR
              ei.carrier_id ILIKE ('%' || :globalSearch || '%') OR
              p.state_tx    ILIKE ('%' || :globalSearch || '%') OR
              p.status_tx   ILIKE ('%' || :globalSearch || '%') )
        AND ( :hasGlobalClientKeys = FALSE OR p.client_id IN (:globalClientKeys) )
      ORDER BY 1
      """,
            nativeQuery = true)
    Set<String> distinctClientIdsForDropdown(
            @Param("fromTs") Instant fromTs,
            @Param("toTs") Instant toTs,
            @Param("hasCarrierId") boolean hasCarrierId,
            @Param("carrierId") String carrierId,
            @Param("state") String state,
            @Param("uiStatus") String uiStatus,
            @Param("hasSystemTypes") boolean hasSystemTypes,
            @Param("systemTypes") Collection<String> systemTypes,
            @Param("associateSearch") String associateSearch,
            @Param("globalSearch") String globalSearch,
            @Param("hasGlobalClientKeys") boolean hasGlobalClientKeys,
            @Param("globalClientKeys") Collection<String> globalClientKeys);

    @Query(
            value =
                    """
      SELECT DISTINCT ei.carrier_id
      FROM bcpm_client_schema.process_step_outcome p
      LEFT JOIN bcpm_client_schema.enrollment_info ei ON ei.process_step_outcome_id = p.process_step_outcome_id
      LEFT JOIN bcpm_client_schema.participant     pa ON pa.aoid                  = p.aoid
      WHERE p.created_on_dt >= :fromTs AND p.created_on_dt < :toTs
        AND ( :hasClientIds = FALSE OR p.client_id IN (:clientIds) )
        AND ( :state = '' OR p.state_tx = :state )
        AND ( :uiStatus = '' OR
              CASE
                WHEN p.status_tx = 'CONTINUE' THEN 'SUCCEEDED'
                WHEN p.status_tx = 'ASYNC' AND p.state_tx IN ('ONE_CG_ERROR','CARRIER_ERROR') THEN 'FAILED'
                WHEN p.status_tx = 'ASYNC' THEN 'SUCCEEDED'
                WHEN p.status_tx = 'IN_PROGRESS' THEN 'IN_REVIEW'
                WHEN p.status_tx IN ('STOP','TIMEOUT','INTERNAL_SERVER_ERROR') THEN 'FAILED'
                ELSE p.status_tx
              END = :uiStatus )
        AND ( :hasSystemTypes = FALSE OR p.system_type IN (:systemTypes) )
        AND ( :associateSearch = '' OR
              p.aoid        ILIKE ('%' || :associateSearch || '%') OR
              pa.first_name ILIKE ('%' || :associateSearch || '%') OR
              pa.last_name  ILIKE ('%' || :associateSearch || '%') )
        AND ( :globalSearch = '' OR
              p.aoid        ILIKE ('%' || :globalSearch || '%') OR
              pa.first_name ILIKE ('%' || :globalSearch || '%') OR
              pa.last_name  ILIKE ('%' || :globalSearch || '%') OR
              ei.carrier_id ILIKE ('%' || :globalSearch || '%') OR
              p.state_tx    ILIKE ('%' || :globalSearch || '%') OR
              p.status_tx   ILIKE ('%' || :globalSearch || '%') )
        AND ( :hasGlobalClientKeys = FALSE OR p.client_id IN (:globalClientKeys) )
      ORDER BY 1
      """,
            nativeQuery = true)
    Set<String> distinctCarriersForDropdown(
            @Param("fromTs") Instant fromTs,
            @Param("toTs") Instant toTs,
            @Param("hasClientIds") boolean hasClientIds,
            @Param("clientIds") Collection<String> clientIds,
            @Param("state") String state,
            @Param("uiStatus") String uiStatus,
            @Param("hasSystemTypes") boolean hasSystemTypes,
            @Param("systemTypes") Collection<String> systemTypes,
            @Param("associateSearch") String associateSearch,
            @Param("globalSearch") String globalSearch,
            @Param("hasGlobalClientKeys") boolean hasGlobalClientKeys,
            @Param("globalClientKeys") Collection<String> globalClientKeys);

    @Query(
            value =
                    """
      SELECT p.*
      FROM bcpm_client_schema.process_step_outcome p
      LEFT JOIN bcpm_client_schema.enrollment_info ei ON ei.process_step_outcome_id = p.process_step_outcome_id
      LEFT JOIN bcpm_client_schema.participant     pa ON pa.aoid                  = p.aoid
      WHERE p.created_on_dt >= :fromTs AND p.created_on_dt < :toTs
        AND ( :hasClientIds = FALSE OR p.client_id IN (:clientIds) )
        AND ( :hasCarrierId = FALSE OR ei.carrier_id = :carrierId )
        AND ( :state = '' OR p.state_tx = :state )
        AND ( :uiStatus = '' OR
              CASE
                WHEN p.status_tx = 'CONTINUE' THEN 'SUCCEEDED'
                WHEN p.status_tx = 'ASYNC' AND p.state_tx IN ('ONE_CG_ERROR','CARRIER_ERROR') THEN 'FAILED'
                WHEN p.status_tx = 'ASYNC' THEN 'SUCCEEDED'
                WHEN p.status_tx = 'IN_PROGRESS' THEN 'IN_REVIEW'
                WHEN p.status_tx IN ('STOP','TIMEOUT','INTERNAL_SERVER_ERROR') THEN 'FAILED'
                ELSE p.status_tx
              END = :uiStatus )
        AND ( :hasSystemTypes = FALSE OR p.system_type IN (:systemTypes) )
        AND ( :associateSearch = '' OR
              p.aoid        ILIKE ('%' || :associateSearch || '%') OR
              pa.first_name ILIKE ('%' || :associateSearch || '%') OR
              pa.last_name  ILIKE ('%' || :associateSearch || '%') )
        AND ( :globalSearch = '' OR
              p.aoid        ILIKE ('%' || :globalSearch || '%') OR
              pa.first_name ILIKE ('%' || :globalSearch || '%') OR
              pa.last_name  ILIKE ('%' || :globalSearch || '%') OR
              ei.carrier_id ILIKE ('%' || :globalSearch || '%') OR
              p.state_tx    ILIKE ('%' || :globalSearch || '%') OR
              p.status_tx   ILIKE ('%' || :globalSearch || '%') )
        AND ( :hasGlobalClientKeys = FALSE OR p.client_id IN (:globalClientKeys) )
      ORDER BY p.created_on_dt DESC, p.process_step_outcome_id DESC
      LIMIT :limit
      """,
            nativeQuery = true)
    List<ProcessStepOutcome> findFirstPageInWindowFiltered(
            @Param("fromTs") Instant fromTs,
            @Param("toTs") Instant toTs,
            @Param("hasClientIds") boolean hasClientIds,
            @Param("clientIds") Collection<String> clientIds,
            @Param("hasCarrierId") boolean hasCarrierId,
            @Param("carrierId") String carrierId,
            @Param("state") String state,
            @Param("uiStatus") String uiStatus,
            @Param("hasSystemTypes") boolean hasSystemTypes,
            @Param("systemTypes") Collection<String> systemTypes,
            @Param("associateSearch") String associateSearch,
            @Param("globalSearch") String globalSearch,
            @Param("hasGlobalClientKeys") boolean hasGlobalClientKeys,
            @Param("globalClientKeys") Collection<String> globalClientKeys,
            @Param("limit") int limit);

    @Query(
            value =
                    """
      SELECT p.*
      FROM bcpm_client_schema.process_step_outcome p
      LEFT JOIN bcpm_client_schema.enrollment_info ei ON ei.process_step_outcome_id = p.process_step_outcome_id
      LEFT JOIN bcpm_client_schema.participant     pa ON pa.aoid                  = p.aoid
      WHERE p.created_on_dt >= :fromTs AND p.created_on_dt < :toTs
        AND ( :hasClientIds = FALSE OR p.client_id IN (:clientIds) )
        AND ( :hasCarrierId = FALSE OR ei.carrier_id = :carrierId )
        AND ( :state = '' OR p.state_tx = :state )
        AND ( :uiStatus = '' OR
              CASE
                WHEN p.status_tx = 'CONTINUE' THEN 'SUCCEEDED'
                WHEN p.status_tx = 'ASYNC' AND p.state_tx IN ('ONE_CG_ERROR','CARRIER_ERROR') THEN 'FAILED'
                WHEN p.status_tx = 'ASYNC' THEN 'SUCCEEDED'
                WHEN p.status_tx = 'IN_PROGRESS' THEN 'IN_REVIEW'
                WHEN p.status_tx IN ('STOP','TIMEOUT','INTERNAL_SERVER_ERROR') THEN 'FAILED'
                ELSE p.status_tx
              END = :uiStatus )
        AND ( :hasSystemTypes = FALSE OR p.system_type IN (:systemTypes) )
        AND ( :associateSearch = '' OR
              p.aoid        ILIKE ('%' || :associateSearch || '%') OR
              pa.first_name ILIKE ('%' || :associateSearch || '%') OR
              pa.last_name  ILIKE ('%' || :associateSearch || '%') )
        AND ( :globalSearch = '' OR
              p.aoid        ILIKE ('%' || :globalSearch || '%') OR
              pa.first_name ILIKE ('%' || :globalSearch || '%') OR
              pa.last_name  ILIKE ('%' || :globalSearch || '%') OR
              ei.carrier_id ILIKE ('%' || :globalSearch || '%') OR
              p.state_tx    ILIKE ('%' || :globalSearch || '%') OR
              p.status_tx   ILIKE ('%' || :globalSearch || '%') )
        AND ( :hasGlobalClientKeys = FALSE OR p.client_id IN (:globalClientKeys) )
        AND (
          p.created_on_dt < :cursorCreatedOn
          OR (p.created_on_dt = :cursorCreatedOn AND p.process_step_outcome_id < :cursorPsoId)
        )
      ORDER BY p.created_on_dt DESC, p.process_step_outcome_id DESC
      LIMIT :limit
      """,
            nativeQuery = true)
    List<ProcessStepOutcome> findNextPageInWindowFiltered(
            @Param("fromTs") Instant fromTs,
            @Param("toTs") Instant toTs,
            @Param("hasClientIds") boolean hasClientIds,
            @Param("clientIds") Collection<String> clientIds,
            @Param("hasCarrierId") boolean hasCarrierId,
            @Param("carrierId") String carrierId,
            @Param("state") String state,
            @Param("uiStatus") String uiStatus,
            @Param("hasSystemTypes") boolean hasSystemTypes,
            @Param("systemTypes") Collection<String> systemTypes,
            @Param("associateSearch") String associateSearch,
            @Param("globalSearch") String globalSearch,
            @Param("hasGlobalClientKeys") boolean hasGlobalClientKeys,
            @Param("globalClientKeys") Collection<String> globalClientKeys,
            @Param("cursorCreatedOn") Instant cursorCreatedOn,
            @Param("cursorPsoId") String cursorPsoId,
            @Param("limit") int limit);
}