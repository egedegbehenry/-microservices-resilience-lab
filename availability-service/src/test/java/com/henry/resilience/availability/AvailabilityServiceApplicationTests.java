package com.henry.resilience.availability;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AvailabilityServiceApplicationTests {

    @org.springframework.beans.factory.annotation.Autowired
    org.springframework.context.ApplicationContext context;

    @Test
    void contextLoads() {
        org.junit.jupiter.api.Assertions.assertTrue(context.getBeansOfType(
                com.henry.resilience.availability.lab.LabOperation.class).isEmpty());
    }

}
