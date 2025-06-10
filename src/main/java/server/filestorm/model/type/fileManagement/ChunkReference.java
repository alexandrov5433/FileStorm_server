package server.filestorm.model.type.fileManagement;

import java.io.Serializable;
import java.util.stream.Collectors;

import server.filestorm.model.entity.Chunk;
import server.filestorm.model.entity.User;
import server.filestorm.model.entity.Chunk.ShareOption;

public class ChunkReference implements Serializable {

    private Long id;
    private Long ownerId;
    private String originalFileName;
    private Long createdOn;
    private Long lastModified;
    private Long sizeBytes;
    private String mimeType;
    private ShareOption shareOption;
    private Long[] shareWith;
    private String shareLink;
    private Boolean isFavorite;
    private Long directory;

    public ChunkReference() {
        this.id = null;
        this.ownerId = null;
        this.originalFileName = null;
        this.createdOn = null;
        this.lastModified = null;
        this.sizeBytes = null;
        this.mimeType = null;
        this.shareOption = null;
        this.shareWith = new Long[0];
        this.shareLink = null;
        this.isFavorite = null;
        this.directory = null;
    };

    public ChunkReference(Chunk chunk) {
        this.id = chunk.getId();
        this.ownerId = chunk.getOwner().getId();
        this.originalFileName = chunk.getOriginalFileName();
        this.createdOn = chunk.getCreatedOn();
        this.lastModified = chunk.getLastModified();
        this.sizeBytes = chunk.getSizeBytes();
        this.mimeType = chunk.getMimeType();
        this.shareOption = chunk.getShareOption();
        this.shareWith = chunk.getShareWith().stream()
            .map(User::getId)
            .collect(Collectors.toList())
            .toArray(new Long[0]);
        this.shareLink = chunk.getShareLink();
        this.isFavorite = chunk.getIsFavorite();
        this.directory = chunk.getDirectory().getId();
    };

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

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
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

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public ShareOption getShareOption() {
        return shareOption;
    }

    public void setShareOption(ShareOption shareOption) {
        this.shareOption = shareOption;
    }

    public Long[] getShareWith() {
        return shareWith;
    }
    
    public void setShareWith(Long[] shareWith) {
        this.shareWith = shareWith;
    }

    public String getShareLink() {
        return shareLink;
    }

    public void setShareLink(String shareLink) {
        this.shareLink = shareLink;
    }

    public Boolean getIsFavorite() {
        return isFavorite;
    }

    public void setIsFavorite(Boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    public Long getDirectory() {
        return directory;
    }

    public void setDirectory(Long directory) {
        this.directory = directory;
    }
}
