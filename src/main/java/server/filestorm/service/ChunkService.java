package server.filestorm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import server.filestorm.exception.FileManagementException;
import server.filestorm.model.entity.Chunk;
import server.filestorm.model.entity.User;
import server.filestorm.model.repository.ChunkRepository;
import server.filestorm.model.type.fileManagement.ChunkReference;

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

    @Transactional
    public Chunk updateOriginalFileName(Chunk chunk, String newName) {
        chunk.setOriginalFileName(newName);
        return chunkRepository.save(chunk);
    }

    // public Chunk findChunkByNameAndOwner(String name, User owner) {
    //     return chunkRepository.findChunkByNameAndOwner(name, owner)
    //             .orElseThrow(() -> new FileManagementException("A file with this name was not found for this user."));
    // }

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
}
