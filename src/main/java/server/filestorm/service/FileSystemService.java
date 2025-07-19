package server.filestorm.service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import server.filestorm.config.ServerConfigurationProperties;
import server.filestorm.exception.ConfigurationException;
import server.filestorm.exception.FileManagementException;
import server.filestorm.exception.ProcessingException;
import server.filestorm.exception.StorageException;
import server.filestorm.model.entity.Chunk;
import server.filestorm.model.entity.Directory;
import server.filestorm.model.entity.User;
import server.filestorm.model.type.FileUploadData;
import server.filestorm.util.PathUtil;
import server.filestorm.util.StringUtil;

@Service
@Transactional
public class FileSystemService {

    private final UserService userService;

    private final ChunkService chunkService;

    private final DirectoryService directoryService;

    private final Path rootLocation;

    private final String clientLocation;

    public FileSystemService(ServerConfigurationProperties confProps, ChunkService chunkService,
            DirectoryService directoryService, UserService userService) {
        if (confProps.getFileStorageLocation().trim().length() == 0) {
            throw new ConfigurationException("File upload location can not be empty.");
        }
        this.rootLocation = Paths.get(confProps.getFileStorageLocation());
        this.chunkService = chunkService;
        this.directoryService = directoryService;
        this.clientLocation = confProps.getClientLocation();
        this.userService = userService;
    }

    /**
     * Saves the given file and assigns it the user as owner and the directory as
     * the one containing the file.
     * 
     * @param fileUploadData  The file upload data, containing the file it self.
     * @param user            The user submitting the file.
     * @param targetDirectory The directory under which the file is to be saved.
     * @return The Chunk created for this file.
     * @throws FileManagementException
     */
    public Chunk store(FileUploadData fileUploadData, User user, Directory targetDirectory)
            throws FileManagementException {
        try {
            MultipartFile file = fileUploadData.getFile();

            // check file is not empty
            if (file.isEmpty() || file.getOriginalFilename() == null) {
                throw new StorageException("Failed to store empty file.");
            }

            // check file name is valid and available
            String originalFileName = StringUtil.sanitizeFileName(file.getOriginalFilename());
            while (directoryService.doesDirectoryIncludeChunk(targetDirectory, originalFileName)) {
                originalFileName = StringUtil.appendUniqueCounter(originalFileName);
            }

            // check directory existence
            boolean uploadDirExists = this.verifyExistance(Long.toString(user.getId()));
            if (!uploadDirExists) {
                throw new FileManagementException("The upload directory does not exist.");
            }

            Chunk chunk = new Chunk();
            chunk.setOwner(user);
            chunk.setDirectory(targetDirectory);
            chunk.setAbsoluteFilePath("tempVal");
            chunk.setName("tempVal");
            chunk.setOriginalFileName(originalFileName);
            chunk.setSizeBytes(file.getSize());
            chunk.setMimeType(file.getContentType());

            // save chunk for it to recieve an ID
            chunk = chunkService.saveChunk(chunk);

            String fileName = String.format("%1$s___%2$s", Long.toString(chunk.getId()), originalFileName);
            chunk.setName(fileName);
            chunk.setAbsoluteFilePath(
                    getAbsolutePath(Long.toString(user.getId()), fileName).toString());

            // update directory
            directoryService.incrementElementsCountByOne(targetDirectory);

            // increase bytesInStorage for user
            userService.increaseBytesInStorage(user, chunk.getSizeBytes());

            // build path to save file
            Path destinationFile = getAbsolutePath(chunk);

            // check if file with this name already exists
            boolean fileExistsInDir = this.verifyExistance(destinationFile);
            if (fileExistsInDir) {
                throw new FileManagementException(
                        "A file with this name exists in this directory.");
            }

            // check file is not being saved in server root storage
            if (destinationFile.equals(this.rootLocation.toAbsolutePath())) {
                throw new StorageException("Can not save file outside of the predefined user-specific root directory.");
            }

            // save file
            try (InputStream inputStream = file.getInputStream();
                    OutputStream outputStream = Files.newOutputStream(destinationFile, StandardOpenOption.CREATE);) {
                byte[] buffer = new byte[8192]; // 8KB buffer
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            // return Chunk
            return chunk;

        } catch (Exception e) {
            throw new FileManagementException(e.getMessage(), e);
        }
    }

    public void streamFileToClient(Chunk chunk, BufferedOutputStream bufferedOutputStream) {
        try {
            Path path = this.getAbsolutePath(chunk);
            boolean isValidFile = this.verifyExistance(path);
            if (!isValidFile) {
                throw new StorageException("File could not be accessed.");
            }
            File file = path.toFile();

            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                boolean isChunkSizeOverTwoGB = chunk.getSizeBytes() >= Long.valueOf("2147483648") ? true : false;
                if (isChunkSizeOverTwoGB) {
                    IOUtils.copyLarge(fileInputStream, bufferedOutputStream);
                } else {
                    IOUtils.copy(fileInputStream, bufferedOutputStream);
                }
            }
        } catch (Exception e) {
            throw new StorageException("Could not read file: " + chunk.getName(), e);
        }
    }

