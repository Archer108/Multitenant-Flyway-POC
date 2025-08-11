package com.adp.benefits.carrier;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class GuidGeneratorTest {

    @Test
    public void shouldPrintGeneratedGuid() {
        UUID guid = UUID.randomUUID();
        System.out.println("Generated GUID: " + guid.toString());
    }
}
