package com.adp.benefits.carrier.model;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public abstract class Auditable {

    @Column(name = "author")
    private String author;

    @Column(name = "created_on_dt", nullable = false, updatable = false)
    @NotNull(message = "Creation Date is required")
    private Date createDt;

    @Column(name = "modified_on_dt")
    @NotNull(message = "Modified Date is required")
    private Date modifyDt;

    @Version
    @Column(name = "version_num")
    private Integer version;
}
