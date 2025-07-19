package server.filestorm.model.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import server.filestorm.model.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    
    @Query("SELECT u FROM User u WHERE u.username = ?1")
    Optional<User> findUserByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.username = ?1 AND u.id = ?2")
    Optional<User> findUserByUsernameAndId(String username, Long id);
    
    @Query("SELECT u.username, u.id FROM User u WHERE u.username ILIKE CONCAT('%', ?1, '%') ORDER BY u.username ASC")
    Optional<List<Object[]>> queryUsersByUsername(String username);

    @Query("SELECT u FROM User u WHERE u.email = ?1")
    Optional<User> searchForUserWithThisEmail(String emial);

    @Modifying
    @Query("UPDATE User u SET u.bytesInStorage = u.bytesInStorage + ?2 WHERE u.id = ?1")
    void increaseBytesInStorage(Long id, Long bytesToAdd);

    @Modifying
    @Query(value = """
            UPDATE users SET bytes_in_storage = CASE WHEN bytes_in_storage - ?2 < 0 THEN 0 ELSE bytes_in_storage - ?2 END
            WHERE id = ?1 
            """, nativeQuery = true)
    void decreaseBytesInStorage(Long id, Long bytesToRemove);

    @Query("SELECT u.bytesInStorage FROM User u WHERE u.id = ?1")
    Optional<Long> getCurrentBytesInStorage(Long id);
}