    public void streamFileStormIndexHtmlToClient(BufferedOutputStream bufferedOutputStream) {
        try {
            Path path = Path.of(this.clientLocation);
            boolean isValidFile = this.verifyExistance(path);
            if (!isValidFile) {
                throw new StorageException("File could not be accessed.");
            }
            File fileStormApp = path.toFile();

            try (FileInputStream fileInputStream = new FileInputStream(fileStormApp)) {
                IOUtils.copy(fileInputStream, bufferedOutputStream);
            }
        } catch (Exception e) {
            throw new StorageException("Could not read the FileStorm app file.", e);
        }
    }

    /**
     * Creates a new derectory in the root storage directory of the server
     * (rootLocation). Returns the File object of the newly created directory.
     * 
     * @param userRootDirectoryName The name of the new user-specific directory.
     *                              This will be the base (root) of the directory
     *                              tree for this user.
     * @throws FileManagementException When a root user directory with this name
     *                                 already exists or the new directory could
     *                                 not be created with
     *                                 File.mkdir().
     */
    public File createRootDirectoryForUser(String userRootDirectoryName)
            throws FileManagementException, ProcessingException {
        File newDir = this.getAbsolutePath(userRootDirectoryName).toFile();

        // check if already exists
        if (newDir.exists()) {
            throw new FileManagementException(
                    "I root user directory with this name already exists: " + userRootDirectoryName);
        }

        boolean isFileCreated = newDir.mkdir();
        if (!isFileCreated) {
            throw new FileManagementException("Could not create new derectory.");
        }
        return newDir;
    }

    /**
     * Deletes all given directories from DB and then all chunks from FS and DB.
     * 
     * @param directories Directories to delete.
     * @param chunks      Chunks to delete.
     * @param owner       The user owner of the directories and chunks.
     */
    public void deleteDirectoriesAndFiles(
            ArrayList<Directory> directories,
            ArrayList<Chunk> chunks,
            User owner) {
        for (Chunk c : chunks) {
            if (c.getOwner().getId() != owner.getId()) {
                continue;
            }
            deleteFileFromFileSystem(c);

            Directory parenDirectory = c.getDirectory();
            parenDirectory.removeChunk(c);
            directoryService.decrementElementsCountByOne(parenDirectory);

            userService.decreaseBytesInStorage(owner, c.getSizeBytes());
            chunkService.delete(c);
        }
        for (Directory d : directories) {
            if (d.getOwner().getId() != owner.getId()) {
                continue;
            }
            d.getParentDirectory().ifPresent((parentDirectory) -> {
                parentDirectory.removeSubdirectory(d);
                directoryService.decrementElementsCountByOne(parentDirectory);
            });
            directoryService.delete(d);
        }
    }

    public void deleteFile(Chunk chunk, User user) {
        chunkService.delete(chunk);
        deleteFileFromFileSystem(chunk);

        Directory parenDirectory = chunk.getDirectory();
        parenDirectory.removeChunk(chunk);
        directoryService.decrementElementsCountByOne(parenDirectory);

        userService.decreaseBytesInStorage(user, chunk.getSizeBytes());
    }

    /**
     * Deletes the file referenced in the chunk.
     * 
     * @param chunk The chunk referencing the file, which is to be deleted.
     * @return true if the file was deleted successfully, false otherwise.
     * @throws FileManagementException When the path is not valid or an unexpected
     *                                 exeption occurs.
     */
    private Boolean deleteFileFromFileSystem(Chunk chunk)
            throws FileManagementException, ProcessingException {
        try {
            Path absolutePath = getAbsolutePath(chunk);
            boolean isValid = verifyExistance(absolutePath);
            if (!isValid) {
                throw new FileManagementException("Directory path is invalid: " + absolutePath.toString());
            }
            File targetFile = absolutePath.toFile();
            return targetFile.delete();
        } catch (Exception e) {
            throw new FileManagementException("A problem occured while deleting: " + chunk.getName());
        }
    }

