package server.filestorm.model.type.fileManagement;

import java.io.Serializable;
import java.util.stream.Collectors;

import server.filestorm.model.entity.Directory;

public class HydratedDirectoryReference implements Serializable {
    private Long id;
    private Long ownerId;
    private String name;
    private Integer elementsCount;
    private ChunkReference[] hydratedChunks;
    private DirectoryReference[] subdirectories;
    private Long parentDirectoryId;
    private Long createdOn;
    private Long lastModified;

    public HydratedDirectoryReference() {
        this.id = null;
        this.ownerId = null;
        this.name = null;
        this.elementsCount = null;
        this.hydratedChunks = new ChunkReference[0];
        this.subdirectories = new DirectoryReference[0];
        this.parentDirectoryId = null;
        this.createdOn = null;
        this.lastModified = null;
    }

    public HydratedDirectoryReference(Directory dir) {
        this.id = dir.getId();
        this.ownerId = dir.getOwner().getId();
        this.name = dir.getName();
        this.elementsCount = dir.getElementsCount();
        this.hydratedChunks = dir.getChunks().stream()
            .map(ChunkReference::new)
            .collect(Collectors.toList())
            .toArray(new ChunkReference[0]);
        this.subdirectories = dir.getSubdirectories().stream()
            .map(DirectoryReference::new)
            .collect(Collectors.toList())
            .toArray(new DirectoryReference[0]);
        this.parentDirectoryId = dir.getParentDirectory().isPresent() ? dir.getParentDirectory().get().getId() : null;
        this.createdOn = dir.getCreatedOn();
        this.lastModified = dir.getLastModified();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getElementsCount() {
        return elementsCount;
    }

    public void setElementsCount(Integer elementsCount) {
        this.elementsCount = elementsCount;
    }

    public ChunkReference[] getHydratedChunks() {
        return hydratedChunks;
    }

    public void setHydratedChunks(ChunkReference[] hydratedChunks) {
        this.hydratedChunks = hydratedChunks;
    }

    public DirectoryReference[] getSubdirectories() {
        return subdirectories;
    }

    public void setSubdirectories(DirectoryReference[] subdirectories) {
        this.subdirectories = subdirectories;
    }

    public Long getParentDirectoryId() {
        return parentDirectoryId;
    }

    public void setParentDirectoryId(Long parentDirectoryId) {
        this.parentDirectoryId = parentDirectoryId;
    }

    public Long getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Long createdOn) {
        this.createdOn = createdOn;
    }

    public Long getLastModified() {
        return lastModified;
    }

    public void setLastModified(Long lastModified) {
        this.lastModified = lastModified;
    }
}
