package server.filestorm.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import server.filestorm.exception.FileManagementException;
import server.filestorm.exception.StorageException;
import server.filestorm.model.entity.Chunk;
import server.filestorm.model.entity.Directory;
import server.filestorm.model.entity.User;
import server.filestorm.model.repository.DirectoryRepository;
import server.filestorm.util.StringUtil;

@Service
@Transactional
public class DirectoryService {

    @Autowired
    private DirectoryRepository directoryRepository;

    public Directory saveDirectory(Directory dir) {
        return directoryRepository.save(dir);
    }

    /**
     * Creates a new root directory for the new user in DB.
     * 
     * @param name  The name of the new root directory.
     * @param owner The user owner of the root directory.
     * @return The newly created directory.
     */
    public Directory createNewDirectory(String name, User owner) {
        Directory dir = new Directory();
        dir.setName(name);
        dir.setOwner(owner);
        dir.setParentDirectory(null);
        return directoryRepository.save(dir);
    }

    /**
     * Creates a new directory (subdirectory) for the user in DB.
     * 
     * @param name  The name of the new directory.
     * @param owner The user owner of the directory.
     * @return The newly created directory.
     */
    public Directory createNewDirectory(String name, User owner, Directory parentDirectory) {
        Directory dir = new Directory();
        while (doesDirectoryIncludeSubdirectoryWithThisName(parentDirectory, name)) {
            name = StringUtil.appendUniqueCounter(name);
        }
        dir.setName(name);
        dir.setOwner(owner);
        dir.setParentDirectory(parentDirectory);
        this.incrementElementsCountByOne(parentDirectory);
        return directoryRepository.save(dir);
    }

    public void delete(Directory directory) {
        directoryRepository.delete(directory);
    }

    public void incrementElementsCountByOne(Long directoryId) {
        directoryRepository.incrementElementsCountByOne(directoryId);
    }

    public void incrementElementsCountByOne(Directory directory) {
        directoryRepository.incrementElementsCountByOne(directory.getId());
    }

    public void decrementElementsCountByOne(Long directoryId) {
        directoryRepository.decrementElementsCountByOne(directoryId);
    }
    
    public void decrementElementsCountByOne(Directory directory) {
        directoryRepository.decrementElementsCountByOne(directory.getId());
    }

    public Directory findDirectoryForUserById(Long directoryId, User owner) throws StorageException {
        return directoryRepository.findDirectoryForUserById(directoryId, owner)
                .orElseThrow(() -> new StorageException("Directory could not be found."));
    }

    public Directory[] bulkCheckDirectoryOwnershipAndCollect(Long[] directoryIds, User owner) throws StorageException {
        if (directoryIds == null) {
            return new Directory[0];
        }

        ArrayList<Directory> directories = new ArrayList<Directory>();
        for (long directoryId : directoryIds) {
            directories.add(findDirectoryForUserById(directoryId, owner));
        }
        return directories.toArray(new Directory[0]);
    }

    /**
     * Collects all chunks from the given directory, it´s subdirectories and all
     * other subdirectories down the directory tree.
     * 
     * @param dir The directory from which the chunk collection must start.
     * @return All collected chunks.
     */
    public ArrayList<Chunk> extractChunksFromDirAndSubDirs(Directory dir) {
        ArrayList<Chunk> chunks = new ArrayList<Chunk>(dir.getChunks());
        List<Directory> subdirectories = dir.getSubdirectories();
        for (Directory subdir : subdirectories) {
            ArrayList<Chunk> subChunks = extractChunksFromDirAndSubDirs(subdir);
            chunks.addAll(subChunks);
        }
        return chunks;
    }

    /**
     * Collects all chunks from all given directories, their subdirectories and all
     * other subdirectories down the directory tree.
     * 
     * @param dirs The directories from which the chunk collection must start.
     * @return All collected chunks.
     */
    public ArrayList<Chunk> extractChunksFromDirAndSubDirs(Directory[] dirs) {
        ArrayList<Chunk> chunks = new ArrayList<Chunk>();
        for (Directory dir : dirs) {
            ArrayList<Chunk> subChunks = extractChunksFromDirAndSubDirs(dir);
            chunks.addAll(subChunks);
        }
        return chunks;
    }

    /**
     * Collects all subdirectories form the given directory and from all directories
     * found down the directory tree. The initially given (first) directory IS NOT
     * included in the final result.
     * 
     * @param dir The initial (first) directory, from which the collection must
     *            start.
     * @return All collected directories.
     */
    public ArrayList<Directory> extractDirectoriesFromDirAndSubDirs(Directory dir) {
        ArrayList<Directory> subdirectories = new ArrayList<Directory>(dir.getSubdirectories());
        for (Directory subdir : subdirectories) {
            ArrayList<Directory> deeperSubdirs = extractDirectoriesFromDirAndSubDirs(subdir);
            subdirectories.addAll(deeperSubdirs);
        }
        return subdirectories;
    }

    /**
     * Collects all subdirectories form all given directories and from all
     * directories
     * found down the directory tree. The initially given (array of) directories ARE
     * NOT
     * included in the final result.
     * 
     * @param dirs The directories, from which the collection must start.
     * @return All collected directories.
     */
    public ArrayList<Directory> extractDirectoriesFromDirAndSubDirs(Directory[] dirs) {
        ArrayList<Directory> allDirs = new ArrayList<Directory>();
        for (Directory dir : allDirs) {
            ArrayList<Directory> subdirs = extractDirectoriesFromDirAndSubDirs(dir);
            allDirs.addAll(subdirs);
        }
        return allDirs;
    }

    /**
     * Checks if the directory already includes a chunk with this originalFileName.
     * 
     * @param directory        The directory in which the check is conducted.
     * @param originalFileName The originalFileName, the availability of which must
     *                         be checked.
     * @return True if the directory includes a chunk with this originalFileName,
     *         false otherwise.
     */
    public Boolean doesDirectoryIncludeChunk(Directory directory, String originalFileName) {
        List<Chunk> chunks = directory.getChunks();
        for (Chunk c : chunks) {
            if (c.getOriginalFileName().equals(originalFileName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the given Directory includes a subdirectory with the same name as
     * the given nameToCheck.
     * 
     * @param directory   The Directory in which to check.
     * @param nameToCheck The name the availability of which must be checked.
     * @return True if a subdirectory (subDir.getName().equals(nameToCheck)) with
     *         the given nameToCheck exists in the directory, false otherwise.
     */
    public Boolean doesDirectoryIncludeSubdirectoryWithThisName(Directory directory, String nameToCheck) {
        List<Directory> allSubdirectories = directory.getSubdirectories();
        for (Directory subDir : allSubdirectories) {
            if (subDir.getName().equals(nameToCheck)) {
                return true;
            }
        }
        return false;
    }

    public Directory changeDirectoryName(Directory directory, String newName) {
        Directory parentDirectory = directory.getParentDirectory()
                .orElseThrow(() -> new FileManagementException("This directory can not be renamed."));

        // check directory name avilability
        if (doesDirectoryIncludeSubdirectoryWithThisName(parentDirectory, newName)) {
            throw new FileManagementException("A directory with this name already exists here.");
        }

        directory.setName(newName);
        return directoryRepository.save(directory);
    }
}
