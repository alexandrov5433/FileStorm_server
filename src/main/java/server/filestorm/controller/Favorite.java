package server.filestorm.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.request.async.DeferredResult;

import server.filestorm.exception.FileManagementException;
import server.filestorm.model.entity.Chunk;
import server.filestorm.model.entity.User;
import server.filestorm.model.type.ApiResponse;
import server.filestorm.model.type.CustomSession;
import server.filestorm.model.type.fileManagement.ChunkReference;
import server.filestorm.service.ChunkService;
import server.filestorm.service.UserService;
import server.filestorm.util.CustomHttpServletRequestWrapper;
import org.springframework.web.bind.annotation.PostMapping;

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

    @PostMapping("/api/favorite/{fileId}")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> markFileAsFavorite(
            @PathVariable Integer fileId,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();
        CustomSession session = req.getCustomSession();

        Integer userId = session.getUserId();
        User user = userService.findById(userId);

        // check file existance and ownership
        if (fileId == null) {
            throw new FileManagementException("File ID is required.");
        }
        Chunk chunk = chunkService.findChunkByIdAndOwner(fileId, user);
        chunkService.markChunkAsFavorite(chunk);

        res.setResult(ResponseEntity.ok().body(new ApiResponse<>("File marked as favorite.")));

        return res;
    }

    @DeleteMapping("/api/favorite/{fileId}")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> removeFileFromFavorite(
            @PathVariable Integer fileId,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();
        CustomSession session = req.getCustomSession();

        Integer userId = session.getUserId();
        User user = userService.findById(userId);

        // check file existance and ownership
        if (fileId == null) {
            throw new FileManagementException("File ID is required.");
        }
        Chunk chunk = chunkService.findChunkByIdAndOwner(fileId, user);
        chunkService.removeChunkFromFavorite(chunk);

        res.setResult(ResponseEntity.ok().body(new ApiResponse<>("File removed from favorite.")));

        return res;
    }
}
