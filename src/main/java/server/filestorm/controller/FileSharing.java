package server.filestorm.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.async.DeferredResult;

import server.filestorm.exception.FileManagementException;
import server.filestorm.model.entity.Chunk;
import server.filestorm.model.entity.User;
import server.filestorm.model.entity.Chunk.ShareOption;
import server.filestorm.model.type.ApiResponse;
import server.filestorm.model.type.CustomSession;
import server.filestorm.model.type.authentication.UserReference;
import server.filestorm.model.type.fileManagement.ChunkReference;
import server.filestorm.service.ChunkService;
import server.filestorm.service.FileSystemService;
import server.filestorm.service.SharingService;
import server.filestorm.service.UserService;
import server.filestorm.thread.ThreadExecutorService;
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

    @Autowired
    private FileSystemService fileSystemService;

    @Autowired
    private ThreadExecutorService threadExecutorService;

    @GetMapping("/api/file-sharing/share_with")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> getShareWithForFileOfUser(
            @RequestParam Long fileId,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();

        Runnable process = () -> {
            CustomSession session = req.getCustomSession();
    
            Long userId = session.getUserId();
            User user = userService.findById(userId);
            Chunk chunk = chunkService.findChunkByIdAndOwner(fileId, user);
    
            LinkedHashMap<String, Long> users = sharingService.getUsersFromShareWith(chunk);
    
            res.setResult(ResponseEntity.ok()
                    .body(new ApiResponse<LinkedHashMap<String, Long>>("Users with which the file is shared.", users)));
        };

        threadExecutorService.execute(process);

        return res;
    }

    @PostMapping("/api/file-sharing/share_with")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> shareFileWithUser(
            @RequestParam Long fileId,
            @RequestParam Long userIdReceiver,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();

        Runnable process = () -> {
            CustomSession session = req.getCustomSession();
    
            // check path vars existance
            if (fileId == null || userIdReceiver == null) {
                throw new FileManagementException("FileId or userId is missing.");
            }
    
            Long userId = session.getUserId();
            User user = userService.findById(userId);
    
            // check if the user is trying to share the file with him self
            if (userId == userIdReceiver) {
                throw new FileManagementException("You can not share a file with your self.");
            }
    
            User userReceiver = userService.findById(userIdReceiver);
            Chunk chunk = chunkService.findChunkByIdAndOwner(fileId, user);
    
            // check if file is shareable
            if (chunk.getShareOption() == ShareOption.PRIVATE) {
                throw new FileManagementException(
                        "In order to share the file with another user, please change the sharing option to 'Share with user' first.");
            }
    
            sharingService.shareFileWithUser(chunk, userReceiver);
    
            res.setResult(ResponseEntity.ok()
                    .body(new ApiResponse<UserReference>("Shared with " + userReceiver.getUsername() + ".",
                            new UserReference(userReceiver))));
        };

        threadExecutorService.execute(process);

        return res;
    }

    @DeleteMapping("/api/file-sharing/share_with")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> removeUserFromShareWith(
            @RequestParam Long fileId,
            @RequestParam Long userIdReceiver,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();

        Runnable process = () -> {
            CustomSession session = req.getCustomSession();
    
            // check path vars existance
            if (fileId == null || userIdReceiver == null) {
                throw new FileManagementException("FileId or userId is missing.");
            }
    
            Long userId = session.getUserId();
            User user = userService.findById(userId);
            User userReceiver = userService.findById(userIdReceiver);
            Chunk chunk = chunkService.findChunkByIdAndOwner(fileId, user);
    
            chunkService.removeUserFromShareWith(chunk, userReceiver);
    
            res.setResult(ResponseEntity.ok()
                    .body(new ApiResponse<>("User " + userReceiver.getUsername() + " was removed from the share list.")));
        };

        threadExecutorService.execute(process);

        return res;
    }

    @PatchMapping("/api/file-sharing/share_option")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> changeFileShareOptions(
            @RequestParam Long fileId,
            @RequestParam String newShareOption,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();

        Runnable process = () -> {
            CustomSession session = req.getCustomSession();
    
            // check path vars existance
            if (fileId == null || newShareOption == null) {
                throw new FileManagementException("FileId or newShareOption is missing.");
            }
            String _newShareOption = newShareOption.toUpperCase();
    
            Long userId = session.getUserId();
            User user = userService.findById(userId);
            Chunk chunk = chunkService.findChunkByIdAndOwner(fileId, user);
    
            // update share option in chunk
            sharingService.updateChunkShareOption(chunk, _newShareOption);
    
            // do extra work for the given option
            switch (_newShareOption) {
                case "PRIVATE":
                case "SHARE_WITH_USER":
                    // remove all users from share_with
                    // delete share_link
                    sharingService.deleteShareWithAndShareLink(chunk);
                    break;
                case "SHARE_WITH_ALL_WITH_LINK":
                    // remove all users from share_with
                    // create share_link
                    sharingService.deleteShareWithAndCreateShareLink(chunk);
                    break;
            }
    
            // return updated ChunkReference
            res.setResult(ResponseEntity.ok()
                    .body(new ApiResponse<ChunkReference>("Share option updated.", new ChunkReference(chunk))));
        };

        threadExecutorService.execute(process);

        return res;
    }

    @GetMapping("/api/users")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> queryUsersByName(
            @RequestParam String username,
            @RequestParam Long fileIdToShare,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();

        Runnable process = () -> {
            CustomSession session = req.getCustomSession();
    
            if (username == null) {
                throw new FileManagementException("Username missing.");
            }
    
            Long userId = session.getUserId();
            User user = userService.findById(userId);
            String userRequesterToExcludeFromSearch = session.getUsername();
    
            Chunk fileToShare = chunkService.findChunkByIdAndOwner(fileIdToShare, user);
    
            LinkedHashMap<String, Long> result = userService.queryUsersByNameForFileSharing(username, userRequesterToExcludeFromSearch, fileToShare);
    
            res.setResult(
                    ResponseEntity.ok().body(new ApiResponse<LinkedHashMap<String, Long>>("Queried users.", result)));
        };

        threadExecutorService.execute(process);

        return res;
    }

    @GetMapping("/api/file-sharing/shared_with_me")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> getFilesSharedWithMe(
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();

        Runnable process = () -> {
            CustomSession session = req.getCustomSession();
    
            Long userId = session.getUserId();
            User user = userService.findById(userId);
    
            ArrayList<ChunkReference> chunkReferences = sharingService.getFilesSharedWithUser(user);
    
            res.setResult(ResponseEntity.ok()
                    .body(new ApiResponse<ArrayList<ChunkReference>>("Files shared with me.", chunkReferences)));
        };

        threadExecutorService.execute(process);

        return res;
    }

    @GetMapping("/api/file-sharing/me_sharing")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> getAllFilesUserIsSharing(
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();

        Runnable process = () -> {
            CustomSession session = req.getCustomSession();
    
            Long userId = session.getUserId();
            User user = userService.findById(userId);
            ArrayList<ChunkReference> refs = sharingService.getFilesUserIsSharing(user);
    
            res.setResult(ResponseEntity.ok()
                    .body(new ApiResponse<ArrayList<ChunkReference>>("Files the user is sharing.", refs)));
        };

        threadExecutorService.execute(process);

        return res;
    }

    @GetMapping("/api/file-sharing/file")
    public DeferredResult<ResponseEntity<?>> downloadFile(
            @RequestParam Long fileId,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<?>> res = new DeferredResult<>();

        Runnable process = () -> {
            CustomSession session = req.getCustomSession();
    
            Long userId = session.getUserId();
            User user = userService.findById(userId);
            Chunk sharedChunk = chunkService.findChunkSharedWithUser(fileId, user);
    
            Resource file = fileSystemService.loadAsResource(sharedChunk);
            res.setResult(ResponseEntity.ok()
                    .header("Content-Type", sharedChunk.getMimeType())
                    .body(file));
        };

        threadExecutorService.execute(process);

        return res;
    }
}
