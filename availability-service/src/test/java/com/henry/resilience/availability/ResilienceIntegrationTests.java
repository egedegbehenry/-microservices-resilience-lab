package com.henry.resilience.availability;
import com.henry.resilience.availability.lab.*;
import io.github.resilience4j.circuitbreaker.*;
import io.github.resilience4j.ratelimiter.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
 "resilience4j.circuitbreaker.instances.dependencyCircuitBreaker.waitDurationInOpenState=200ms",
 "resilience4j.ratelimiter.instances.dependencyCircuitBreaker.limitForPeriod=1000",
 "resilience4j.retry.instances.dependencyCircuitBreaker.waitDuration=1ms"})
@ActiveProfiles("lab")
class ResilienceIntegrationTests {
    @Autowired LabOperation operation;
    @Autowired FaultPlan plan;
    @Autowired CircuitBreakerRegistry breakers;
    @Autowired RateLimiterRegistry limits;
    @Value("${local.server.port}") int port;
    CircuitBreaker breaker;
    
    @BeforeEach void reset() {
        breaker=breakers.circuitBreaker("dependencyCircuitBreaker"); breaker.reset();
        plan.configure(new FaultPlan.Plan("success",0,0));
    }
    void failFour() {
        plan.configure(new FaultPlan.Plan("failure",0,0));
        for(int i=0;i<4;i++) assertThrows(RuntimeException.class,operation::execute);
        assertEquals(CircuitBreaker.State.OPEN,breaker.getState());
    }
    void awaitHalfOpen() throws Exception {
        long deadline=System.nanoTime()+java.time.Duration.ofSeconds(3).toNanos();
        while(breaker.getState()!=CircuitBreaker.State.HALF_OPEN && System.nanoTime()<deadline) Thread.sleep(10);
        assertEquals(CircuitBreaker.State.HALF_OPEN,breaker.getState());
    }
    @Test void annotationIsInterceptedAndRetryRecovers() {
        assertTrue(org.springframework.aop.support.AopUtils.isAopProxy(operation));
        plan.configure(new FaultPlan.Plan("transient",2,0));
        assertEquals("AVAILABLE",operation.execute()); assertEquals(3,plan.attempts());
        assertEquals(1,breaker.getMetrics().getNumberOfSuccessfulCalls());
    }
    @Test void openRejectsWithoutAttemptsThenRecovers() throws Exception {
        failFour(); assertEquals(12,plan.attempts());
        assertThrows(CallNotPermittedException.class,operation::execute); assertEquals(12,plan.attempts());
        plan.configure(new FaultPlan.Plan("success",0,0)); awaitHalfOpen();
        operation.execute(); assertEquals(CircuitBreaker.State.HALF_OPEN,breaker.getState());
        operation.execute(); assertEquals(CircuitBreaker.State.CLOSED,breaker.getState());
    }
    @Test void failedHalfOpenReturnsToOpen() throws Exception {
        failFour(); awaitHalfOpen();
        assertThrows(RuntimeException.class,operation::execute);
        assertThrows(RuntimeException.class,operation::execute);
        assertEquals(CircuitBreaker.State.OPEN,breaker.getState()); assertEquals(18,plan.attempts());
    }
    @Test void halfOpenRetryRecoveryCountsOneProbe() throws Exception {
        failFour(); awaitHalfOpen(); plan.configure(new FaultPlan.Plan("transient",2,0));
        operation.execute(); assertEquals(3,plan.attempts());
        assertEquals(CircuitBreaker.State.HALF_OPEN,breaker.getState());
        operation.execute(); assertEquals(CircuitBreaker.State.CLOSED,breaker.getState());
    }
    @Test void monitoringAndApiAreReachableOverHttp() throws Exception {
        var client=java.net.http.HttpClient.newHttpClient();
        for(String endpoint:new String[]{"/actuator/circuitbreakers","/actuator/circuitbreakerevents","/actuator/metrics","/lab/state","/lab/operation","/api/availability/check"}) {
            var response=client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+endpoint)).GET().build(),java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(200,response.statusCode(),endpoint+response.body());
            if(endpoint.equals("/actuator/circuitbreakers")) assertTrue(response.body().contains("dependencyCircuitBreaker"));
        }
    }
    @Test void simultaneousCallsAreCounted() throws Exception {
        try(var pool=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks=new java.util.ArrayList<java.util.concurrent.Future<String>>();
            for(int i=0;i<8;i++) tasks.add(pool.submit(operation::execute));
            for(var task:tasks) assertEquals("AVAILABLE",task.get());
        }
        assertEquals(8,plan.attempts());
    }

    @Test void halfOpenPermitsOnlyTwoConcurrentProbes() throws Exception {
        failFour(); awaitHalfOpen(); plan.configure(new FaultPlan.Plan("delay",0,200));
        try(var pool=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var gate=new java.util.concurrent.CountDownLatch(1);
            var tasks=new java.util.ArrayList<java.util.concurrent.Future<Boolean>>();
            for(int i=0;i<4;i++) tasks.add(pool.submit(() -> {
                gate.await();
                try { operation.execute(); return true; }
                catch(CallNotPermittedException e) { return false; }
            }));
            gate.countDown(); int permitted=0;
            for(var task:tasks) if(task.get()) permitted++;
            assertEquals(2,permitted); assertEquals(2,plan.attempts());
        }
        assertEquals(CircuitBreaker.State.CLOSED,breaker.getState());
    }
}
