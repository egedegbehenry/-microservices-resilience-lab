package com.henry.resilience.booking;
import com.henry.resilience.booking.lab.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
 "resilience4j.ratelimiter.instances.availabilityCircuitBreaker.limitForPeriod=1",
 "resilience4j.ratelimiter.instances.availabilityCircuitBreaker.limitRefreshPeriod=1h"})
@ActiveProfiles("lab")
class RateLimitIntegrationTests {
 @Autowired LabOperation operation; @Autowired FaultPlan plan;
 @Value("${local.server.port}") int port;
 
    static final com.sun.net.httpserver.HttpServer downstream;
    static {
        try {
            downstream=com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1",0),0);
            downstream.createContext("/", exchange -> {
                byte[] body="AVAILABLE".getBytes(); exchange.sendResponseHeaders(200,body.length);
                exchange.getResponseBody().write(body); exchange.close();
            }); downstream.start();
        } catch(java.io.IOException e) { throw new ExceptionInInitializerError(e); }
    }
    @org.springframework.test.context.DynamicPropertySource
    static void downstream(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("services.availability.base-url", () -> "http://127.0.0.1:"+downstream.getAddress().getPort());
    }
    @org.junit.jupiter.api.AfterAll static void stop() { downstream.stop(0); }

 @Test void secondRequestIs429AndNeverReachesOperation() throws Exception {
    // Failure on first request is harmless here: the permit is consumed before retry execution.
    plan.configure(new FaultPlan.Plan("failure",0,0));
    assertThrows(RuntimeException.class,operation::execute);
    int before=plan.attempts();
    var response=java.net.http.HttpClient.newHttpClient().send(java.net.http.HttpRequest.newBuilder(
        java.net.URI.create("http://127.0.0.1:"+port+"/lab/operation")).GET().build(),java.net.http.HttpResponse.BodyHandlers.ofString());
    assertEquals(429,response.statusCode()); assertTrue(response.headers().firstValue("Retry-After").isPresent());
    assertEquals(before,plan.attempts());
 }
}
