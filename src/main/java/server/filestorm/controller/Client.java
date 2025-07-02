package server.filestorm.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.request.async.DeferredResult;

import server.filestorm.service.ClientService;
import server.filestorm.thread.ThreadExecutorService;

@Controller
public class Client {

    @Autowired
    private ClientService clientService;

    @Autowired
    private ThreadExecutorService threadExecutorService;

    @GetMapping("/")
    public DeferredResult<ResponseEntity<?>> getClient() throws IOException {
        DeferredResult<ResponseEntity<?>> res = new DeferredResult<ResponseEntity<?>>();

        Runnable process = () -> {
            try {
                Resource clientHtml = clientService.loadClientHtmlAsResource();
                if (clientHtml == null) {
                    res.setResult(ResponseEntity.notFound().build());
                    return;
                }
                Long contentLength = clientHtml.contentLength();
                res.setResult(ResponseEntity
                        .ok()
                        .header("Content-Type", "text/html")
                        .header("Content-Length", Long.toString(contentLength))
                        .body(clientHtml));
            } catch (Exception e) {
                res.setResult(ResponseEntity.notFound().build());
            }
        };

        threadExecutorService.execute(process);
        
        return res;
    }

}
