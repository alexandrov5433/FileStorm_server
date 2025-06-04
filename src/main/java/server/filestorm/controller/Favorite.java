package server.filestorm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.request.async.DeferredResult;

import server.filestorm.model.entity.User;
import server.filestorm.model.type.ApiResponse;
import server.filestorm.model.type.CustomSession;
import server.filestorm.model.type.fileManagement.ChunkReference;
import server.filestorm.service.ChunkService;
import server.filestorm.service.UserService;
import server.filestorm.util.CustomHttpServletRequestWrapper;

@Controller
public class Favorite {

    @Autowired
    private UserService userService;

    @Autowired
    private ChunkService chunkService;

    @GetMapping("/api/favorite")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> getMethodName(
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();
        CustomSession session = req.getCustomSession();

        Integer userId = session.getUserId();
        User user = userService.findById(userId);

        ChunkReference[] favorites = chunkService.getFavoritesForUser(user);

        res.setResult(ResponseEntity.ok().body(new ApiResponse<ChunkReference[]>("Favorite files.", favorites)));
        
        return res;
    }

}
