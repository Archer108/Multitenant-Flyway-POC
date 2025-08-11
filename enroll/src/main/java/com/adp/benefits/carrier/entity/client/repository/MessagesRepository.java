package com.adp.benefits.carrier.entity.client.repository;

import com.adp.benefits.carrier.entity.client.Message;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MessagesRepository extends JpaRepository<Message, String> {
    List<Message> findByProcessStepOutcomeId(String processStepOutcomeId);


    @Query(value = """
        SELECT *
        FROM bcpm_client_schema.message
        WHERE process_step_outcome_id IN (:psoIds)
        """, nativeQuery = true)
    List<Message> findByProcessStepOutcomeIdIn(@Param("psoIds") Collection<String> psoIds);
}
