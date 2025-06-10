package server.filestorm.controller;

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
import server.filestorm.util.PathUtil;

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

    @GetMapping("/api/file")
    public DeferredResult<ResponseEntity<?>> downloadFile(
            @RequestParam Integer fileId,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<?>> res = new DeferredResult<>();
        CustomSession session = req.getCustomSession();

        Integer userId = session.getUserId();
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
        Directory directory = directoryService.getDirectory(targetDirectoryId);

        // check storage space availability
        Long fileSize = fileUploadData.getFile().getSize();
        if (user.getMaxStorageSpace() - user.getBytesInStorage() < fileSize) {
            throw new FileManagementException("Not enough free storage space for this file.");
        }

        // save file is FS
        Chunk chunk = fileSystemService.store(fileUploadData, user, directory);

        ChunkReference chunkRef = new ChunkReference(chunk);

        res.setResult(ResponseEntity.ok()
                .body(new ApiResponse<ChunkReference>("File saved.", chunkRef)));
        return res;
    }

    @DeleteMapping("/api/file")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> deleteFile(
            @RequestParam String targetDirectoryPath,
            @RequestParam String targetFileName,
            @RequestParam Integer targetFileId,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();
        CustomSession session = req.getCustomSession();

        Integer userId = session.getUserId();
        User user = userService.findById(userId);

        // validate directory and file existance and ownership in DB
        userService.verifyDirectoryExistance(user, targetDirectoryPath);
        chunkService.findChunkByIdAndOwner(targetFileId, user);

        // check chunk existance in dir reference in DB
        userService.verifyChunkRefInDirRef(user, targetDirectoryPath, targetFileName, targetFileId);

        // delete chunk from FS
        boolean isChunkDeleted = fileSystemService.deleteUserFile(targetDirectoryPath, targetFileName);
        if (!isChunkDeleted) {
            throw new FileManagementException("Could not delete file: " + targetFileName);
        }

        // delete chunk from DB
        Integer deletedChunkId = userService.deleteChunkAndRefFromDirRef(user, targetDirectoryPath, targetFileName);
        if (deletedChunkId == null) {
            throw new FileManagementException("Could not delete file: " + targetFileName);
        }

        res.setResult(ResponseEntity.ok()
                .body(new ApiResponse<Integer>("File deleted.", deletedChunkId)));

        return res;
    }

    @PatchMapping("/api/file/name/{fileId}")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> changeFileName(
            @PathVariable Integer fileId,
            @RequestParam String newFileName,
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

        // sanitize and check newFileName
        newFileName = PathUtil.sanitizeFileName(newFileName);

        // change file name in FS
        fileSystemService.changeFileName(chunk.getRelativeFilePath(), chunk.getName(), newFileName);

        // chage chunk name and chunkRef name in DB
        ChunkReference chunkRef = userService.changeChunkRefName(chunk, user, newFileName);
        if (chunkRef == null) {
            throw new FileManagementException("Could not change the name.");
        }

        // return ChunkReference
        res.setResult(ResponseEntity.ok()
                .body(new ApiResponse<ChunkReference>("Name changed.", chunkRef)));
        return res;
    }

    @GetMapping("/api/directory")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> getDirectoryData(
            @RequestParam String targetDirectoryPath,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();
        CustomSession session = req.getCustomSession();

        Integer userId = session.getUserId();
        User user = userService.findById(userId);

        // check target dir starts with root user storage dir
        PathUtil.verifyRelativePath(targetDirectoryPath, userId);

        // check directory existance
        userService.verifyDirectoryExistance(user, targetDirectoryPath);

        // return directory data
        HydratedDirectoryReference hydratedDirRef = userService.getHydratedDirectoryDataForUser(user, targetDirectoryPath)
                .orElseThrow(() -> new FileManagementException("Could not get directory's data."));

        res.setResult(ResponseEntity.ok()
                .body(new ApiResponse<HydratedDirectoryReference>("Serving directory data.", hydratedDirRef)));
        return res;
    }

    @PostMapping("/api/directory")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> createDirectory(
            DirectoryCreationData data,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();
        CustomSession session = req.getCustomSession();

        String targetSubDirPath = data.getTargetDirectoryPath().trim();
        String newDirName = data.getNewDirectoryName().trim();
        Integer userId = session.getUserId();
        User user = userService.findById(userId);

        // check newDirName
        if (newDirName == null || newDirName.length() == 0) {
            throw new FileManagementException(
                    "The name of the new directory is not valid.");
        }

        // check sub dir path string
        PathUtil.verifyRelativePath(targetSubDirPath, userId);

        // create sub dir in FS
        DirectoryReference dirRef = fileSystemService.createSubDirectoryForUser(targetSubDirPath, newDirName);

        // add sub dir reference in DB
        userService.addDirectory(user, dirRef, targetSubDirPath);

        HydratedDirectoryReference hydratedDirRef = userService.getHydratedDirectoryDataFromDirRef(dirRef)
                .orElseThrow(() -> new FileManagementException("Could not get directory's data."));

        res.setResult(ResponseEntity.ok()
                .body(new ApiResponse<HydratedDirectoryReference>("New directory created.", hydratedDirRef)));
        return res;
    }

    @DeleteMapping("/api/directory")
    public DeferredResult<ResponseEntity<ApiResponse<?>>> deleteDirectory(
            @RequestParam String targetDirectoryPath,
            CustomHttpServletRequestWrapper req) {
        DeferredResult<ResponseEntity<ApiResponse<?>>> res = new DeferredResult<>();
        CustomSession session = req.getCustomSession();

        Integer userId = session.getUserId();
        String rootUserStorageDir = String.format("%1$d", userId);
        User user = userService.findById(userId);

        // check target dir starts with root user storage dir
        PathUtil.verifyRelativePath(targetDirectoryPath, userId);

        // check target dir is not root user storage dir
        if (targetDirectoryPath.equals(rootUserStorageDir)) {
            throw new FileManagementException(
                    "The targeted directory for deletion can not be the root user storage directory.");
        }

        // check directory existance in DB
        userService.verifyDirectoryExistance(user, targetDirectoryPath);

        // delete directory form FS and everything in it
        boolean isDirectoryDeleted = fileSystemService.deleteUserDirectory(targetDirectoryPath);
        if (!isDirectoryDeleted) {
            throw new FileManagementException("Could not delete directory.");
        }

        // delete dir reference form DB and everything in it
        DirectoryReference deletedDirRef = userService.deleteDirectory(user, targetDirectoryPath);
        if (deletedDirRef == null) {
            throw new FileManagementException("Could not delete directory.");
        }

        res.setResult(ResponseEntity.ok()
                .body(new ApiResponse<DirectoryReference>("Directory deleted.", deletedDirRef)));
        return res;
    }
}
