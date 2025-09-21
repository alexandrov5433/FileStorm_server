package server.filestorm.filter.rateLimiter;

import java.io.IOException;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(1)
public class RateLimiter implements Filter {

  @Autowired
  private RateIPLog ipLog;

  public void doFilter(
      ServletRequest req,
      ServletResponse res,
      FilterChain filterChain) throws ServletException, IOException {

    HttpServletResponse httpRes = (HttpServletResponse) res;
    String IP = req.getRemoteAddr();

    IPRequestData client = ipLog.findByIP(IP);
    if (client == null) {
      ipLog.addData(IP);
      filterChain.doFilter(req, res);
    } else {
      if (client.isBanned()) {
        // 425 Too Early
        httpRes.setStatus(425);
        httpRes.setHeader("Content-Type", "application/json");
        httpRes.setHeader(
            "Retry-After",
            Long.toString(client.getBannedUntil() - new Date().getTime()));
        httpRes.getWriter().write("{\"message\": \"Too Early.\", \"payload\": null}");

      } else if (client.getRequestCount() > 1200) {
        // 429 Too Many Requests - 1200 requests per minute allowed
        client.setBannedUntil(new Date().getTime() + 60000L);
        httpRes.setStatus(429);
        httpRes.setHeader("Content-Type", "application/json");
        httpRes.setHeader("Retry-After", "60000"); // 1min
        httpRes.getWriter().write("{\"message\": \"Too Many Requests. 1min ban.\", \"payload\": null}");

      } else {
        client.updateLastRequestTimeToNow();
        client.incrementRequestCount();
        filterChain.doFilter(req, res);
      }
    }
  }
}
