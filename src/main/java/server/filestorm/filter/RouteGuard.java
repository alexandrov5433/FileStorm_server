package server.filestorm.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.filestorm.model.type.CustomSession;
import server.filestorm.util.CustomHttpServletRequestWrapper;

@Order(2)
@Component
public class RouteGuard implements Filter {
    
    @Override
    public void doFilter(
        ServletRequest req,
        ServletResponse res,
        FilterChain filterChain
    )throws ServletException, IOException {
        CustomHttpServletRequestWrapper customReq = (CustomHttpServletRequestWrapper) req;
        HttpServletRequest httpReq = (HttpServletRequest) req;
        HttpServletResponse httpRes = (HttpServletResponse) res;
        String httpMethod = httpReq.getMethod().toUpperCase();
        String reqPath = httpReq.getRequestURI();


        HashMap<String, String> endpointMap = new HashMap<>();
        // Client - not protected
        // Authentication - not protected
        // FileSystem
        endpointMap.put("/api/file", "^GET|POST|DELETE$");
        endpointMap.put("/api/file/name/.+", "^PATCH$");
        endpointMap.put("/api/directory", "^GET|POST|DELETE$");
        // FileSharing
        endpointMap.put("/api/file/share_option/.+/.+", "^PATCH$");
        endpointMap.put("/api/users", "^GET$");
        endpointMap.put("/api/file/share_with_user/.+/.+", "^POST$");
        endpointMap.put("/api/file/shared_with_me", "^GET$");
        endpointMap.put("/api/file/me_sharing", "^GET$");
        endpointMap.put("/api/file/share_with/.+", "^GET$");

        Iterator<Entry<String, String>> iterator = endpointMap.entrySet().iterator();

        boolean isTargetPathSecure = false;
        while (iterator.hasNext()) {
            Entry<String, String> entry = iterator.next();
            String pathRegex = entry.getKey();
            String methodsRegex = entry.getValue();
            boolean pathMatch = reqPath.matches(pathRegex);
            boolean methodMatch = httpMethod.matches(methodsRegex);
            if (pathMatch && methodMatch) {
                isTargetPathSecure = true;
                break;
            }
        }

        if (isTargetPathSecure) {
            CustomSession session = customReq.getCustomSession();
            if (session == null ||
            session.getClaims() == null ||
            session.getUserId() == null ||
            session.getUsername() == null) {
                httpRes.setStatus(400);
                httpRes.setHeader("Content-Type", "application/json");
                httpRes.getWriter().write("{\"message\": \"Not authenticated.\", \"payload\": null}");
                return;
            }
        }
        filterChain.doFilter(req, res);
    }
}
