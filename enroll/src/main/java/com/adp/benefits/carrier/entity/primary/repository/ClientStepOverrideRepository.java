package com.adp.benefits.carrier.entity.primary.repository;

import com.adp.benefits.carrier.entity.primary.ClientStepOverride;
import com.adp.benefits.carrier.entity.primary.ClientStepOverrideId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientStepOverrideRepository
        extends JpaRepository<ClientStepOverride, ClientStepOverrideId> {

    // e.g. find a single override by composite key
    // Optional<ClientStepOverride> findById_ClientIdAndId_OverrideStepId(UUID clientId, String
    // overrideStepId);

}
