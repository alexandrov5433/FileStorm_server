package server.filestorm.filter;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.filestorm.service.FileSystemService;

@Order(1)
@Component
public class NonExistentPathHandler implements Filter {

    Logger logger = LoggerFactory.getLogger(NonExistentPathHandler.class);

    @Autowired
    private FileSystemService fileSystemService;

    @Override
    public void doFilter(
            ServletRequest req,
            ServletResponse res,
            FilterChain filterChain) throws ServletException, IOException {
        HttpServletRequest httpReq = (HttpServletRequest) req;
        HttpServletResponse httpRes = (HttpServletResponse) res;

        String reqPath = httpReq.getRequestURI();

        Pattern pattern = Pattern
                .compile("^(/api.*|/.*\\.(js|css|png|jpg|jpeg|svg|ico|woff|woff[0-9]*|json|csv|txt|webp))$", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(reqPath);

        if (!matcher.matches() && !reqPath.equals("/")) {
            httpRes.setStatus(200);
            httpRes.setHeader("Content-Type", "text/html;charset=UTF-8");

            try (ServletOutputStream strout = httpRes.getOutputStream();
                    BufferedOutputStream buffout = new BufferedOutputStream(strout);) {

                fileSystemService.streamFileStormIndexHtmlToClient(buffout);

            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            return;
        }

        filterChain.doFilter(httpReq, httpRes);
    }
}
