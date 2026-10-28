package com.henry.resilience.booking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BookingServiceApplicationTests {

    @org.springframework.beans.factory.annotation.Autowired
    org.springframework.context.ApplicationContext context;

    @Test
    void contextLoads() {
        org.junit.jupiter.api.Assertions.assertTrue(context.getBeansOfType(
                com.henry.resilience.booking.lab.LabOperation.class).isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(org.springframework.aop.support.AopUtils.isAopProxy(
                context.getBean(com.henry.resilience.booking.service.BookingService.class)));
    }

}
