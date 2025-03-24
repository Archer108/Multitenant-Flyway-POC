package com.poc.flyway.Multitenant_Flyway_POC.appConfig.apis.testtmp;

import com.adp.benefits.carrier.entity.client.ClientTestDb;
import com.adp.benefits.carrier.entity.primary.PrimaryTestDb;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test-db")
@RequiredArgsConstructor
@Slf4j
public class TestDbController {

    private final ClientTestDbService clientTestDbService;

    private final PrimaryTestDbService primaryTestDbService;

    @PostMapping("/client/save")
    public ResponseEntity<ClientTestDb> saveTestDb(@RequestBody ClientTestDb clientTestDb) {
        log.info("📌 Received request to save TestDb: {}", clientTestDb);
        return ResponseEntity.ok(clientTestDbService.saveTestDb(clientTestDb));
    }

    @PostMapping("/primary/save")
    public ResponseEntity<PrimaryTestDb> saveTestDb(@RequestBody PrimaryTestDb primaryTestDb) {
        log.info("📌 Received request to save TestDb: {}", primaryTestDb);
        return ResponseEntity.ok(primaryTestDbService.saveTestDb(primaryTestDb));
    }


}
