package server.filestorm.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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
public class FileSystemService {

    private final ChunkService chunkService;

    private final DirectoryService directoryService;

    private final UserService userService;

    private final Path rootLocation;

    public FileSystemService(ServerConfigurationProperties confProps, ChunkService chunkService,
            DirectoryService directoryService, UserService userService) {
        if (confProps.getFileStorageLocation().trim().length() == 0) {
            throw new ConfigurationException("File upload location can not be empty.");
        }
        this.rootLocation = Paths.get(confProps.getFileStorageLocation());
        this.chunkService = chunkService;
        this.directoryService = directoryService;
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
    @Transactional
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

            chunk = chunkService.saveChunk(chunk);

            String fileName = String.format("%1$s___%2$s", Long.toString(chunk.getId()), originalFileName);
            chunk.setName(fileName);
            chunk.setAbsoluteFilePath(
                    getAbsolutePath(Long.toString(user.getId()), fileName).toString());

            chunk = chunkService.saveChunk(chunk);

            // make directory dirty and save it
            targetDirectory.setLastModified(new Date().getTime());
            directoryService.saveDirectory(targetDirectory);

            // increase bytesInStorage for user
            user.addBytesInStorage(chunk.getSizeBytes());
            userService.saveUser(user);

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

    // @Async
    // public CompletableFuture<URI> store(HttpServletRequest req) throws
    // IOException, URISyntaxException {
    // String contentType = req.getHeader("Content-Type");
    // String boundary = contentType.substring(contentType.indexOf("boundary=") +
    // 9);
    // if (boundary.startsWith("\"") && boundary.endsWith("\"")) {
    // boundary = boundary.substring(1, boundary.length() - 1);
    // }
    // boundary = "--" + boundary;

    // try (
    // InputStream inputStream = req.getInputStream();
    // BufferedReader reader = new BufferedReader(new
    // InputStreamReader(inputStream));) {
    // String line;
    // String fileName = null;
    // Path destinationFile = null;
    // OutputStream fileOutputStream = null;

    // while ((line = reader.readLine()) != null) {
    // if (line.startsWith(boundary)) {
    // if (fileOutputStream != null) {
    // // in case there is more than 1 file
    // fileOutputStream.close();
    // fileOutputStream = null;
    // }

    // while ((line = reader.readLine()) != null && !line.isEmpty()) {
    // if (line.startsWith("Content-Disposition:")) {
    // String[] parts = line.split(";");
    // for (String part : parts) {
    // part = part.trim();
    // if (part.startsWith("filename=")) {
    // // get filename and create output stream
    // fileName = part.split("=")[1].replaceAll("\"", "");
    // destinationFile = this.rootLocation
    // .resolve(Paths.get(fileName))
    // .normalize().toAbsolutePath();
    // if (!destinationFile.getParent().equals(this.rootLocation.toAbsolutePath()))
    // {
    // throw new StorageException(
    // "Can not save file outside of the predefined current directory.");
    // }
    // fileOutputStream = Files.newOutputStream(destinationFile,
    // StandardOpenOption.CREATE);
    // }
    // }
    // }
    // }

    // line = reader.readLine();
    // if (fileOutputStream != null) {
    // // save file and break loop
    // while ((line = reader.readLine()) != null && !line.startsWith(boundary)) {
    // fileOutputStream.write(line.getBytes());
    // fileOutputStream.write("\n".getBytes());
    // }
    // // remove next 3 lines if there should be more than one file
    // fileOutputStream.close();
    // reader.close();
    // break;
    // }
    // }
    // }
    // URI uri = new URI(this.base_url + "/file/download/?fileName=" + fileName);
    // return CompletableFuture.completedFuture(uri);
    // }
    // }

    public Resource loadAsResource(Chunk chunk) {
        try {
            Path path = this.getAbsolutePath(chunk);
            boolean isValidFile = this.verifyExistance(path);
            if (!isValidFile) {
                throw new StorageException("File could not be accessed.");
            }
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new StorageException("Could not read file: " + chunk.getName());
            }
        } catch (MalformedURLException e) {
            throw new StorageException("Could not read file: " + chunk.getName(), e);
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
    @Transactional
    public void deleteDirectoryAndFiles(
            ArrayList<Directory> directories,
            ArrayList<Chunk> chunks,
            User owner) {
        for (Directory d : directories) {
            if (d.getOwner().getId() != owner.getId()) {
                continue;
            }
            directoryService.delete(d);
        }
        for (Chunk c : chunks) {
            if (c.getOwner().getId() != owner.getId()) {
                continue;
            }
            deleteFileFromFileSystem(c);
            owner.removeBytesInStorage(c.getSizeBytes());
            chunkService.delete(c);
        }
    }

    @Transactional
    public void deleteFile(Chunk chunk, User user) {
        chunkService.delete(chunk);
        deleteFileFromFileSystem(chunk);
        user.removeBytesInStorage(chunk.getSizeBytes());
        userService.saveUser(user);
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

    private void zipChunks(ZipOutputStream zipOutputStream, Chunk[] chunks, String containingDirPathInZip) {
        for (Chunk chunk : chunks) {
            File file = getAbsolutePath(chunk).toFile();
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                zipOutputStream.putNextEntry(new ZipEntry(
                        (containingDirPathInZip == null ? "" : containingDirPathInZip) + chunk.getOriginalFileName()));

                // copy and copyLarge method from Apache Tomcat for coping data from one stream
                // to another
                boolean isChunkSizeOverTwoGB = chunk.getSizeBytes() >= Long.valueOf("2147483648") ? true : false;
                if (isChunkSizeOverTwoGB) {
                    IOUtils.copyLarge(fileInputStream, zipOutputStream);
                } else {
                    IOUtils.copy(fileInputStream, zipOutputStream);
                }

                zipOutputStream.closeEntry();

            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    private void zipDirectory(ZipOutputStream zipOutputStream, Directory directory, String containingDirPathInZip) {
        try {
            // cteate new empty directory in the zip file
            String dirPathInZip = (containingDirPathInZip == null ? "" : containingDirPathInZip) + directory.getName()
                    + "/";
            zipOutputStream.putNextEntry(new ZipEntry(dirPathInZip));
            zipOutputStream.closeEntry();

            Chunk[] chunks = directory.getChunks().toArray(new Chunk[0]);
            zipChunks(zipOutputStream, chunks, dirPathInZip);

            Directory[] subdirectories = directory.getSubdirectories().toArray(new Directory[0]);
            for (Directory subdirectory : subdirectories) {
                zipDirectory(zipOutputStream, subdirectory, dirPathInZip);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void zipEtities(ZipOutputStream zipOutputStream, Chunk[] chunks, Directory[] directories) {
        if (chunks != null) {
            zipChunks(zipOutputStream, chunks, null);
        }
        if (directories != null) {
            for (Directory directory : directories) {
                zipDirectory(zipOutputStream, directory, null);
            }
        }
    }
}
