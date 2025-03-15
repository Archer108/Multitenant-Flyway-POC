package com.poc.flyway.Multitenant_Flyway_POC.multitenant.testService;

import com.adp.benefits.carrier.entity.primary.PrimaryTestDb;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/primary-test-db")
@RequiredArgsConstructor
@Slf4j
public class PrimaryTestDbController {

    private final PrimaryTestDbService primaryTestDbService;

    @PostMapping("/save")
    public ResponseEntity<PrimaryTestDb> saveTestDb(@RequestBody PrimaryTestDb primaryTestDb) {
        log.info("📌 Received request to save TestDb: {}", primaryTestDb);
        return ResponseEntity.ok(primaryTestDbService.saveTestDb(primaryTestDb));
    }
}
