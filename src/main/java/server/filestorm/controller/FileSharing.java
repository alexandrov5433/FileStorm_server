package server.filestorm.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.async.DeferredResult;

import server.filestorm.exception.FileManagementException;
import server.filestorm.model.entity.Chunk;
import server.filestorm.model.entity.User;
import server.filestorm.model.entity.Chunk.ShareOption;
import server.filestorm.model.type.ApiResponse;
import server.filestorm.model.type.CustomSession;
import server.filestorm.model.type.fileManagement.ChunkReference;
import server.filestorm.service.ChunkService;
import server.filestorm.service.SharingService;
import server.filestorm.service.UserService;
import server.filestorm.util.CustomHttpServletRequestWrapper;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class FileSharing {

    @Autowired
    private UserService userService;

    @Autowired
    private ChunkService chunkService;

    @Autowired
    private SharingService sharingService;

    @PostMapping("/api/file/share_with_user/{fileId}/{userIdReceiver}")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> shareFileWithUser(
            @PathVariable Integer fileId,
            @PathVariable Integer userIdReceiver,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();
        CustomSession session = req.getCustomSession();

        // check path vars existance
        if (fileId == null || userIdReceiver == null) {
            throw new FileManagementException("FileId or userId is missing.");
        }

        Integer userId = session.getUserId();
        User user = userService.findById(userId);
        User userReceiver = userService.findById(userIdReceiver);
        Chunk chunk = chunkService.findChunkByIdAndOwner(fileId, user);

        // check if file is shareable
        if (chunk.getShareOption() == ShareOption.PRIVATE) {
            throw new FileManagementException(
                    "In order to share the file with another user, please change the sharing option to 'Share with user' first.");
        }

        boolean isDone = sharingService.shareFileWithUser(chunk, userReceiver);
        if (isDone) {
            res.setResult(ResponseEntity.ok()
                    .body(new ApiResponse<Boolean>("Shared with " + userReceiver.getUsername(), isDone)));
        } else {
            res.setResult(ResponseEntity.badRequest()
                    .body(new ApiResponse<Boolean>("Could not share file with " + userReceiver.getUsername(), isDone)));
        }
        return res;
    }

    @PatchMapping("/api/file/share_option/{fileId}/{newShareOption}")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> changeFileShareOptions(
            @PathVariable Integer fileId,
            @PathVariable String newShareOption,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();
        CustomSession session = req.getCustomSession();

        // check path vars existance
        if (fileId == null || newShareOption == null) {
            throw new FileManagementException("FileId or newShareOption is missing.");
        }
        newShareOption = newShareOption.toUpperCase();

        Integer userId = session.getUserId();
        User user = userService.findById(userId);
        Chunk chunk = chunkService.findChunkByIdAndOwner(fileId, user);

        // update share option in chunk
        ChunkReference updatedChunkRef = sharingService.updateChunkShareOption(chunk, newShareOption)
                .orElseThrow(() -> new FileManagementException("Could not update share option."));

        // do extra work for the given option
        switch (newShareOption) {
            case "PRIVATE":
                // remove all users from share_with
                // delete share_link
                updatedChunkRef = sharingService.deleteShareWithAndShareLink(chunk);
                break;
            case "SHARE_WITH_USER":
            case "SHARE_WITH_ALL_WITH_LINK":
                // remove all users from share_with
                // create share_link
                updatedChunkRef = sharingService.deleteShareWithAndCreateShareLink(chunk);
                break;
        }

        // return updated ChunkReference
        res.setResult(ResponseEntity.ok().body(new ApiResponse<>("Share option updated.", updatedChunkRef)));
        return res;
    }

    @GetMapping("/api/users")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> queryUsersByName(
            @RequestParam String username) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();
        if (username == null) {
            throw new FileManagementException("Username missing.");
        }
        LinkedHashMap<String, Integer> result = userService.queryUsersByName(username);
        // HashMap<String, Integer> result = userService.queryUsersByName(userName);

        res.setResult(ResponseEntity.ok().body(new ApiResponse<>("Queried users.", result)));
        return res;
    }

    @GetMapping("/api/file/shared_with_me")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> getFilesSharedWithMe(
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();
        CustomSession session = req.getCustomSession();

        Integer userId = session.getUserId();
        User user = userService.findById(userId);

        ArrayList<ChunkReference> chunkReferences = sharingService.getFilesSharedWithUser(user);

        res.setResult(ResponseEntity.ok()
                .body(new ApiResponse<ArrayList<ChunkReference>>("Files shared with me.", chunkReferences)));

        return res;
    }

    @GetMapping("/api/file/me_sharing")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> getAllFilesUserIsSharing(
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();
        CustomSession session = req.getCustomSession();

        Integer userId = session.getUserId();
        User user = userService.findById(userId);
        ArrayList<ChunkReference> refs = sharingService.getFilesUserIsSharing(user);

        res.setResult(ResponseEntity.ok()
                .body(new ApiResponse<ArrayList<ChunkReference>>("Files the user is sharing.", refs)));
        return res;
    }

    @GetMapping("/api/file/share_with/{fileId}")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> getShareWithForFileOfUser(
            @PathVariable Integer fileId,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();
        CustomSession session = req.getCustomSession();

        Integer userId = session.getUserId();
        User user = userService.findById(userId);
        Chunk chunk = chunkService.findChunkByIdAndOwner(fileId, user);

        LinkedHashMap<String, Integer> users = sharingService.getUsersFromShareWith(chunk);

        res.setResult(ResponseEntity.ok()
                .body(new ApiResponse<LinkedHashMap<String, Integer>>("Users with which the file is shared.", users)));

        return res;
    }
}
