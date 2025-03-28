package com.poc.flyway.Multitenant_Flyway_POC.appConfig.apis.service;

public interface IClientCacheManagementService {

    void refreshTenantCache() throws Exception;
}
