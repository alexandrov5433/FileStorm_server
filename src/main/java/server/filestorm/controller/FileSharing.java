package server.filestorm.controller;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import server.filestorm.exception.FileManagementException;
import server.filestorm.exception.StorageException;
import server.filestorm.model.entity.Chunk;
import server.filestorm.model.entity.User;
import server.filestorm.model.entity.Chunk.ShareOption;
import server.filestorm.model.type.ApiResponse;
import server.filestorm.model.type.BulkManipulationData;
import server.filestorm.model.type.CustomSession;
import server.filestorm.model.type.authentication.UserReference;
import server.filestorm.model.type.fileManagement.ChunkReference;
import server.filestorm.service.ChunkService;
import server.filestorm.service.FileSystemService;
import server.filestorm.service.SharingService;
import server.filestorm.service.UserService;
import server.filestorm.thread.ThreadExecutorService;
import server.filestorm.util.CustomHttpServletRequestWrapper;
import server.filestorm.util.StringUtil;

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

    private Logger logger = LoggerFactory.getLogger(FileSharing.class);

    @GetMapping("/api/file-sharing/share_with")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> getShareWithForFileOfUser(
            @RequestParam Long fileId,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();

        Runnable process = () -> {
            try {
                CustomSession session = req.getCustomSession();

                Long userId = session.getUserId();
                User user = userService.findById(userId);
                Chunk chunk = chunkService.findChunkByIdAndOwner(fileId, user);

                LinkedHashMap<String, Long> users = sharingService.getUsersFromShareWith(chunk);

                res.setResult(ResponseEntity.ok()
                        .body(new ApiResponse<LinkedHashMap<String, Long>>("Users with which the file is shared.",
                                users)));
            } catch (Exception e) {
                res.setErrorResult(e);
            }
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
            try {
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
            } catch (Exception e) {
                res.setErrorResult(e);
            }
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
            try {
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
                        .body(new ApiResponse<>(
                                "User " + userReceiver.getUsername() + " was removed from the share list.")));
            } catch (Exception e) {
                res.setErrorResult(e);
            }
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
            try {
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
            } catch (Exception e) {
                res.setErrorResult(e);
            }
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
            try {
                CustomSession session = req.getCustomSession();

                if (username == null) {
                    throw new FileManagementException("Username missing.");
                }

                Long userId = session.getUserId();
                User user = userService.findById(userId);
                String userRequesterToExcludeFromSearch = session.getUsername();

                Chunk fileToShare = chunkService.findChunkByIdAndOwner(fileIdToShare, user);

                LinkedHashMap<String, Long> result = userService.queryUsersByNameForFileSharing(username,
                        userRequesterToExcludeFromSearch, fileToShare);

                res.setResult(
                        ResponseEntity.ok()
                                .body(new ApiResponse<LinkedHashMap<String, Long>>("Queried users.", result)));
            } catch (Exception e) {
                res.setErrorResult(e);
            }
        };

        threadExecutorService.execute(process);

        return res;
    }

    @GetMapping("/api/file-sharing/shared_with_me")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> getFilesSharedWithMe(
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();

        Runnable process = () -> {
            try {
                CustomSession session = req.getCustomSession();

                Long userId = session.getUserId();
                User user = userService.findById(userId);

                ArrayList<ChunkReference> chunkReferences = sharingService.getFilesSharedWithUser(user);

                res.setResult(ResponseEntity.ok()
                        .body(new ApiResponse<ArrayList<ChunkReference>>("Files shared with me.", chunkReferences)));
            } catch (Exception e) {
                res.setErrorResult(e);
            }
        };

        threadExecutorService.execute(process);

        return res;
    }

    @GetMapping("/api/file-sharing/me_sharing")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> getAllFilesUserIsSharing(
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();

        Runnable process = () -> {
            try {
                CustomSession session = req.getCustomSession();

                Long userId = session.getUserId();
                User user = userService.findById(userId);
                ArrayList<ChunkReference> refs = sharingService.getFilesUserIsSharing(user);

                res.setResult(ResponseEntity.ok()
                        .body(new ApiResponse<ArrayList<ChunkReference>>("Files the user is sharing.", refs)));
            } catch (Exception e) {
                res.setErrorResult(e);
            }
        };

        threadExecutorService.execute(process);

        return res;
    }

    @GetMapping("/api/file-sharing/file")
    public ResponseEntity<StreamingResponseBody> downloadFile(
            @RequestParam Long fileId,
            CustomHttpServletRequestWrapper req) {
        CustomSession session = req.getCustomSession();
        Long userId = session.getUserId();
        User user = userService.findById(userId);
        Chunk sharedChunk = chunkService.findChunkSharedWithUser(fileId, user);

        StreamingResponseBody srb = out -> {
            try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(out)) {
                fileSystemService.streamFileToClient(sharedChunk, bufferedOutputStream);

            } catch (Exception e) {
                e.printStackTrace();
                throw new StorageException("Erro occured while streaming the file.", e);
            }
        };
        return ResponseEntity.ok()
                .header("Content-Type", sharedChunk.getMimeType())
                .header("Content-Length", String.valueOf(sharedChunk.getSizeBytes()))
                .body(srb);
    }

    @GetMapping(path = "/api/file-sharing/file/bulk", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> bulkDownloadFilesSharedWithMe(
            @RequestParam String chunkIdsStr,
            CustomHttpServletRequestWrapper req) {
        CustomSession session = req.getCustomSession();
        Long userId = session.getUserId();
        User user = userService.findById(userId);

        BulkManipulationData buldDownloadData = StringUtil.extractManipulationData(chunkIdsStr, null);

        Long[] chunkIds = buldDownloadData.getChunks();

        Chunk[] chunks = chunkService.bulkConfirmSharedWithMeAndCollect(chunkIds, user);

        StreamingResponseBody srb = out -> {
            try (TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(out)) {
                tarOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
                fileSystemService.tarEtities(tarOutputStream, chunks, null);
                try {
                    tarOutputStream.finish();
                } catch (IOException e) {
                    logger.warn("Client aborted download before tar finished.", e.getMessage());
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                throw new StorageException("Erro occured while streaming the file.", e);
            }
        };

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"FileStorm.tar\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.EXPIRES, "0")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(srb);
    }

}
