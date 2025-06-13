package server.filestorm.controller;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.request.async.DeferredResult;

import server.filestorm.exception.FileManagementException;
import server.filestorm.model.entity.Chunk;
import server.filestorm.model.entity.Directory;
import server.filestorm.model.entity.User;
import server.filestorm.model.type.ApiResponse;
import server.filestorm.model.type.CustomSession;
import server.filestorm.model.type.FileUploadData;
import server.filestorm.model.type.fileManagement.ChunkReference;
import server.filestorm.model.type.fileManagement.DirectoryCreationData;
import server.filestorm.model.type.fileManagement.DirectoryReference;
import server.filestorm.model.type.fileManagement.HydratedDirectoryReference;
import server.filestorm.service.ChunkService;
import server.filestorm.service.DirectoryService;
import server.filestorm.service.FileSystemService;
import server.filestorm.service.UserService;
import server.filestorm.util.CustomHttpServletRequestWrapper;
import server.filestorm.util.StringUtil;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

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

    @GetMapping("/api/file/{fileId}")
    public DeferredResult<ResponseEntity<?>> downloadFile(
            @PathVariable Long fileId,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<?>> res = new DeferredResult<>();
        CustomSession session = req.getCustomSession();

        Long userId = session.getUserId();
        User user = userService.findById(userId);
        Chunk chunk = chunkService.findChunkByIdAndOwner(fileId, user);

        // return file
        Resource file = fileSystemService.loadAsResource(chunk);
        res.setResult(ResponseEntity.ok()
                .header("Content-Type", chunk.getMimeType())
                .body(file));
        return res;
    }

    @PostMapping(path = "/api/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DeferredResult<ResponseEntity<ApiResponse<?>>> uploadFile(
            FileUploadData fileUploadData,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();
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
        return res;
    }

    @DeleteMapping("/api/file/{fileId}")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> deleteFile(
            @PathVariable Long fileId,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();
        CustomSession session = req.getCustomSession();

        Long userId = session.getUserId();
        User user = userService.findById(userId);

        // verify file existance and ownership in DB
        Chunk chunkForDeletion = chunkService.findChunkByIdAndOwner(fileId, user);

        // delete chunk from DB and FS
        fileSystemService.deleteFile(chunkForDeletion, user);

        res.setResult(ResponseEntity.ok()
                .body(new ApiResponse<Long>("File deleted.", chunkForDeletion.getId())));

        return res;
    }

    @PatchMapping("/api/file/{fileId}")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> changeFileName(
            @PathVariable Long fileId,
            @RequestParam String newFileName,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();
        CustomSession session = req.getCustomSession();

        Long userId = session.getUserId();
        User user = userService.findById(userId);

        // check file existance and ownership
        if (fileId == null) {
            throw new FileManagementException("File ID is required.");
        }
        Chunk chunk = chunkService.findChunkByIdAndOwner(fileId, user);

        // sanitize and check newFileName
        newFileName = StringUtil.sanitizeFileName(newFileName);

        chunk = chunkService.updateOriginalFileName(chunk, newFileName);

        // chage chunk name in DB
        if (chunk == null) {
            throw new FileManagementException("Could not change the name.");
        }

        // return ChunkReference
        res.setResult(ResponseEntity.ok()
                .body(new ApiResponse<ChunkReference>("Name changed.", new ChunkReference(chunk))));
        return res;
    }

    @GetMapping("/api/directory/{directoryId}")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> getDirectoryData(
            @PathVariable Long directoryId,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();
        CustomSession session = req.getCustomSession();

        Long userId = session.getUserId();
        User user = userService.findById(userId);

        // check target dir (sub dir)
        Directory directory = directoryService.findDirectoryForUserById(directoryId, user);

        // return directory data
        HydratedDirectoryReference hydratedDirectory = new HydratedDirectoryReference(directory);

        res.setResult(ResponseEntity.ok()
                .body(new ApiResponse<HydratedDirectoryReference>("Serving directory data.", hydratedDirectory)));
        return res;
    }

    @PostMapping("/api/directory")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> createDirectory(
            DirectoryCreationData data,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();
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
                .body(new ApiResponse<DirectoryReference>("New directory created.", new DirectoryReference(newDirectory))));
        return res;
    }

    @DeleteMapping("/api/directory/{directoryId}")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> deleteDirectory(
            @PathVariable Long directoryId,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();
        CustomSession session = req.getCustomSession();

        Long userId = session.getUserId();
        User user = userService.findById(userId);

        // check target dir and that it is not root user storage dir
        Directory targetDirectory = directoryService.findDirectoryForUserById(directoryId, user);
        if (targetDirectory.getName() == Long.toString(userId) && targetDirectory.getParentDirectory() == null) {
            throw new FileManagementException(
                    "The targeted directory for deletion can not be the root user storage directory.");
        }

        // collect everything for deletion
        ArrayList<Chunk> chunksForDeletion = directoryService.extractChunksFromDirAndSubDirs(targetDirectory);
        ArrayList<Directory> directoriesForDeletion = directoryService.extractDirectoriesFromDirAndSubDirs(targetDirectory);
        directoriesForDeletion.add(targetDirectory);

        // delete files form FS and DB and directories from DB
        fileSystemService.deleteDirectoryAndFiles(directoriesForDeletion, chunksForDeletion, user);

        res.setResult(ResponseEntity.ok()
                .body(new ApiResponse<Long>("Directory deleted.", directoryId )));
        return res;
    }
}
