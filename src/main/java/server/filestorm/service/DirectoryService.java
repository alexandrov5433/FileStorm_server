package server.filestorm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import server.filestorm.model.entity.Directory;
import server.filestorm.model.entity.User;
import server.filestorm.model.repository.DirectoryRepository;

@Service
public class DirectoryService {

    @Autowired
    private DirectoryRepository directoryRepository;
    
    /**
     * Creates a new root directory for the new user in DB.
     * @param name The name of the new root directory.
     * @param absoluteFilePath The absolute file path to the root directory.
     * @param owner The user owner of the root directory.
     * @return The newly created directory.
     */
    @Transactional
    public Directory createNewDirectory(String name, String absoluteFilePath, User owner) {
        Directory dir = new Directory();
        dir.setName(name);
        dir.setOwner(owner);
        dir.setAbsoluteFilePath(absoluteFilePath);
        dir.setParentDirectory(null);
        return directoryRepository.save(dir);
    }

    /**
     * Creates a new directory (subdirectory) for the user in DB.
     * @param name The name of the new directory.
     * @param absoluteFilePath The absolute file path to the directory.
     * @param owner The user owner of the directory.
     * @return The newly created directory.
     */
    @Transactional
    public Directory createNewDirectory(String name, String absoluteFilePath, User owner, Directory parentDirectory) {
        Directory dir = new Directory();
        dir.setName(name);
        dir.setOwner(owner);
        dir.setAbsoluteFilePath(absoluteFilePath);
        dir.setParentDirectory(parentDirectory);
        return directoryRepository.save(dir);
    }
}
