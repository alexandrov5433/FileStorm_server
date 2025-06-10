package server.filestorm.model.entity;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.Length;

import jakarta.annotation.Nonnull;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "chunks")
public class Chunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "FK_user_id")
    @Nonnull
    private User owner;

    @Column(nullable = false, length = 2500)
    private String name;

    @Column(name = "original_file_name", nullable = false, length = 2500)
    private String originalFileName;

    @Column(name = "absolute_file_path", nullable = false, length = Length.LONG32)
    // 32-bit (2147483647) string == TEXT type in PostgreSQL
    private String absoluteFilePath;

    @Column(name = "created_on", nullable = false, updatable = false)
    private Long createdOn = new Date().getTime();

    @Column(name = "last_modified", nullable = false)
    private Long lastModified = new Date().getTime();

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "share_option", nullable = false)
    @Enumerated(EnumType.STRING)
    private ShareOption shareOption = ShareOption.PRIVATE;

    public enum ShareOption {
        PRIVATE, SHARE_WITH_ALL_WITH_LINK, SHARE_WITH_USER
    }

    @ManyToMany
    @JoinTable(name = "chunk_shared_with", joinColumns = @JoinColumn(name = "chunk_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "share_with", nullable = false)
    private Set<User> shareWith = new HashSet<>();

    @Column(name = "share_link", nullable = true, length = 2500)
    private String shareLink;

    @Column(name = "is_favorite", nullable = false)
    private Boolean isFavorite = false;

    @ManyToOne
    @JoinColumn(name = "directory_id")
    private Directory directory;

    @PrePersist
    private void prePersist() {
        if (this.createdOn == null) {
            this.createdOn = new Date().getTime();
        }
        if (this.shareOption == null) {
            this.shareOption = ShareOption.PRIVATE;
        }
        if (this.shareWith == null) {
            this.shareWith = new HashSet<>();
        }
        if (this.isFavorite == null) {
            this.isFavorite = false;
        }
    }

    @PreUpdate
    private void preUpdate() {
        this.lastModified = new Date().getTime();
    }

    public Long getId() {
        return this.id;
    }

    public User getOwner() {
        return this.owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public String getName() {
        return this.name;
    }

    public String getOriginalFileName() {
        return originalFileName;
    }

    public void setOriginalFileName(String originalFileName) {
        this.originalFileName = originalFileName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAbsoluteFilePath() {
        return absoluteFilePath;
    }

    public void setAbsoluteFilePath(String absoluteFilePath) {
        this.absoluteFilePath = absoluteFilePath;
    }

    public Long getCreatedOn() {
        return createdOn;
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

    public String getShareLink() {
        return shareLink;
    }

    public void setShareLink(String shareLink) {
        this.shareLink = shareLink;
    }

    public Set<User> getShareWith() {
        return shareWith;
    }

    public void setShareWith(Set<User> shareWith) {
        this.shareWith = shareWith;
    }

    public void addUserToShareWithList(User u) {
        this.shareWith.add(u);
    }

    public Boolean getIsFavorite() {
        return isFavorite;
    }

    public void setIsFavorite(Boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    public Directory getDirectory() {
        return directory;
    }

    public void setDirectory(Directory directory) {
        this.directory = directory;
    }

}
