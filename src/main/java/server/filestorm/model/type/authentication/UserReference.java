package server.filestorm.model.type.authentication;

import java.io.Serializable;

import server.filestorm.model.entity.Directory;
import server.filestorm.model.entity.User;

public class UserReference implements Serializable {
    private Long id;
    private String username;
    private String email;
    private Long maxStorageSpace;
    private Long bytesInStorage;
    private Long rootStorageDir;

    public UserReference() {
        this.id = null;
        this.username = null;
        this.email = null;
        this.maxStorageSpace = null;
        this.bytesInStorage = null;
        this.rootStorageDir = null;
    }

    public UserReference(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.maxStorageSpace = user.getMaxStorageSpace();
        this.bytesInStorage = user.getBytesInStorage();
        this.rootStorageDir = user.getRootStorageDir().getId();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public Long getRootStorageDir() {
        return rootStorageDir;
    }

    public void setRootStorageDir(Long rootStorageDir) {
        this.rootStorageDir = rootStorageDir;
    }

    public void setRootStorageDir(Directory rootStorageDir) {
        this.rootStorageDir = rootStorageDir.getId();
    }
}
