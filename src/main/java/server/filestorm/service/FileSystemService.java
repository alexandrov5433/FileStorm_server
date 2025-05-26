package server.filestorm.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import server.filestorm.config.ServerConfigurationProperties;
import server.filestorm.exception.ConfigurationException;
import server.filestorm.exception.FileManagementException;
import server.filestorm.exception.ProcessingException;
import server.filestorm.exception.StorageException;
import server.filestorm.model.entity.Chunk;
import server.filestorm.model.type.FileUploadData;
import server.filestorm.model.type.fileManagement.DirectoryReference;
import server.filestorm.util.PathUtil;

@Service
public class FileSystemService {

    private final Path rootLocation;

    public FileSystemService(ServerConfigurationProperties confProps) {
        if (confProps.getFileStorageLocation().trim().length() == 0) {
            throw new ConfigurationException("File upload location can not be empty.");
        }
        this.rootLocation = Paths.get(confProps.getFileStorageLocation());
    }

    /**
     * Saves the given file using a given relative path.
     * 
     * @param file     The file which is to be saved. The name of the file is
     *                 extracted from this property and used to generate the
     *                 absolute path.
     * @param filePath The string path to the file, relative to the root,
     *                 user-specific storage directory. E.g. "11/" or
     *                 "11/books/fantasy". Save location example with file name
     *                 "lotr.pdf"
     *                 "D:/somePlatformDirectory/rootLocation/11/books/fantasy/lotr.pdf",
     *                 where rootLocation and everything before it comes from
     *                 ServerConfigurationProperties.
     * @param userId   The id of the user submitting the file.
     * @return The Chunk comes with the following properties set: name,
     *         absolute_file_path, mime_type and size_bytes.
     * @throws IOException
     * @throws URISyntaxException
     */
    public Chunk store(FileUploadData fileUploadData, Integer userId) throws FileManagementException {
        try {
            MultipartFile file = fileUploadData.getFile();

            // check file is not empty
            if (file.isEmpty() || file.getOriginalFilename() == null) {
                throw new StorageException("Failed to store empty file.");
            }

            // check path is valid string
            String relativeFilePath = PathUtil.standardizeRelativePathString(fileUploadData.getRelativePath());
            PathUtil.verifyRelativePath(relativeFilePath, userId);

            // check file name is valid
            String fileName = PathUtil.sanitizeFileName(file.getOriginalFilename());

            // check directory and file existence
            boolean uploadDirExists = this.verifyExistance(relativeFilePath);
            if (!uploadDirExists) {
                throw new FileManagementException("The upload directory does not exist: " + relativeFilePath);
            }

            // check if file with this name already exists
            boolean fileExistsInDir = this.verifyExistance(
                    PathUtil.concatNameAtEndOfPath(relativeFilePath, fileName));
            if (fileExistsInDir) {
                throw new FileManagementException(
                        "A file with this name exists in this directory: " + fileExistsInDir);
            }

            // build path to save file
            Path destinationFile = this.getAbsolutePath(
                    PathUtil.concatNameAtEndOfPath(relativeFilePath, fileName));

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
            Chunk chunk = new Chunk();
            chunk.setAbsoluteFilePath(destinationFile.toString());
            chunk.setName(fileName);
            chunk.setRelativeFilePath(relativeFilePath);
            chunk.setSizeBytes(file.getSize());
            chunk.setMimeType(file.getContentType());
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

    Stream<Path> loadAll() {
        try {
            return Files.walk(this.rootLocation, 1)
                    .filter(path -> !path.equals(this.rootLocation))
                    .map(this.rootLocation::relativize);
        } catch (IOException e) {
            throw new StorageException("Failed to read stored files.", e);
        }
    }

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

    // public void deleteAll() {
    // FileSystemUtils.deleteRecursively(this.rootLocation.toFile());
    // }

    // public void init() {
    // try {
    // Files.createDirectories(rootLocation);
    // } catch (IOException e) {
    // throw new StorageException("Could not initialize storage directory.", e);
    // }
    // }

    /**
     * Creates a new derectory in the root storage directory of the server
     * (rootLocation).
     * 
     * @param userRootDirectoryName The name of the new user-specific directory.
     *                              This will be the base (root) of the directory
     *                              tree for this user.
     * @throws FileManagementException When a root user directory with this name
     *                                 already exists or the new directory could
     *                                 not be created with
     *                                 File.mkdir().
     */
    public DirectoryReference createRootDirectoryForUser(String userRootDirectoryName) throws FileManagementException, ProcessingException {
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
        return new DirectoryReference(userRootDirectoryName);
    }

    /**
     * Creates a new subdirectory in the user-specific storage derectory.
     * 
     * @param subDirectoryPath The path to the directory in which the new
     *                         subdirectory must be placed. This path is relative to
     *                         the rootStorage directory of the server. It must
     *                         start with the user´s root storage directory. E.g.
     *                         "/11/firstSubdirectory/targetSubDirectory". 11 is the
     *                         user´s root storage directory.
     * @param newDirectoryName The name of the new subdirectory - e.g. "new_docs",
     *                         which will produce
     *                         "/11/firstSubdirectory/targetSubDirectory/new_docs".
     * @throws FileManagementException When subDirectoryPath does not exist, is not
     *                                 readable or writable, or when the new
     *                                 directory could not be created with
     *                                 File.mkdir().
     * @throws ProcessingException
     */
    public DirectoryReference createSubDirectoryForUser(String subDirectoryPath, String newDirectoryName) throws FileManagementException, ProcessingException {
        // sanitize new name
        newDirectoryName = PathUtil.sanitizeFileName(newDirectoryName);
        if (newDirectoryName.length() == 0 || newDirectoryName == null) {
            throw new FileManagementException("This directory name is invalid.");
        }
        // check sub dir and new dir existance
        boolean subDirExists = this.verifyExistance(subDirectoryPath);
        if (!subDirExists) {
            throw new FileManagementException("The targeted subdirectory does not exist: " + subDirectoryPath);
        }
        boolean newDirExistsInSubDir = this.verifyExistance(
                PathUtil.concatNameAtEndOfPath(subDirectoryPath, newDirectoryName));
        if (newDirExistsInSubDir) {
            throw new FileManagementException(
                    "This directory already includes a directory with this name: " + newDirectoryName);
        }

        File newDir = this.getAbsolutePath(
                PathUtil.concatNameAtEndOfPath(subDirectoryPath, newDirectoryName)).toFile();
        boolean isDirectoryCreated = newDir.mkdir();
        if (!isDirectoryCreated) {
            throw new FileManagementException("Could not create the new derectory: " + newDirectoryName);
        }

        return new DirectoryReference(newDirectoryName);
    }

    /**
     * Deletes the File on the given path.
     * 
     * @param targetFilePath The path of the file, which is to be deleted.
     * @return true if the file was deleted successfully, false otherwise.
     * @throws FileManagementException When the path is not valid or an unexpected
     *                                 exeption occurs.
     */
    public Boolean deleteUserFile(String targetDirPath, String targetFileName) throws FileManagementException, ProcessingException {
        try {
            String fullPathString = PathUtil.concatNameAtEndOfPath(targetDirPath, targetFileName);
            boolean isValid = verifyExistance(fullPathString);
            if (!isValid) {
                throw new FileManagementException("Directory path is invalid: " + targetDirPath);
            }
            File targetDir = this.getAbsolutePath(fullPathString).toFile();
            return this.deleteFile(targetDir);
        } catch (Exception e) {
            throw new FileManagementException("A problem occured while deleting: " + targetDirPath);
        }
    }

    /**
     * Deletes the directory of this path and everything in it.
     * 
     * @param targetDirectoryPath The directory which is to be deleted.
     * @return True if the directory was successfully deleted and false otherwise.
     * @throws FileManagementException When the FileSystemService.verifyExistance()
     *                                 returns false or when an unexpected Exception
     *                                 occurs.
     */
    public Boolean deleteUserDirectory(String targetDirectoryPath)
            throws FileManagementException {
        try {
            boolean isValid = verifyExistance(targetDirectoryPath);
            if (!isValid) {
                throw new FileManagementException("Directory path is invalid: " + targetDirectoryPath);
            }
            File targetDir = this.getAbsolutePath(targetDirectoryPath).toFile();
            return this.deleteFile(targetDir);
        } catch (Exception e) {
            throw new FileManagementException("A problem occured while deleting: " + targetDirectoryPath);
        }
    }

    /**
     * Deletes the target directory (or file), the contents of the target directory
     * and all of
     * it´s subdirectories and their contents recursively.
     * 
     * @param targetDir The directory which is to be deleted.
     */
    private Boolean deleteFile(File targetDir) {
        File[] contents = targetDir.listFiles();
        if (contents != null) {
            for (File file : contents) {
                this.deleteFile(file);
            }
        }
        return targetDir.delete();
    }

    /**
     * Renames the file in the file system.
     * @param targetDir The directory where the file can be found.
     * @param oldFileName The old name of the file. Is used to find the file.
     * @param newFileName The new name of the file.
     * @throws FileManagementException When the old file can not be found in the given directory. When the directory itself is invalid. When the File.renameTo() method could not rename the file. 
     */
    public void changeFileName(String targetDir, String oldFileName, String newFileName) throws FileManagementException {
        boolean isTargetDirValid = verifyExistance(targetDir);
        if (!isTargetDirValid) {
            throw new FileManagementException("Target directory does not exist.");
        }
        Path oldPath = getAbsolutePath(PathUtil.concatNameAtEndOfPath(targetDir, oldFileName));
        boolean isOldFileValid = verifyExistance(oldPath);
        if (!isOldFileValid) {
            throw new FileManagementException("Target file does not exist.");
        }
        Path newPath = getAbsolutePath(PathUtil.concatNameAtEndOfPath(targetDir, newFileName));
        File oldFile = oldPath.toFile();
        File newFile = newPath.toFile();
        boolean successfullyRenamed = oldFile.renameTo(newFile);
        if (!successfullyRenamed) {
            throw new FileManagementException("Could not rename file.");
        }
    }

    /**
     * Cretes a Path object with absolute path, using the rootLocation (of the
     * server) and the given relativeFilePath.
     * 
     * @param relativeFilePath The relative file path, which will be resolved
     *                         against the rootLocation - the root storage location
     *                         of the server. E.g. for relativeFilePath:
     *                         "11/my_docs/work".
     */
    private Path getAbsolutePath(String relativeFilePath) throws ProcessingException {
        relativeFilePath = PathUtil.standardizeRelativePathString(relativeFilePath);
        return this.rootLocation
                .resolve(Paths.get(relativeFilePath))
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
}
