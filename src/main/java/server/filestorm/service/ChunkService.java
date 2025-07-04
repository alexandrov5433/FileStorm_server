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
    public Chunk updateOriginalFileName(Chunk chunk, String newFileNameWithoutTheExtention) throws FileManagementException {
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

    public Chunk findChunkSharedWithUser(Long chunkId, User u) {
        Chunk chunk = chunkRepository.findById(chunkId)
                .orElseThrow(() -> new FileManagementException("File not found."));
        boolean isChunkSharedWithUser = chunk.getShareWith().contains(u);
        if (!isChunkSharedWithUser) {
            new FileManagementException("This file is not shared with you.");
        }
        return chunk;
    }
}
