package server.filestorm.model.entity;

import java.util.Set;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import server.filestorm.exception.StorageException;
import server.filestorm.model.type.fileManagement.DirectoryReference;
import server.filestorm.util.DirectoryReferenceConverter;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Nonnull
    private String username;

    @Nonnull
    private String password;

    @Nonnull
    private String email;

    private Long max_storage_space = Long.valueOf("53687091200"); // 50GB

    private Long bytes_in_storage = Long.valueOf("0");

    @ManyToMany(mappedBy = "share_with")
    private Set<Chunk> chunks_shared_with_me;

    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = DirectoryReferenceConverter.class)
    private DirectoryReference storage_directory;
     

    @PrePersist
    public void prePersist() {
        if (this.bytes_in_storage == null) {
            this.bytes_in_storage = Long.valueOf("0");
        }
        if (this.max_storage_space == null) {
            this.max_storage_space = Long.valueOf("53687091200"); // 50GB
        }
    }

    public Integer getId() {
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
        return this.max_storage_space;
    }

    public void setMaxStorageSpace(Long spaceInBytes) {
        this.max_storage_space = spaceInBytes;
    }

    public Long getBytesInStorage() {
        return this.bytes_in_storage;
    }

    public void setBytesInStorage(Long bytesInStorage) {
        this.bytes_in_storage = bytesInStorage;
    }

    public DirectoryReference getStorageDirectory() {
        return this.storage_directory;
    }

    public void setStorageDirectory(DirectoryReference storage_directory) {
        this.storage_directory = storage_directory;
    }

    public Long addBytesInStorage(Long bytesToAdd) throws StorageException {
        if (bytesToAdd < 0) {
            throw new StorageException("Can not add a negative value to the storage tracker.");
        }
        this.bytes_in_storage += bytesToAdd;
        return this.bytes_in_storage;
    }

    public Long removeBytesInStorage(Long bytesToRemove) {
        this.bytes_in_storage -= bytesToRemove;
        if (this.bytes_in_storage < (long) 0) {
            this.bytes_in_storage = (long) 0;
        }
        return this.bytes_in_storage;
    }

    public Long getAvailableStorage() {
        return this.max_storage_space - this.bytes_in_storage;
    }

    public Set<Chunk> getChunksSharedWithMe() {
        return chunks_shared_with_me;
    }
}
