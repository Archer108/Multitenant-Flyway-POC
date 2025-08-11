package com.adp.benefits.carrier.entity.primary.repository;

import com.adp.benefits.carrier.entity.primary.Client;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientRepository extends JpaRepository<Client, java.util.UUID> {

    @Query(
            value =
                    """
        SELECT oid
        FROM bcpm_primary_schema.client
        WHERE name_tx = ANY(:names)
        """,
            nativeQuery = true)
    List<String> findOidsByNameIn(@Param("names") List<String> names);

    @Query(
            value =
                    """
        SELECT oid
        FROM bcpm_primary_schema.client
        WHERE name_tx ILIKE CONCAT('%', :name, '%')
        """,
            nativeQuery = true)
    List<String> findOidsByNameLike(@Param("name") String name);
}
