package server.filestorm.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import server.filestorm.model.entity.Directory;

public interface DirectoryRepository extends JpaRepository<Directory, Long>{
    
}
