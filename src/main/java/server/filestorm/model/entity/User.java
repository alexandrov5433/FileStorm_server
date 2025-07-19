package server.filestorm.model.entity;

import java.util.Set;

import jakarta.annotation.Nonnull;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Nonnull
    private String username;

    @Nonnull
    private String password;

    @Nonnull
    private String email;

    @Column(name = "max_storage_space", nullable = false)
    private Long maxStorageSpace = 53687091200L; // 50GB

    @Column(name = "bytes_in_storage", nullable = false)
    private Long bytesInStorage = 0L;

    @Column(name = "chunks_shared_with_me")
    @ManyToMany(mappedBy = "shareWith")
    private Set<Chunk> chunksSharedWithMe;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "root_storage_dir", referencedColumnName = "id")
    private Directory rootStorageDir; 
     

    @PrePersist
    public void prePersist() {
        if (this.bytesInStorage == null) {
            this.bytesInStorage = 0L;
        }
        if (this.maxStorageSpace == null) {
            this.maxStorageSpace = 53687091200L; // 50GB
        }
    }

    public Long getId() {
        return this.id;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getMaxStorageSpace() {
        return maxStorageSpace;
    }

    public void setMaxStorageSpace(Long maxStorageSpace) {
        this.maxStorageSpace = maxStorageSpace;
    }

    public Long getBytesInStorage() {
        return bytesInStorage;
    }

    public void setBytesInStorage(Long bytesInStorage) {
        this.bytesInStorage = bytesInStorage;
    }

    public Long getAvailableStorage() {
        return maxStorageSpace - bytesInStorage;
    }

    public Set<Chunk> getChunksSharedWithMe() {
        return chunksSharedWithMe;
    }

    public Directory getRootStorageDir() {
        return rootStorageDir;
    }

    public void setRootStorageDir(Directory rootStorageDir) {
        this.rootStorageDir = rootStorageDir;
    }
}
