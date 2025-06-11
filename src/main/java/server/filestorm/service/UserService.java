package server.filestorm.service;

import java.util.LinkedHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import server.filestorm.exception.AuthenticationException;
import server.filestorm.model.entity.User;
import server.filestorm.model.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User saveNewUser(User user) {
        return userRepository.save(user);
    }

    public User findUserByUsername(String username) {
        return userRepository.findUserByUsername(username).orElse(null);
    }

    public User findUserByUsernameAndId(String username, Long id) {
        return userRepository.findUserByUsernameAndId(username, id).orElse(null);
    }

    /**
     * Checks is the given email is available - not taken by an other user - by searching for an existing user with this exact email.
     * @param email Email to search for.
     * @return true if the email may be used - is not already in use.
     */
    public Boolean isEmailAvailable(String email) {
        User user = userRepository.searchForUserWithThisEmail(email).orElse(null);
        return user == null ? true : false;
    }

    /**
     * Checks is the given username is available - not taken by an other user - by searching for an existing user with this exact username.
     * @param username The username to search for.
     * @return true if the username may be used - is not already in use.
     */
    public Boolean isUsernameAvailable(String username) {
        User user = userRepository.findUserByUsername(username).orElse(null);
        return user == null ? true : false;
    }

    /**
     * Finds a User with the given id.
     * 
     * @param userId The id of the User.
     * @return The User, if found.
     * @throws AuthenticationException When a User with this id can not be found.
     */
    public User findById(Long userId) throws AuthenticationException {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("User not found."));
    }

    // /**
    //  * Adds a new DirectoryReference in the storage reference of the User.
    //  * 
    //  * @param user             The User for whom the new DirectoryReference is.
    //  * @param newDirRef        The new DirectoryReference, which must be added.
    //  * @param targetSubDirPath The path to the directory in which the new
    //  *                         DirectoryReference (newDirRef) must be added.
    //  * @throws StorageException    When the targetSubDirPath is invalid.
    //  * @throws ProcessingException From
    //  *                             PathUtil.standardizeRelativePathString(targetSubDirPath).
    //  */
    // @Transactional(rollbackFor = StorageException.class)
    // public void addDirectory(User user, DirectoryReference newDirRef, String targetSubDirPath)
    //         throws StorageException {
    //     // "11/my_docs/work"
    //     DirectoryReference currentDir = user.getStorageDirectory(); // "11"
    //     String[] parts = PathUtil.standardizeRelativePathString(targetSubDirPath).split("/");

    //     for (int i = 1; i < parts.length; i++) {
    //         currentDir = currentDir.getDirectoryRefs().get(parts[i]);
    //         if (currentDir == null) {
    //             throw new StorageException("A directory with this name does not exists: " + parts[i]);
    //         }
    //     }
    //     if (currentDir.getDirectoryRefs().get(newDirRef.getName()) != null) {
    //         throw new StorageException("A directory with this name already exists.");
    //     }
    //     currentDir.getDirectoryRefs().put(newDirRef.getName(), newDirRef);

    //     userRepository.save(user);
    // }

    // /**
    //  * Deletes the DirectoryReference, its Chunks and all other DirectoryReferences
    //  * and their Chunks, which go deeper, from the User's storage reference. Also
    //  * deducts the used storage space for each Chunk in bytes from the User's
    //  * storage usage.
    //  * 
    //  * @param user          The targeted User.
    //  * @param targetDirPath The path to the DirectoryReference, which must be
    //  *                      deleted.
    //  * @return The deleted DirectoryReference.
    //  * @throws StorageException    When the targetDirPath is invalid.
    //  * @throws ProcessingException From
    //  *                             PathUtil.standardizeRelativePathString(targetDirPath)
    //  */
    // @Transactional(rollbackFor = StorageException.class)
    // public DirectoryReference deleteDirectory(User user, String targetDirPath)
    //         throws StorageException, ProcessingException {
    //     // "11/my_docs/work"
    //     DirectoryReference currentDir = user.getStorageDirectory(); // "11"
    //     String[] parts = PathUtil.standardizeRelativePathString(targetDirPath).split("/");

    //     for (int i = 1; i < parts.length - 1; i++) {
    //         // cicle until currentDir becomes the parent dir of the target dir
    //         currentDir = currentDir.getDirectoryRefs().get(parts[i]);
    //         if (currentDir == null) {
    //             throw new StorageException("A directory with this name does not exists: " + parts[i]);
    //         }
    //     }
    //     DirectoryReference deletedDirRef = currentDir.getDirectoryRefs().remove(parts[parts.length - 1]);
    //     // delete chunks from DB, which were in this dir and its subDirs
    //     Integer[] chunksForDeletion = collectAllChunkIdsFromDirAndDeeper(deletedDirRef);
    //     for (Integer chunkId : chunksForDeletion) {
    //         // remove bytes from user storage tracker
    //         Chunk c = chunkService.findById(chunkId);
    //         user.removeBytesInStorage(c.getSizeBytes());
    //         // delete chunk
    //         chunkService.deleteChunkByIdAndOwner(chunkId, user);
    //     }
    //     userRepository.save(user);
    //     return deletedDirRef;
    // }

    // /**
    //  * Deletes the Chunk from the DB and it's reference from the User's storage
    //  * regerence.
    //  * 
    //  * @param user            The User for whom the Chunk must be deleted.
    //  * @param targetDirPath   The path to the directory in the User's storage
    //  *                        reference where the Chunk is referenced.
    //  * @param targetChunkName The name of the Chunk to delete.
    //  * @return The ID of the deleted Chunk.
    //  * @throws StorageException    When the given targetDirPath is invalid.
    //  * @throws ProcessingException From
    //  *                             PathUtil.standardizeRelativePathString(targetDirPath).
    //  */
    // @Transactional(rollbackFor = StorageException.class)
    // public Integer deleteChunkAndRefFromDirRef(User user, String targetDirPath, String targetChunkName)
    //         throws StorageException, ProcessingException {
    //     // "11/my_docs/work"
    //     DirectoryReference currentDir = user.getStorageDirectory(); // "11"
    //     String[] parts = PathUtil.standardizeRelativePathString(targetDirPath).split("/");

    //     for (int i = 1; i < parts.length; i++) {
    //         currentDir = currentDir.getDirectoryRefs().get(parts[i]);
    //         if (currentDir == null) {
    //             throw new StorageException("A directory with this name does not exists: " + parts[i]);
    //         }
    //     }
    //     Integer deletedChunkId = currentDir.getChunkRefs().remove(targetChunkName);
    //     Chunk c = chunkService.findById(deletedChunkId);
    //     chunkService.deleteChunkByIdAndOwner(deletedChunkId, user);
    //     user.removeBytesInStorage(c.getSizeBytes());
    //     userRepository.save(user);
    //     return deletedChunkId;
    // }

    // private Integer[] collectAllChunkIdsFromDirAndDeeper(DirectoryReference dirRef) {
    //     ArrayList<Integer> ids = new ArrayList<Integer>();
    //     // add Chunk ids form this dir
    //     ids.addAll(dirRef.getChunkRefs().values());
    //     // get subdirs
    //     Collection<DirectoryReference> subDirRefs = dirRef.getDirectoryRefs().values();
    //     for (DirectoryReference subRef : subDirRefs) {
    //         // get Chunk ids from subdirs
    //         Integer[] subRefChunkIds = collectAllChunkIdsFromDirAndDeeper(subRef);
    //         ids.addAll(Arrays.asList(subRefChunkIds));
    //     }
    //     return ids.toArray(new Integer[0]);
    // }

    // /**
    //  * Saves the new Chunk in the DB and ads the reference in the user's storage
    //  * reference.
    //  * 
    //  * @param chunk         The Chunk to save.
    //  * @param user          The User in who's storage reference the Chunk reference
    //  *                      must be added.
    //  * @param targetDirPath The path to the target directory, where the Chunk
    //  *                      reference will be added to, in the User's storage
    //  *                      reference.
    //  * @return The new saved Chunk.
    //  * @throws StorageException    When the directory path is invalid.
    //  * @throws ProcessingException When PathUtil.standardizeRelativePathString()
    //  *                             throws.
    //  */
    // @Transactional(rollbackFor = StorageException.class)
    // public Chunk saveChunkAndAddChunkInDirRef(Chunk chunk, User user, String targetDirPath)
    //         throws StorageException, ProcessingException {
    //     Chunk newChunk = chunkService.saveChunk(chunk);
    //     // "11/my_docs/work"
    //     DirectoryReference currentDir = user.getStorageDirectory(); // "11"
    //     String[] parts = PathUtil.standardizeRelativePathString(targetDirPath).split("/");

    //     for (int i = 1; i < parts.length; i++) {
    //         currentDir = currentDir.getDirectoryRefs().get(parts[i]);
    //         if (currentDir == null) {
    //             throw new StorageException("A directory with this name does not exists: " + parts[i]);
    //         }
    //     }

    //     currentDir.addChunkRef(chunk.getName(), chunk.getId());
    //     user.addBytesInStorage(chunk.getSizeBytes());
    //     userRepository.save(user);
    //     return newChunk;
    // }

    // /**
    //  * Checks if the targeted directory exists in the user's directory reference. If
    //  * at least one part of the path string is invalid then the path is invalid.
    //  * 
    //  * @param user          User for which to check.
    //  * @param targetDirPath Path string to check.
    //  * @throws FileManagementException When the path string is invalid.
    //  */
    // public void verifyDirectoryExistance(User user, String targetDirPath) throws FileManagementException {
    //     // "11/my_docs/work"
    //     DirectoryReference currentDir = user.getStorageDirectory(); // "11"
    //     String[] parts = PathUtil.standardizeRelativePathString(targetDirPath).split("/");

    //     for (int i = 1; i < parts.length; i++) {
    //         currentDir = currentDir.getDirectoryRefs().get(parts[i]);
    //         if (currentDir == null) {
    //             throw new FileManagementException("This directory does not exist.");
    //         }
    //     }
    // }

    // /**
    //  * Checks if the targeted directory exists in the user's directory reference and
    //  * if a Chunk with the given name is included in the Chuk references of the
    //  * directory. If at least one part of the path string is invalid then the path
    //  * is invalid.
    //  * 
    //  * @param user          User for which to check.
    //  * @param targetDirPath Path string to check.
    //  * @throws FileManagementException When the path string is invalid.
    //  */
    // public void verifyChunkRefInDirRef(User user, String targetDirPath, String targetChunkName, Integer targetFileId)
    //         throws StorageException, ProcessingException {
    //     // "11/my_docs/work"
    //     DirectoryReference currentDir = user.getStorageDirectory(); // "11"
    //     String[] parts = PathUtil.standardizeRelativePathString(targetDirPath).split("/");

    //     for (int i = 1; i < parts.length; i++) {
    //         currentDir = currentDir.getDirectoryRefs().get(parts[i]);
    //         if (currentDir == null) {
    //             throw new StorageException("A directory with this name does not exists: " + parts[i]);
    //         }
    //     }
    //     if (currentDir.getChunkRefs().get(targetChunkName) != targetFileId) {
    //         throw new FileManagementException(
    //                 "A file with this name was not found in the directory: " + targetChunkName);
    //     }
    // }

    // /**
    //  * Returns HydratedDirectoryReference of a directory for a given User: a DirectoryReference with
    //  * complete Chunk data (hydratedChunkRefs) instead of Chunk references.
    //  * 
    //  * @param user          The User onwe of the directory.
    //  * @param targetDirPath The directory to hydrate.
    //  * @return The HydratedDirectoryReference of the directory.
    //  * @throws StorageException    If the given targetDirPath is invalid.
    //  * @throws ProcessingException From
    //  *                             PathUtil.standardizeRelativePathString(targetDirPath).
    //  */
    // @Transactional
    // public Optional<HydratedDirectoryReference> getHydratedDirectoryDataForUser(User user, String targetDirPath)
    //         throws StorageException, ProcessingException {
    //     DirectoryReference currentDir = user.getStorageDirectory();
    //     String[] parts = PathUtil.standardizeRelativePathString(targetDirPath).split("/");
    //     for (int i = 1; i < parts.length; i++) {
    //         currentDir = currentDir.getDirectoryRefs().get(parts[i]);
    //         if (currentDir == null) {
    //             throw new StorageException("A directory with this name does not exists: " + parts[i]);
    //         }
    //     }
    //     return Optional.ofNullable(hydrateChunkRefsFromDir(currentDir));
    // }

    // /**
    //  * Returns HydratedDirectoryReference of a directory for a given DirectoryReference: a DirectoryReference with
    //  * complete Chunk data (hydratedChunkRefs) instead of Chunk references.
    //  * 
    //  * @param dirRef The directory to hydrate.
    //  * @return The HydratedDirectoryReference of the directory.
    //  */
    // @Transactional
    // public Optional<HydratedDirectoryReference> getHydratedDirectoryDataFromDirRef(DirectoryReference dirRef) {
    //     return Optional.ofNullable(hydrateChunkRefsFromDir(dirRef));
    // }

    // @Transactional
    // private HydratedDirectoryReference hydrateChunkRefsFromDir(DirectoryReference dir) {
    //     HydratedDirectoryReference hydratedDirRef = new HydratedDirectoryReference(dir.getName(),
    //             dir.getDirectoryRefs());
    //     HashMap<String, Integer> chunkRefs = dir.getChunkRefs();
    //     chunkRefs.entrySet().forEach(entry -> {
    //         Integer fileId = entry.getValue();
    //         Chunk c = chunkService.findById(fileId);
    //         ChunkReference newChunkRef = new ChunkReference(c);
    //         hydratedDirRef.addHydratedChunkRef(newChunkRef);
    //     });
    //     return hydratedDirRef;
    // }

    // /**
    //  * Changes the name of the Chunk and the name of its reference in the user
    //  * storage reference.
    //  * 
    //  * @param c           The Chunk to rename.
    //  * @param u           The User, owner of the Chunk.
    //  * @param newFileName The new name of the Chunk.
    //  * @return A ChunkReference of the updated Chunk.
    //  * @throws StorageException    When the relative file path extracted from the
    //  *                             Chunk is invalid.
    //  * @throws ProcessingException
    //  */
    // @Transactional
    // public ChunkReference changeChunkRefName(Chunk c, User u, String newFileName)
    //         throws StorageException, ProcessingException {
    //     DirectoryReference currentDir = u.getStorageDirectory();
    //     String[] parts = PathUtil.standardizeRelativePathString(
    //             c.getRelativeFilePath()).split("/");
    //     for (int i = 1; i < parts.length; i++) {
    //         currentDir = currentDir.getDirectoryRefs().get(parts[i]);
    //         if (currentDir == null) {
    //             throw new StorageException("A directory with this name does not exists: " + parts[i]);
    //         }
    //     }
    //     currentDir.getChunkRefs().remove(c.getName(), c.getId());
    //     currentDir.getChunkRefs().put(newFileName, c.getId());
    //     c.setName(newFileName);
    //     chunkService.saveChunk(c);
    //     return new ChunkReference(c);
    // }

    public LinkedHashMap<String, Long> queryUsersByName(String username) {
        // null-check username
        username = username == null ? "" : username;

        // escape username
        username = username.replaceAll("[^A-Za-z0-9_]", "");

        LinkedHashMap<String, Long> users = new LinkedHashMap<>();

        // no need to query on empty string; return empty result
        if (username.equals(""))
            return users;

        userRepository.queryUsersByUsername(username).ifPresent(queryResult -> {
            for (Object[] el : queryResult) {
                users.put((String) el[0], (Long) el[1]);
            }
        });
        return users;
    }
}
