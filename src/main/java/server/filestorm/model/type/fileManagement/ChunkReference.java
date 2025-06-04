package server.filestorm.model.type.fileManagement;

import java.io.Serializable;

import server.filestorm.model.entity.Chunk;
import server.filestorm.model.entity.Chunk.ShareOption;

public class ChunkReference implements Serializable{
    
    private Integer id;
    private Integer owner;
    private String name;
    private String relative_file_path;
    private Long created_on;
    private Long size_bytes;
    private String mime_type;
    private ShareOption share_option;
    private String share_link;
    private Boolean is_favorite;

    public ChunkReference() {};

    public ChunkReference(Chunk chunk) {
        this.id = chunk.getId();
        this.owner = chunk.getOwner().getId();
        this.name = chunk.getName();
        this.relative_file_path = chunk.getRelativeFilePath();
        this.created_on = chunk.getCreatedOn();
        this.size_bytes = chunk.getSizeBytes();
        this.mime_type = chunk.getMimeType();
        this.share_option = chunk.getShareOption();
        this.share_link = chunk.getShareLink();
        this.is_favorite = chunk.getIsFavorite();
    };

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getOwner() {
        return owner;
    }

    public void setOwner(Integer owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRelativeRilePath() {
        return relative_file_path;
    }

    public void setRelativeRilePath(String relative_file_path) {
        this.relative_file_path = relative_file_path;
    }

    public Long getCreated_on() {
        return created_on;
    }

    public void setCreated_on(Long created_on) {
        this.created_on = created_on;
    }

    public Long getSize_bytes() {
        return size_bytes;
    }

    public void setSize_bytes(Long size_bytes) {
        this.size_bytes = size_bytes;
    }

    public String getMime_type() {
        return mime_type;
    }

    public void setMime_type(String mime_type) {
        this.mime_type = mime_type;
    }

    public ShareOption getShare_option() {
        return share_option;
    }

    public void setShare_option(ShareOption share_option) {
        this.share_option = share_option;
    }

    public String getShare_link() {
        return share_link;
    }

    public void setShare_link(String share_link) {
        this.share_link = share_link;
    }

    public Boolean getIs_favorite() {
        return is_favorite;
    }

    public void setIs_favorite(Boolean is_favorite) {
        this.is_favorite = is_favorite;
    }
}
