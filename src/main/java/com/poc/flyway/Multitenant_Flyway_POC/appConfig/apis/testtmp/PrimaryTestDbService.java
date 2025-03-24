package com.poc.flyway.Multitenant_Flyway_POC.appConfig.apis.testtmp;

import com.adp.benefits.carrier.entity.primary.PrimaryTestDb;
import com.adp.benefits.carrier.entity.primary.repository.PrimaryTestDbRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrimaryTestDbService {

    private final PrimaryTestDbRepository primaryTestDbRepository;

    public PrimaryTestDb saveTestDb(PrimaryTestDb primaryTestDb) {
        return primaryTestDbRepository.save(primaryTestDb);
    }
}
