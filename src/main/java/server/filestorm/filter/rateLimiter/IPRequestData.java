package server.filestorm.filter.rateLimiter;

import java.util.Date;

public class IPRequestData {
    private String IP;
    private int requestCount;
    private Long bannedUntil = null;
    private Long lastRequestTime;

    public IPRequestData(String IP) {
        this.IP = IP;
        this.requestCount = 1;
        this.lastRequestTime = new Date().getTime();
    }

    public String getIP() {
        return this.IP;
    }

    public int getRequestCount() {
        return this.requestCount;
    }

    public void incrementRequestCount() {
        this.requestCount++;
    }

    public void resetRequestCount() {
        this.requestCount = 0;
    }

    public Boolean isBanned() {
        return this.bannedUntil != null;
    }

    public Boolean isStale() {
        if (isBanned()) {
            return false;
        }
        long timeSinceLastRequest = new Date().getTime() - this.lastRequestTime;
        // 2h = 7200000ms
        return timeSinceLastRequest >= 7200000L;
    }

    public long getBannedUntil() {
        return this.bannedUntil;
    }

    public void setBannedUntil(long time) {
        this.bannedUntil = time;
    }

    /**
     * Checks if the IP is banned and unbans it - sets requestCont to 0 and bannedUntil to null - if the ban duration has passed.
     */
    public void processBanned() {
        if (this.bannedUntil != null) {
            if (this.bannedUntil <= new Date().getTime()) {
                this.bannedUntil = null;
                this.requestCount = 0;
            }
        }
    }

    public long getLastRequestTime() {
        return this.lastRequestTime;
    }

    public void updateLastRequestTimeToNow() {
        this.lastRequestTime = new Date().getTime();
    }
}
