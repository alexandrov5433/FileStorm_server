package server.filestorm.model.type.fileManagement;

import java.io.Serializable;
import java.util.stream.Collectors;

import server.filestorm.model.entity.Chunk;
import server.filestorm.model.entity.Directory;

public class DirectoryReference implements Serializable {

    private Long id;
    private Long ownerId;
    private String name;
    private Integer elementsCount;
    private Long[] chunks;
    private Long[] subdirectories;
    private Long parentDirectoryId;
    private Long createdOn;
    private Long lastModified;

    public DirectoryReference() {
        this.id = null;
        this.ownerId = null;
        this.name = null;
        this.elementsCount = null;
        this.chunks = new Long[0];
        this.subdirectories = new Long[0];
        this.parentDirectoryId = null;
        this.createdOn = null;
        this.lastModified = null;
    }

    public DirectoryReference(Directory dir) {
        this.id = dir.getId();
        this.ownerId = dir.getOwner().getId();
        this.name = dir.getName();
        this.elementsCount = dir.getElementsCount();
        this.chunks = dir.getChunks().stream()
                .map(Chunk::getId)
                .collect(Collectors.toList())
                .toArray(new Long[0]);
        this.subdirectories = dir.getSubdirectories().stream()
                .map(Directory::getId)
                .collect(Collectors.toList())
                .toArray(new Long[0]);
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

    public Long[] getChunks() {
        return chunks;
    }

    public void setChunks(Long[] chunks) {
        this.chunks = chunks;
    }

    public Long[] getSubdirectories() {
        return subdirectories;
    }

    public void setSubdirectories(Long[] subdirectories) {
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
