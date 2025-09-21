package server.filestorm.filter.rateLimiter;

import java.util.HashMap;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RateIPLog {

    private HashMap<String, IPRequestData> ipLog = new HashMap<String, IPRequestData>();

    public void addData(String IP) {
        this.ipLog.put(IP, new IPRequestData(IP));
    }

    public IPRequestData findByIP(String IP) {
        return this.ipLog.get(IP);
    }

    // 1min = 60000ms
    @Scheduled(fixedDelay = 60000L)
    private void processRequestCount() {
        // set requestCount to 0 every 1 min
        this.ipLog.values().stream().forEach(IPRequestData::resetRequestCount);
    }

    // 10min = 600000ms
    @Scheduled(fixedDelay = 600000L)
    private void processBanned() {
        // unban banned IPs, if the ban duration is up
        this.ipLog.values().stream().forEach(IPRequestData::processBanned);
    }

    // 30min = 1800000ms
    @Scheduled(fixedDelay = 1800000L)
    private void processStale() {
        // remove stale entries from ipLog to free up memory
        this.ipLog.entrySet().removeIf(entry -> entry.getValue().isStale());
    }
    
}
