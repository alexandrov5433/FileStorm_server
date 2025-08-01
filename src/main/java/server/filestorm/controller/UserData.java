package server.filestorm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import server.filestorm.model.entity.User;
import server.filestorm.model.type.ApiResponse;
import server.filestorm.model.type.CustomSession;
import server.filestorm.service.UserService;
import server.filestorm.thread.ThreadExecutorService;
import server.filestorm.util.CustomHttpServletRequestWrapper;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserData {
    @Autowired
    private UserService userService;

    @Autowired
    private ThreadExecutorService threadExecutorService;

    @GetMapping("/api/user-data/bytesInStorage")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> getBytesInStorage(CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();

        Runnable process = () -> {
            try {
                CustomSession session = req.getCustomSession();
                Long userId = session.getUserId();
                User user = userService.findById(userId);
                Long bytesInStorage = userService.getCurrentBytesInStorage(user);
                res.setResult(ResponseEntity.ok()
                        .body(new ApiResponse<Long>("Ok", bytesInStorage)));
            } catch (Exception e) {
                res.setErrorResult(e);
            }
        };

        threadExecutorService.execute(process);

        return res;
    }

    @GetMapping("/api/user-data/username")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> getUsername(@RequestParam Long userId) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();

        Runnable process = () -> {
            try {
                User user = userService.findById(userId);
                res.setResult(ResponseEntity.ok()
                        .body(new ApiResponse<String>("Username for this ID.", user.getUsername())));
            } catch (Exception e) {
                res.setErrorResult(e);
            }
        };

        threadExecutorService.execute(process);

        return res;
    }

}
