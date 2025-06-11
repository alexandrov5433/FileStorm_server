package server.filestorm.model.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import server.filestorm.model.entity.Directory;
import server.filestorm.model.entity.User;

public interface DirectoryRepository extends JpaRepository<Directory, Long>{

    @Query("SELECT d FROM Directory d WHERE d.id = ?1 AND d.owner = ?2")
    Optional<Directory> findDirectoryForUserById(Long directoryId, User owner);

    @Query("DELETE FROM Directory d WHERE d.id = ?1 AND d.owner = ?2")
    Integer deleteDirectoryForUserById(Long directoryId, User owner);
}
