package server.filestorm.model.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import jakarta.annotation.Nonnull;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import server.filestorm.exception.ProcessingException;

@Entity
@Table(name = "directories")
public class Directory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "FK_user_id")
    @Nonnull
    private User owner;

    @Column(name = "name", nullable = false, length = 400)
    private String name;

    @Column(name = "elements_count", nullable = false)
    private Integer elementsCount = 0;

    @OneToMany(mappedBy = "directory", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.EAGER)
    private List<Chunk> chunks = new ArrayList<Chunk>();

    @ManyToOne
    @JoinColumn(name = "parentDirectory_id", nullable = true)
    private Directory parentDirectory;

    // fetch = FetchType.EAGER fixes:
    // org.hibernate.LazyInitializationException: failed to lazily initialize a collection of role: server.filestorm.model.entity.Directory.subdirectories: could not initialize proxy - no Session
    @OneToMany(mappedBy = "parentDirectory", cascade = CascadeType.ALL, orphanRemoval = false, fetch = FetchType.EAGER)
    private List<Directory> subdirectories = new ArrayList<Directory>();

    @Column(name = "created_on", nullable = false, updatable = false)
    private Long createdOn = new Date().getTime();

    @Column(name = "last_modified", nullable = false)
    private Long lastModified = new Date().getTime();

    @PrePersist
    private void prePersist() {
        if (this.createdOn == null) {
            this.createdOn = new Date().getTime();
        }
        if (this.chunks == null) {
            this.chunks = new ArrayList<Chunk>();
        }
        if (this.subdirectories == null) {
            this.subdirectories = new ArrayList<Directory>();
        }
        if (this.elementsCount == null) {
            this.elementsCount = 0;
        }
        if (this.lastModified == null) {
            this.lastModified = new Date().getTime();
        }
    }

    @PreUpdate
    private void preUpdate() {
        this.lastModified = new Date().getTime();
    }

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getElementsCount() {
        return elementsCount;
    }

    public List<Chunk> getChunks() {
        return chunks;
    }

    public void addChunk(Chunk chunk) {
        this.chunks.add(chunk);
    }

    public boolean removeChunk(Chunk chunk) {
        return this.chunks.remove(chunk);
    }

    public Optional<Directory> getParentDirectory() {
        return Optional.ofNullable(parentDirectory);
    }

    public void setParentDirectory(Directory parentDirectory) {
        if (parentDirectory == this) {
            throw new ProcessingException("A directory can not be it's own parent.");
        }
        if (this.parentDirectory != null) {
            this.parentDirectory.getSubdirectories().remove(this);
        }
        this.parentDirectory = parentDirectory;
        if (parentDirectory != null && !parentDirectory.getSubdirectories().contains(this)) {
            parentDirectory.getSubdirectories().add(this);
        }
    }

    public List<Directory> getSubdirectories() {
        return subdirectories;
    }

    public void addSubdirectory(Directory directory) {
        this.subdirectories.add(directory);
        directory.setParentDirectory(this);
    }

    public boolean removeSubdirectory(Directory directory) {
        boolean isRemoved = this.subdirectories.remove(directory);
        if (isRemoved) {
            directory.setParentDirectory(null);
        }
        return isRemoved;
    }

    public Long getCreatedOn() {
        return createdOn;
    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }

    public Long getLastModified() {
        return lastModified;
    }
}
