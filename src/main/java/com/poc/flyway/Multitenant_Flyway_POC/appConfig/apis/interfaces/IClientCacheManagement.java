package com.poc.flyway.Multitenant_Flyway_POC.appConfig.apis.interfaces;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/api")
public interface IClientCacheManagement {

    @Operation(summary = "Refresh tenant cache and reload database connections")
    @ApiResponses(
            value = {
                    @ApiResponse(responseCode = "200", description = "Tenant cache refreshed and database connections reloaded successfully"),
                    @ApiResponse(responseCode = "500", description = "An error occurred while refreshing the tenant cache or reloading database connections")
            })
    @PostMapping("/refresh-tenant-cache")
    ResponseEntity<String> refreshTenantCache();
}
