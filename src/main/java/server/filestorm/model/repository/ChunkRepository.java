package server.filestorm.model.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import server.filestorm.model.entity.Chunk;
import server.filestorm.model.entity.User;

public interface ChunkRepository extends JpaRepository<Chunk, Long>{
    
    @Query("SELECT c FROM Chunk c WHERE c.id = ?1 AND c.owner = ?2")
    Optional<Chunk> findChunkByIdAndOwner(Long id, User owner);
    
    @Query("SELECT c FROM Chunk c WHERE c.id = ?1 AND c.shareOption = 'SHARE_WITH_ALL_WITH_LINK'")
    Optional<Chunk> findPublicChunkById(Long id);

    @Query("SELECT c FROM Chunk c WHERE c.owner = ?1 AND c.shareOption != 'PRIVATE'")
    Optional<List<Chunk>> getFilesUserIsSharing(User owner);

    @Query("SELECT c FROM Chunk c WHERE c.isFavorite = TRUE AND c.owner = ?1")
    Optional<List<Chunk>> getFavoritesForUser(User owner);

    @Query("SELECT c FROM Chunk c WHERE LOWER(c.originalFileName) LIKE LOWER(CONCAT('%', ?1, '%')) AND c.owner = ?2 ORDER BY c.originalFileName ASC")
    Optional<List<Chunk>> searchChunksForUser(String searchValue, User owner);

    @Query("SELECT c FROM Chunk c JOIN c.shareWith u WHERE LOWER(c.originalFileName) LIKE LOWER(CONCAT('%', ?1, '%')) AND u = ?2 ORDER BY c.originalFileName ASC")
    Optional<List<Chunk>> searchChunksSharedWithUser(String seachValue, User user);
}
