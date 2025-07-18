package server.filestorm.controller;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

import server.filestorm.exception.FileManagementException;
import server.filestorm.exception.StorageException;
import server.filestorm.model.entity.Chunk;
import server.filestorm.model.entity.Directory;
import server.filestorm.model.entity.User;
import server.filestorm.model.type.ApiResponse;
import server.filestorm.model.type.BulkManipulationData;
import server.filestorm.model.type.CustomSession;
import server.filestorm.model.type.FileUploadData;
import server.filestorm.model.type.authentication.UserReference;
import server.filestorm.model.type.fileManagement.ChunkReference;
import server.filestorm.model.type.fileManagement.DirectoryCreationData;
import server.filestorm.model.type.fileManagement.DirectoryReference;
import server.filestorm.model.type.fileManagement.HydratedDirectoryReference;
import server.filestorm.model.type.search.UserFileSearchResults;
import server.filestorm.service.ChunkService;
import server.filestorm.service.DirectoryService;
import server.filestorm.service.FileSystemService;
import server.filestorm.service.UserService;
import server.filestorm.thread.ThreadExecutorService;
import server.filestorm.util.CustomHttpServletRequestWrapper;
import server.filestorm.util.StringUtil;

@Controller
public class FileSystem {

    @Autowired
    private FileSystemService fileSystemService;

    @Autowired
    private UserService userService;

    @Autowired
    private ChunkService chunkService;

    @Autowired
    private DirectoryService directoryService;

    @Autowired
    private ThreadExecutorService threadExecutorService;

    Logger logger = LoggerFactory.getLogger(FileSystem.class);

    @GetMapping("/api/file/{fileId}")
    public ResponseEntity<StreamingResponseBody> downloadFile(
            @PathVariable Long fileId,
            CustomHttpServletRequestWrapper req) {

        CustomSession session = req.getCustomSession();

        Long userId = session.getUserId();
        User user = userService.findById(userId);
        Chunk chunk = chunkService.findChunkByIdAndOwner(fileId, user);

        StreamingResponseBody srb = out -> {
            try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(out)) {
                fileSystemService.streamFileToClient(chunk, bufferedOutputStream);

            } catch (Exception e) {
                e.printStackTrace();
                throw new StorageException("Erro occured while streaming the file.", e);
            }
        };

