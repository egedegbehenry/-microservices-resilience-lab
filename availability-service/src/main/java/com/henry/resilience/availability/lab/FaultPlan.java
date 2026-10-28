package com.henry.resilience.availability.lab;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
@Component @Profile("lab")
public class FaultPlan {
    private volatile Plan plan = new Plan("success", 0, 0);
    private final AtomicInteger attempts = new AtomicInteger();
    private final AtomicInteger remaining = new AtomicInteger();
    public record Plan(String mode, int failures, long delayMs) {}
    public synchronized void configure(Plan value) {
        if (value == null || value.mode() == null || !java.util.Set.of("success","failure","transient","delay").contains(value.mode())
                || value.failures()<0 || value.failures()>100 || value.delayMs()<0 || value.delayMs()>10000)
            throw new IllegalArgumentException("mode: success/failure/transient/delay; failures: 0..100; delayMs: 0..10000");
        plan=value; remaining.set(value.failures()); attempts.set(0);
    }
    public void invoke() {
        int attempt=attempts.incrementAndGet();
        Plan current=plan;
        org.slf4j.LoggerFactory.getLogger(getClass()).info("event=dependency_attempt attempt={} mode={}",attempt,current.mode());
        if (current.mode().equals("failure") || (current.mode().equals("transient") && remaining.getAndUpdate(n -> Math.max(0,n-1))>0))
            throw new com.henry.resilience.availability.exception.AvailabilityDependencyException("Injected dependency failure");
        if (current.mode().equals("delay")) try { Thread.sleep(current.delayMs()); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
    }
    public int attempts() { return attempts.get(); }
    public Plan plan() { return plan; }
}
