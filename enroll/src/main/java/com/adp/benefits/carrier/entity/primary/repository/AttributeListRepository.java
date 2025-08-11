package com.adp.benefits.carrier.entity.primary.repository;

import com.adp.benefits.carrier.entity.primary.AttributeList;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AttributeListRepository extends JpaRepository<AttributeList, String> {

    @Query("SELECT al FROM AttributeList al JOIN al.attribute a WHERE a.attributeId = :attributeId")
    List<AttributeList> findByAttributeId(@Param("attributeId") String attributeId);
}
