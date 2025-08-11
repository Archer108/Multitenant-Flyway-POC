package com.adp.benefits.carrier.entity.client.repository;

import com.adp.benefits.carrier.entity.client.Participant;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipantRepository extends JpaRepository<Participant, String> {

    /*    @Query("SELECT p from Participant p WHERE p.clientId IN :clientIds")
    List<Participant> findByClientIdIn(@Param("clientIds") List<String> clientIds);

    Optional<Participant> findFirstByAoid(String aoid);

    List<Participant> findByAoidIn(Collection<String> aoids);*/

    @Query("SELECT p from Participant p WHERE p.clientId IN :clientIds")
    List<Participant> findByClientIdIn(@Param("clientIds") List<String> clientIds);

    // NEW: by AOID (batch)
    @Query("SELECT p FROM Participant p WHERE p.aoid IN :aoids")
    List<Participant> findByAoidIn(@Param("aoids") Collection<String> aoids);
}
