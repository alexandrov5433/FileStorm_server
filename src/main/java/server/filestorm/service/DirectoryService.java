package server.filestorm.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import server.filestorm.exception.StorageException;
import server.filestorm.model.entity.Chunk;
import server.filestorm.model.entity.Directory;
import server.filestorm.model.entity.User;
import server.filestorm.model.repository.DirectoryRepository;

@Service
public class DirectoryService {

    @Autowired
    private DirectoryRepository directoryRepository;

    /**
     * Creates a new root directory for the new user in DB.
     * 
     * @param name             The name of the new root directory.
     * @param owner            The user owner of the root directory.
     * @return The newly created directory.
     */
    @Transactional
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
     * @param name             The name of the new directory.
     * @param owner            The user owner of the directory.
     * @return The newly created directory.
     */
    @Transactional
    public Directory createNewDirectory(String name, User owner, Directory parentDirectory) {
        Directory dir = new Directory();
        dir.setName(name);
        dir.setOwner(owner);
        dir.setParentDirectory(parentDirectory);
        return directoryRepository.save(dir);
    }

    public Integer deleteDirectoryForUserById(Long directoryId, User owner) {
        return directoryRepository.deleteDirectoryForUserById(directoryId, owner);
    }

    public Directory findDirectoryForUserById(Long directoryId, User owner) {
        return directoryRepository.findDirectoryForUserById(directoryId, owner)
            .orElseThrow(() -> new StorageException("Directory could not be found."));
    }

    /**
     * Collects all chunks from the given directory, it´s subdirectories and all other subdirectories down the directory tree.
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
     * Collects all subdirectories form the given directory and from all directories found down the directory tree. The initially given (first) directory IS NOT included in the final result.
     * @param dir The initial (first) directory, from which the collection must start. 
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

}