        return ResponseEntity.ok()
                .header("Content-Type", chunk.getMimeType())
                .header("Content-Length", String.valueOf(chunk.getSizeBytes()))
                .body(srb);
    }

    @GetMapping("/api/search/file")
    public DeferredResult<ResponseEntity<?>> searchThroughUserFiles(
            @RequestParam String searchValue,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<?>> res = new DeferredResult<>();

        Runnable process = () -> {
            try {
                CustomSession session = req.getCustomSession();
                Long userId = session.getUserId();
                User user = userService.findById(userId);
                UserFileSearchResults results = chunkService.searchUserFiles(searchValue, user);
                res.setResult(ResponseEntity.ok()
                        .body(new ApiResponse<UserFileSearchResults>("Search results.", results)));
            } catch (Exception e) {
                res.setErrorResult(e);
            }
        };

        threadExecutorService.execute(process);

        return res;
    }

    @GetMapping("/api/public/file/{fileId}/download")
    public ResponseEntity<StreamingResponseBody> downloadPublicFile(@PathVariable Long fileId) {
        // Interface StreamingResponseBody
        // A controller method return value type for asynchronous request processing
        // where the application can write directly to the response OutputStream without
        // holding up the Servlet container thread.
        // https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/StreamingResponseBody.html
        Chunk chunk = chunkService.findPublicChunkById(fileId);

        StreamingResponseBody srb = out -> {
            try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(out)) {
                fileSystemService.streamFileToClient(chunk, bufferedOutputStream);

            } catch (Exception e) {
                e.printStackTrace();
                throw new StorageException("Erro occured while streaming the file.", e);
            }
        };

        return ResponseEntity.ok()
                .header("Content-Length", String.valueOf(chunk.getSizeBytes()))
                .header("Content-Disposition", "attachment; filename=\"" + chunk.getOriginalFileName() + "\"")
                .body(srb);

    }

    @GetMapping("/api/public/file/{fileId}/data")
    public DeferredResult<ResponseEntity<?>> getPublicFileData(@PathVariable Long fileId) {
        DeferredResult<ResponseEntity<?>> res = new DeferredResult<>();

        Runnable process = () -> {
            try {
                Chunk chunk = chunkService.findPublicChunkById(fileId);
                res.setResult(ResponseEntity.ok()
                        .body(new ApiResponse<ChunkReference>("Chunk data.", new ChunkReference(chunk))));
            } catch (Exception e) {
                res.setErrorResult(e);
            }
        };

        threadExecutorService.execute(process);

        return res;
    }

    @PostMapping(path = "/api/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DeferredResult<ResponseEntity<ApiResponse<?>>> uploadFile(
            FileUploadData fileUploadData,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();

        Runnable process = () -> {
            try {
                CustomSession session = req.getCustomSession();
                Long userId = session.getUserId();
                User user = userService.findById(userId);

                Long targetDirectoryId = fileUploadData.getTargetDirectoryId();
                // check upload dir and entry in DB
                Directory directory = directoryService.findDirectoryForUserById(targetDirectoryId, user);

                // check storage space availability
                Long fileSize = fileUploadData.getFile().getSize();
                if (user.getMaxStorageSpace() - user.getBytesInStorage() < fileSize) {
                    throw new FileManagementException("Not enough free storage space for this file.");
                }

                // save file is FS and DB
                Chunk chunk = fileSystemService.store(fileUploadData, user, directory);

                ChunkReference chunkRef = new ChunkReference(chunk);

                res.setResult(ResponseEntity.ok()
                        .body(new ApiResponse<ChunkReference>("File saved.", chunkRef)));
            } catch (Exception e) {
                res.setErrorResult(e);
            }
        };

        threadExecutorService.execute(process);

        return res;
    }

    @DeleteMapping("/api/file/{fileId}")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> deleteFile(
            @PathVariable Long fileId,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();

        Runnable process = () -> {
            try {
                CustomSession session = req.getCustomSession();

                Long userId = session.getUserId();
                User user = userService.findById(userId);

                // verify file existance and ownership in DB
                Chunk chunkForDeletion = chunkService.findChunkByIdAndOwner(fileId, user);

                // delete chunk from DB and FS
                fileSystemService.deleteFile(chunkForDeletion, user);

                res.setResult(ResponseEntity.ok()
                        .body(new ApiResponse<Long>("File deleted.", chunkForDeletion.getId())));
            } catch (Exception e) {
                res.setErrorResult(e);
            }
        };

        threadExecutorService.execute(process);

        return res;
    }

    @PatchMapping("/api/file/{fileId}")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> changeFileName(
            @PathVariable Long fileId,
            @RequestParam String newFileNameWithoutTheExtention,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();

        Runnable process = () -> {
            try {
                CustomSession session = req.getCustomSession();

                Long userId = session.getUserId();
                User user = userService.findById(userId);

                // check file existance and ownership
                if (fileId == null) {
                    throw new FileManagementException("File ID is required.");
                }
                Chunk chunk = chunkService.findChunkByIdAndOwner(fileId, user);

                // sanitize newFileNameWithoutTheExtention
                String sanitizedFileName = StringUtil.sanitizeFileName(newFileNameWithoutTheExtention);

                // chage chunk name in DB
                chunk = chunkService.updateOriginalFileName(chunk, sanitizedFileName);
                if (chunk == null) {
                    throw new FileManagementException("Could not change the name.");
                }

                // return ChunkReference
                res.setResult(ResponseEntity.ok()
                        .body(new ApiResponse<ChunkReference>("Name changed.", new ChunkReference(chunk))));
            } catch (Exception e) {
                res.setErrorResult(e);
            }
        };

        threadExecutorService.execute(process);

        return res;
    }

    @GetMapping(path = "/api/file/bulk", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> bulkDownloadFiles(
            @RequestParam String chunkIdsStr,
            @RequestParam String directoryIdsStr,
            CustomHttpServletRequestWrapper req) {
        CustomSession session = req.getCustomSession();
        Long userId = session.getUserId();
        User user = userService.findById(userId);

        BulkManipulationData buldDownloadData = StringUtil.extractManipulationData(chunkIdsStr, directoryIdsStr);

        Long[] chunkIds = buldDownloadData.getChunks();
        Long[] directoryIds = buldDownloadData.getDirectories();

        Chunk[] chunks = chunkService.bulkCheckChunkOwnershipAndCollect(chunkIds, user);
        Directory[] directories = directoryService.bulkCheckDirectoryOwnershipAndCollect(directoryIds, user);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"FileStorm.tar\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.EXPIRES, "0")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(out -> {
                    try (TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(out)) {
                        tarOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
                        fileSystemService.tarEtities(tarOutputStream, chunks, directories);
                        try {
                            tarOutputStream.finish();
                        } catch (IOException e) {
                            logger.warn("Client aborted download before tar finished.", e.getMessage());
                        }
                    } catch (Exception e) {
                        logger.error(e.getMessage(), e);
                        throw new StorageException("Erro occured while streaming the file.", e);
                    }
                });
    }

    @DeleteMapping(path = "/api/file/bulk", consumes = "application/json")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> bulkDeleteFiles(
            @RequestBody BulkManipulationData buklDeleteData,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();

        Runnable process = () -> {
            try {
                CustomSession session = req.getCustomSession();
                Long userId = session.getUserId();
                User user = userService.findById(userId);

                Long[] chunkIds = buklDeleteData.getChunks();
                Long[] directoryIds = buklDeleteData.getDirectories();

                Chunk[] chunks = chunkService.bulkCheckChunkOwnershipAndCollect(chunkIds, user);
                Directory[] directories = directoryService.bulkCheckDirectoryOwnershipAndCollect(directoryIds, user);

                ArrayList<Chunk> chunksForDeletion = directoryService.extractChunksFromDirAndSubDirs(directories);
                chunksForDeletion.addAll(Arrays.asList(chunks));

                ArrayList<Directory> directoriesForDeletion = directoryService
                        .extractDirectoriesFromDirAndSubDirs(directories);
                directoriesForDeletion.addAll(Arrays.asList(directories));

                fileSystemService.deleteDirectoriesAndFiles(directoriesForDeletion, chunksForDeletion, user);

                res.setResult(ResponseEntity.ok()
                        .body(new ApiResponse<UserReference>("OK.", new UserReference(user))));
            } catch (Exception e) {
                res.setErrorResult(e);
            }
        };

        threadExecutorService.execute(process);

        return res;
    }

    @GetMapping("/api/directory/{directoryId}")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> getDirectoryData(
            @PathVariable Long directoryId,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();

        Runnable process = () -> {
            try {
                CustomSession session = req.getCustomSession();

                Long userId = session.getUserId();
                User user = userService.findById(userId);

                // check target dir (sub dir)
                Directory directory = directoryService.findDirectoryForUserById(directoryId, user);

                // return directory data
                HydratedDirectoryReference hydratedDirectory = new HydratedDirectoryReference(directory);

                res.setResult(ResponseEntity.ok()
                        .body(new ApiResponse<HydratedDirectoryReference>("Serving directory data.",
                                hydratedDirectory)));
            } catch (Exception e) {
                res.setErrorResult(e);
            }
        };

        threadExecutorService.execute(process);

        return res;
    }

    @PostMapping("/api/directory")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> createDirectory(
            DirectoryCreationData data,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();

        Runnable process = () -> {
            try {
                CustomSession session = req.getCustomSession();

                Long targetDirectoryId = data.getTargetDirectoryId();
                String newDirName = StringUtil.sanitizeFileName(data.getNewDirectoryName());

                Long userId = session.getUserId();
                User user = userService.findById(userId);

                // check newDirName
                if (newDirName == null || newDirName.length() == 0) {
                    throw new FileManagementException(
                            "The name of the new directory is not valid.");
                }

                // check target dir (sub dir)
                Directory parentDirectory = directoryService.findDirectoryForUserById(targetDirectoryId, user);

                // create new dir
                Directory newDirectory = directoryService.createNewDirectory(newDirName, user, parentDirectory);

                res.setResult(ResponseEntity.ok()
                        .body(new ApiResponse<DirectoryReference>("New directory created.",
                                new DirectoryReference(newDirectory))));
            } catch (Exception e) {
                res.setErrorResult(e);
            }
        };

        threadExecutorService.execute(process);

        return res;
    }

    @DeleteMapping("/api/directory/{directoryId}")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> deleteDirectory(
            @PathVariable Long directoryId,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();

        Runnable process = () -> {
            try {
                CustomSession session = req.getCustomSession();

                Long userId = session.getUserId();
                User user = userService.findById(userId);

                // check target dir and that it is not root user storage dir
                Directory targetDirectory = directoryService.findDirectoryForUserById(directoryId, user);
                if (targetDirectory.getName() == Long.toString(userId)
                        && targetDirectory.getParentDirectory() == null) {
                    throw new FileManagementException(
                            "The targeted directory for deletion can not be the root user storage directory.");
                }

                // collect everything for deletion
                ArrayList<Chunk> chunksForDeletion = directoryService.extractChunksFromDirAndSubDirs(targetDirectory);
                ArrayList<Directory> directoriesForDeletion = directoryService
                        .extractDirectoriesFromDirAndSubDirs(targetDirectory);
                directoriesForDeletion.add(targetDirectory);

                // delete files form FS and DB and directories from DB
                fileSystemService.deleteDirectoriesAndFiles(directoriesForDeletion, chunksForDeletion, user);

                res.setResult(ResponseEntity.ok()
                        .body(new ApiResponse<UserReference>("Directory deleted.", new UserReference(user))));
            } catch (Exception e) {
                res.setErrorResult(e);
            }
        };

        threadExecutorService.execute(process);

        return res;
    }

    @PatchMapping("/api/directory/{directoryId}")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> changeDirectoryName(
            @PathVariable Long directoryId,
            @RequestParam String newDirecotoryName,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();

        Runnable process = () -> {
            try {
                CustomSession session = req.getCustomSession();
                Long userId = session.getUserId();
                User user = userService.findById(userId);

                Directory directory = directoryService.findDirectoryForUserById(directoryId, user);
                String sanitizedDirectoryName = StringUtil.sanitizeFileName(newDirecotoryName);

                directoryService.changeDirectoryName(directory, sanitizedDirectoryName);

                res.setResult(ResponseEntity.ok()
                        .body(new ApiResponse<DirectoryReference>("Directory name changed.",
                                new DirectoryReference(directory))));
            } catch (Exception e) {
                res.setErrorResult(e);
            }
        };

        threadExecutorService.execute(process);

        return res;
    }

}
