package server.filestorm.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import server.filestorm.exception.FileManagementException;
import server.filestorm.model.entity.Chunk;
import server.filestorm.model.entity.Directory;
import server.filestorm.model.entity.User;
import server.filestorm.model.repository.ChunkRepository;
import server.filestorm.model.type.fileManagement.ChunkReference;
import server.filestorm.model.type.search.FileSearchResult;
import server.filestorm.model.type.search.UserFileSearchResults;
import server.filestorm.util.StringUtil;

@Service
public class ChunkService {

    @Autowired
    private ChunkRepository chunkRepository;

    public Chunk saveChunk(Chunk chunk) {
        return chunkRepository.save(chunk);
    }

    public Chunk findById(Long id) throws FileManagementException {
        return chunkRepository.findById(id)
                .orElseThrow(() -> new FileManagementException("A file with this ID was not found."));
    }

    /**
     * Finds a Chunk with the given id and with an owner reference of the given
     * owner.
     * 
     * @param chunk_id The id of the wanted Chunk.
     * @param owner    The presumable owner of the Chunk.
     * @return The Chunk if found.
     * @throws FileManagementException When a Chunk with this id was not found; when
     *                                 the given owner does not match the owner
     *                                 reference in the found Chunk.
     */
    public Chunk findChunkByIdAndOwner(Long chunkId, User owner) throws FileManagementException {
        return chunkRepository.findChunkByIdAndOwner(chunkId, owner)
                .orElseThrow(() -> new FileManagementException("A file with this ID was not found for this user."));
    }

    public Chunk findPublicChunkById(Long chunkId) throws FileManagementException {
        return chunkRepository.findPublicChunkById(chunkId)
                .orElseThrow(() -> new FileManagementException("Public file not found."));
    }

    public Chunk[] bulkCheckChunkOwnershipAndCollect(Long[] chunkIds, User owner) throws FileManagementException {
        if (chunkIds == null) {
            return new Chunk[0];
        }

        ArrayList<Chunk> chunks = new ArrayList<Chunk>();
        for (long chunkId : chunkIds) {
            chunks.add(findChunkByIdAndOwner(chunkId, owner));
        }
        return chunks.toArray(new Chunk[0]);
    }

    @Transactional
    public Chunk updateOriginalFileName(Chunk chunk, String newFileNameWithoutTheExtention)
            throws FileManagementException {
        String extention = StringUtil.extractFileExtention(chunk);
        String newOriginalFileName = newFileNameWithoutTheExtention + extention;

        // check name availability
        List<String> allFileNamesInDir = chunk.getDirectory().getChunks().stream()
                .map(Chunk::getOriginalFileName)
                .collect(Collectors.toList());

        for (String otherName : allFileNamesInDir) {
            if (otherName.equals(newOriginalFileName)) {
                throw new FileManagementException("A file with this name already exists in this directory.");
            }
        }

        chunk.setOriginalFileName(newOriginalFileName);
        return chunkRepository.save(chunk);
    }

    public void delete(Chunk chunk) {
        chunkRepository.delete(chunk);
    }

    public ChunkReference[] getFavoritesForUser(User user) {
        return chunkRepository.getFavoritesForUser(user)
                .map(chunkList -> chunkList.stream()
                        .map(ChunkReference::new)
                        .toArray(ChunkReference[]::new))
                .orElse(new ChunkReference[0]);
    }

    @Transactional
    public void markChunkAsFavorite(Chunk c) {
        c.setIsFavorite(true);
        chunkRepository.save(c);
    }

    @Transactional
    public void removeChunkFromFavorite(Chunk c) {
        c.setIsFavorite(false);
        chunkRepository.save(c);
    }

    @Transactional
    public boolean removeUserFromShareWith(Chunk c, User u) {
        return c.getShareWith().remove(u);
    }

    /**
     * Finds the Chunk by ID, shared with this User.
     * 
     * @param chunkId ID of the wanted Chunk.
     * @param u       The User with whom the Chunk must be shared.
     * @return The Chunk, if found and if its share_with Set contains this User.
     * @throws FileManagementException When the Chunk is not found by ID or when the
     *                                 found Chunk is not shared with the given
     *                                 User.
     */
    public Chunk findChunkSharedWithUser(Long chunkId, User u) {
        Chunk chunk = chunkRepository.findById(chunkId)
                .orElseThrow(() -> new FileManagementException("File not found."));
        boolean isChunkSharedWithUser = chunk.getShareWith().contains(u);
        if (!isChunkSharedWithUser) {
            new FileManagementException("This file is not shared with you.");
        }
        return chunk;
    }

    /**
     * Collects the Chunks for all given IDs if they are found and shered with the
     * given User.
     * 
     * @param chunkIds The wanted Chunks.
     * @param receiver The User with whom the Chunks must be shared.
     * @return All Chunks as an Array.
     * @throws FileManagementException
     */
    public Chunk[] bulkConfirmSharedWithMeAndCollect(Long[] chunkIds, User receiver) throws FileManagementException {
        if (chunkIds == null) {
            return new Chunk[0];
        }

        ArrayList<Chunk> chunks = new ArrayList<Chunk>();
        for (long chunkId : chunkIds) {
            chunks.add(findChunkSharedWithUser(chunkId, receiver));
        }
        return chunks.toArray(new Chunk[0]);
    }

    public UserFileSearchResults searchUserFiles(String searchValue, User searchingUser) {
        UserFileSearchResults results = new UserFileSearchResults();
        if (searchValue == null || searchValue.length() == 0) {
            return results;
        }

        List<Chunk> chunksForUser = chunkRepository.searchChunksForUser(searchValue, searchingUser)
                .orElse(new ArrayList<Chunk>());
        List<Chunk> sharedChunksForUser = chunkRepository.searchChunksSharedWithUser(searchValue, searchingUser)
                .orElse(new ArrayList<Chunk>());

        FileSearchResult[] chunksForMyStorage = chunksForUser
                .stream()
                .map((Chunk chunk) -> {
                    List<Object> directoryPath = extractDirPathInFormatForClient(chunk.getDirectory(), null);
                    ChunkReference chunkReference = new ChunkReference(chunk);
                    return new FileSearchResult(directoryPath.toArray(new Object[0]), chunkReference);
                })
                .collect(Collectors.toList())
                .toArray(new FileSearchResult[0]);

        FileSearchResult[] chunksForSharedWithMe = sharedChunksForUser
                .stream()
                .map((Chunk chunk) -> {
                    return new FileSearchResult(null, new ChunkReference(chunk));
                })
                .collect(Collectors.toList())
                .toArray(new FileSearchResult[0]);

        results.setMyStorageResults(chunksForMyStorage);
        results.setSharedWithMeResults(chunksForSharedWithMe);
        return results;
    }

    private List<Object> extractDirPathInFormatForClient(Directory dir, List<Object> dirPath) {
        if (dirPath == null) {
            dirPath = new ArrayList<Object>();
        }
        if (dir.getParentDirectory().isPresent()) {
            dirPath.addFirst(new Object[] { dir.getId(), dir.getName() });
            Directory parentDir = dir.getParentDirectory().get();
            return extractDirPathInFormatForClient(parentDir, dirPath);
        } else {
            dirPath.addFirst(new Object[] { dir.getId(), "My Storage" });
        }

        return dirPath;
    }
}
