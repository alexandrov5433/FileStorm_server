package server.filestorm.model.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import server.filestorm.model.entity.Chunk;
import server.filestorm.model.entity.User;

public interface ChunkRepository extends JpaRepository<Chunk, Integer>{
    
    @Query("SELECT c FROM Chunk c WHERE c.id = ?1 AND c.owner = ?2")
    Optional<Chunk> findChunkByIdAndOwner(Integer id, User owner);

    @Query("SELECT c FROM Chunk c WHERE c.name = ?1 AND c.owner = ?2")
    Optional<Chunk> findChunkByNameAndOwner(String name, User owner);

    @Modifying
    @Query("DELETE FROM Chunk c WHERE c.id = ?1 AND c.owner = ?2")
    Integer deleteChunkByIdAndOwner(Integer chunk_id, User owner);

    @Query("SELECT c FROM Chunk c WHERE c.owner = ?1 AND c.share_option != 'PRIVATE'")
    Optional<List<Chunk>> getFilesUserIsSharing(User owner);
}
