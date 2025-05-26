package server.filestorm.model.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import server.filestorm.model.entity.User;

public interface UserRepository extends JpaRepository<User, Integer> {
    
    @Query("SELECT u FROM User u WHERE u.username = ?1")
    Optional<User> findUserByUsername(String username);

    @Query("SELECT u.username, u.id FROM User u WHERE u.username ILIKE CONCAT('%', ?1, '%') ORDER BY u.username ASC")
    Optional<List<Object[]>> queryUsersByUsername(String username);
}