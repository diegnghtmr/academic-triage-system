package co.edu.uniquindio.triage.infrastructure.idempotency;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.idempotency")
public class IdempotencyProperties {

    private Ttl ttl = new Ttl();
    private Cleanup cleanup = new Cleanup();

    public Ttl getTtl() { return ttl; }
    public void setTtl(Ttl ttl) { this.ttl = ttl; }

    public Cleanup getCleanup() { return cleanup; }
    public void setCleanup(Cleanup cleanup) { this.cleanup = cleanup; }

    public static class Ttl {
        private int defaultDays = 7;
        private int aiDays = 1;

        public int getDefaultDays() { return defaultDays; }
        public void setDefaultDays(int defaultDays) { this.defaultDays = defaultDays; }

        public int getAiDays() { return aiDays; }
        public void setAiDays(int aiDays) { this.aiDays = aiDays; }
    }

    public static class Cleanup {
        private String schedule = "0 */10 * * * *";
        private int batchSize = 500;

        public String getSchedule() { return schedule; }
        public void setSchedule(String schedule) { this.schedule = schedule; }

        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
    }
}
