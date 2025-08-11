package com.adp.benefits.carrier.entity.primary.repository;

import com.adp.benefits.carrier.entity.primary.ProcStepAttrRuleAssoc;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcStepAttrRuleAssocRepository
        extends JpaRepository<ProcStepAttrRuleAssoc, String> {
    //    List<ProcStepAttrRuleAssoc> findByProcStepAttrAssoc_ProcStepAttrAssocIdAndEnabledIn(String
    // procStepAttrAssocId, boolean enabledIn);

    @Query(
            "SELECT r FROM ProcStepAttrRuleAssoc r WHERE r.procStepAttrAssoc.procStepAttrAssocId = :assocId AND r.enabledIn = :enabledIn")
    List<ProcStepAttrRuleAssoc> findByProcStepAttrAssocIdAndEnabledIn(
            @Param("assocId") String assocId, @Param("enabledIn") boolean enabledIn);
}
