package com.henry.resilience.availability;
import com.henry.resilience.availability.lab.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
 "resilience4j.ratelimiter.instances.dependencyCircuitBreaker.limitForPeriod=1",
 "resilience4j.ratelimiter.instances.dependencyCircuitBreaker.limitRefreshPeriod=1h"})
@ActiveProfiles("lab")
class RateLimitIntegrationTests {
 @Autowired LabOperation operation; @Autowired FaultPlan plan;
 @Value("${local.server.port}") int port;
 
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
