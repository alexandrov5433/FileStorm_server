package server.filestorm.model.entity;

import java.util.HashSet;
import java.util.Set;

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
import jakarta.persistence.Table;

@Entity
@Table(name = "chunks")
public class Chunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "FK_user_id")
    @Nonnull
    private User owner;

    @Column(nullable = false, length = 2500)
    private String name;

    @Column(nullable = false, length = 2500)
    private String absolute_file_path;

    @Column(nullable = false, length = 2500)
    private String relative_file_path;

    @Column(nullable = false, updatable = false)
    private Long created_on;

    @Nonnull
    private Long size_bytes;

    @Nonnull
    private String mime_type;

    @Nonnull
    @Enumerated(EnumType.STRING)
    private ShareOption share_option = ShareOption.PRIVATE;

    public enum ShareOption {
        PRIVATE, SHARE_WITH_ALL_WITH_LINK, SHARE_WITH_USER
    }
    
    @ManyToMany
    @JoinTable(
        name = "chunk_shared_with",
        joinColumns = @JoinColumn(name = "chunk_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> share_with = new HashSet<>();

    @Column(nullable = true, length = 2500)
    private String share_link;


    @PrePersist
    private void onCreate() {
        this.created_on = System.currentTimeMillis();
        if (this.share_option == null) {
            this.share_option = ShareOption.PRIVATE;
        }
        if (this.share_with == null) {
            this.share_with = new HashSet<>();
        }
    }


    public Integer getId() {
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

    public void setName(String name) {
        this.name = name;
    }

    public String getAbsoluteFilePath() {
        return this.absolute_file_path;
    }

    public void setAbsoluteFilePath(String absolute_file_path) {
        this.absolute_file_path = absolute_file_path;
    }

    public String getRelativeFilePath() {
        return relative_file_path;
    }

    public void setRelativeFilePath(String relative_file_path) {
        this.relative_file_path = relative_file_path;
    }

    public Long getCreatedOn() {
        return this.created_on;
    }

    public Long getSizeBytes() {
        return size_bytes;
    }

    public void setSizeBytes(Long size_bytes) {
        this.size_bytes = size_bytes;
    }

    public String getMimeType() {
        return this.mime_type;
    }

    public void setMimeType(String mime_type) {
        this.mime_type = mime_type;
    }

    public ShareOption getShareOption() {
        return this.share_option;
    }

    public void setShareOption(ShareOption share_option) {
        this.share_option = share_option;
    }

    public String getShareLink() {
        return this.share_link;
    }

    public void setShareLink(String share_link) {
        this.share_link = share_link;
    }

    public Set<User> getShareWith() {
        return share_with;
    }

    public void setShareWith(Set<User> share_with) {
        this.share_with = share_with;
    }

    public void addUserToShareWithList(User u) {
        this.share_with.add(u);
    }

}
