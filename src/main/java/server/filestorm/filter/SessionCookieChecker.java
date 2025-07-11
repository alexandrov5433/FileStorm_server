package server.filestorm.filter;

import java.io.IOException;
import java.util.Map;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import server.filestorm.model.type.CustomSession;
import server.filestorm.util.CustomHttpServletRequestWrapper;
import server.filestorm.util.JwtUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Order(2)
@Component
public class SessionCookieChecker implements Filter {

    private JwtUtil jwtUtil;

    public SessionCookieChecker(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void doFilter(
        ServletRequest request,
        ServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        HttpServletRequest req = (HttpServletRequest) request;
        CustomHttpServletRequestWrapper customReq = new CustomHttpServletRequestWrapper(req);
        Cookie[] cookies = customReq.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals("FileStormUserSession")) {
                    Map<String, Object> claims = jwtUtil.extractAllClaims(cookies[i].getValue());
                    CustomSession customSession = claims == null ? null : new CustomSession(claims);
                    customReq.setCustomSession(customSession);
                    break;
                }
            }
        }
        filterChain.doFilter(customReq, response);
    }
}
