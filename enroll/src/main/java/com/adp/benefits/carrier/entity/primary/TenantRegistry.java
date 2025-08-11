package com.adp.benefits.carrier.entity.primary;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tenant_registry", schema = "bcpm_primary_schema")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantRegistry {

    @Id
    @Column(name = "tenant_id")
    private String tenantID;

    @Column(name = "schema_tx", nullable = false)
    private String schema;

    @Column(name = "database_tx", nullable = false)
    private String database;

    @Column(name = "connection_tx", nullable = false)
    private String connection;

    @Column(name = "real_client", nullable = false)
    private boolean realClient;

    @Column(name = "test_data", nullable = false)
    private boolean testData;
}