    /**
     * Cretes a Path object with absolute path, using the rootLocation (of the
     * server) and the given relativeFilePath.
     * 
     * @param relativeFilePath The relative file path, which will be resolved
     *                         against the rootLocation - the root storage location
     *                         of the server. E.g. for relativeFilePath:
     *                         "11/my_doc.pdf".
     */
    private Path getAbsolutePath(String relativeFilePath) throws ProcessingException {
        relativeFilePath = PathUtil.standardizeRelativePathString(relativeFilePath);
        return this.rootLocation
                .resolve(Paths.get(relativeFilePath))
                .normalize().toAbsolutePath();
    }

    /**
     * Cretes a Path object with absolute path, using the rootLocation (of the
     * server) and the given userId and fileName.
     * 
     * @param userId   The ID of the user. Is used to indicate the root directory of
     *                 the user.
     * @param fileName The name of the file, found in the root user directory.
     */
    private Path getAbsolutePath(String userId, String fileName) throws ProcessingException {
        return this.rootLocation
                .resolve(Paths.get(String.format("%1$s/%2$s", userId, fileName)))
                .normalize().toAbsolutePath();
    }

    /**
     * Extracts the absolute path string from the Chunk and creates an absolute Path
     * object from it.
     * 
     * @param chunk The targeted Chunk object.
     */
    private Path getAbsolutePath(Chunk chunk) {
        String absolutePath = chunk.getAbsoluteFilePath();
        return Path.of(absolutePath).toAbsolutePath();
    }

    /**
     * Creates a File object with absolute path out of the given relativePath and
     * checks if this File can be red, written to and if it exists.
     * 
     * @param relativePath The path to the desired directory, relative to the
     *                     rootLocation which is the root storage location of the
     *                     server. E.g. "11/my_docs/work".
     */
    public Boolean verifyExistance(String relativePath) throws ProcessingException {
        Path path = this.getAbsolutePath(relativePath);
        File file = path.toFile();
        return (file.exists() && file.canRead() && file.canWrite());
    }

    /**
     * Converts the given Path to File and checks it exists, can be red and written
     * to.
     * 
     * @param path The Path to check.
     */
    public Boolean verifyExistance(Path path) {
        File file = path.toFile();
        return (file.exists() && file.canRead() && file.canWrite());
    }

    public void tarChunks(TarArchiveOutputStream tarOutputStream, Chunk[] chunks, String containingDirPathInTar)
            throws IOException {
        for (Chunk chunk : chunks) {
            File file = getAbsolutePath(chunk).toFile();
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                TarArchiveEntry tarEntity = new TarArchiveEntry(
                        file,
                        (containingDirPathInTar == null ? "" : containingDirPathInTar) + chunk.getOriginalFileName());

                tarOutputStream.putArchiveEntry(tarEntity);

                byte[] bytes = new byte[2048];
                Integer bytesCount;
                while ((bytesCount = fileInputStream.read(bytes)) != -1) {
                    tarOutputStream.write(bytes, 0, bytesCount);
                }
                tarOutputStream.closeArchiveEntry();

            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
        }
    }

    public void tarDirectory(TarArchiveOutputStream tarOutputStream, Directory directory,
            String containingDirPathInTar)
            throws IOException {
        try {
            // cteate new empty directory in the tar file
            String dirPathInTar = (containingDirPathInTar == null ? "" : containingDirPathInTar) + directory.getName()
                    + "/";
            TarArchiveEntry tarEntry = new TarArchiveEntry(dirPathInTar);
            tarEntry.setMode(0755);
            tarOutputStream.putArchiveEntry(tarEntry);
            tarOutputStream.closeArchiveEntry();

            Chunk[] chunks = directory.getChunks().toArray(new Chunk[0]);
            tarChunks(tarOutputStream, chunks, dirPathInTar);

            Directory[] subdirectories = directory.getSubdirectories().toArray(new Directory[0]);
            for (Directory subdirectory : subdirectories) {
                tarDirectory(tarOutputStream, subdirectory, dirPathInTar);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    public void tarEtities(TarArchiveOutputStream tarOutputStream, Chunk[] chunks, Directory[] directories)
            throws IOException {
        if (chunks != null) {
            tarChunks(tarOutputStream, chunks, null);
        }
        if (directories != null) {
            for (Directory directory : directories) {
                tarDirectory(tarOutputStream, directory, null);
            }
        }
    }
}
