package com.adp.benefits.carrier.entity.primary.repository;

import com.adp.benefits.carrier.entity.primary.ProcessStep;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessStepRepository extends JpaRepository<ProcessStep, String> {

    @Query(
            value =
                    "SELECT * FROM bcpm_primary_schema.process_step WHERE process_id = :processId and step_order = :stepOrder LIMIT 1",
            nativeQuery = true)
    Optional<ProcessStep> findNextStep(
            @Param("processId") String processId, @Param("stepOrder") int stepOrder);


    /**
     * Map process_step_id -> system_type_tx for a batch of step ids.
     * Returned as rows [process_step_id, system_type_tx].
     */
    @Query(value = """
        SELECT ps.process_step_id, s.system_type_tx
        FROM bcpm_primary_schema.process_step ps
        JOIN bcpm_primary_schema.process p ON p.process_id = ps.process_id
        JOIN bcpm_primary_schema.system  s ON s.system_id  = p.system_id
        WHERE ps.process_step_id IN (:processStepIds)
        """, nativeQuery = true)
    List<Object[]> findSystemTypesByProcessStepIds(@Param("processStepIds") Collection<String> processStepIds);

    /**
     * Given system type names, return all process_step_id that belong to those system types.
     */
    @Query(value = """
        SELECT ps.process_step_id
        FROM bcpm_primary_schema.process_step ps
        JOIN bcpm_primary_schema.process p ON p.process_id = ps.process_id
        JOIN bcpm_primary_schema.system  s ON s.system_id  = p.system_id
        WHERE s.system_type_tx IN (:systemTypes)
        """, nativeQuery = true)
    Set<String> findProcessStepIdsBySystemTypes(@Param("systemTypes") Collection<String> systemTypes);




}
