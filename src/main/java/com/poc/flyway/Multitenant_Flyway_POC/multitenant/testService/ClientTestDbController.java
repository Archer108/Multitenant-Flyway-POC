package com.poc.flyway.Multitenant_Flyway_POC.multitenant.testService;

import com.adp.benefits.carrier.entity.client.ClientTestDb;
import com.adp.benefits.carrier.service.ClientTestDbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client-test-db")
@RequiredArgsConstructor
@Slf4j
public class ClientTestDbController {

    private final ClientTestDbService clientTestDbService;

    @PostMapping("/save")
    public ResponseEntity<ClientTestDb> saveTestDb(@RequestBody ClientTestDb clientTestDb) {
        log.info("📌 Received request to save TestDb: {}", clientTestDb);
        return ResponseEntity.ok(clientTestDbService.saveTestDb(clientTestDb));
    }
}