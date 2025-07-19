package server.filestorm.model.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import server.filestorm.model.entity.Directory;
import server.filestorm.model.entity.User;

public interface DirectoryRepository extends JpaRepository<Directory, Long> {

    @Query("SELECT d FROM Directory d WHERE d.id = ?1 AND d.owner = ?2")
    Optional<Directory> findDirectoryForUserById(Long directoryId, User owner);

    @Modifying
    @Query("UPDATE Directory d SET d.elementsCount = d.elementsCount + 1 WHERE d.id = ?1")
    void incrementElementsCountByOne(Long id);

    @Modifying
    @Query(value = """
                UPDATE directories
                SET elements_count = CASE WHEN elements_count > 0 THEN elements_count - 1 ELSE 0 END
                WHERE id = ?1
            """, nativeQuery = true)
    void decrementElementsCountByOne(Long id);
}
