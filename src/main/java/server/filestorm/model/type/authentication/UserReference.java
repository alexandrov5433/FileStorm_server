package server.filestorm.model.type.authentication;

import java.io.Serializable;

import server.filestorm.model.entity.User;

public class UserReference implements Serializable{
    private int id;
    private String username;
    private String email;
    private Long max_storage_space;
    private Long bytes_in_storage;

    public UserReference() {}

    public UserReference(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.max_storage_space = user.getMaxStorageSpace();
        this.bytes_in_storage = user.getBytesInStorage();
    }

    public int getId() {
        return id;
    }
    
    public void setId(int id) {
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

    public Long getMax_storage_space() {
        return max_storage_space;
    }

    public void setMax_storage_space(Long max_storage_space) {
        this.max_storage_space = max_storage_space;
    }

    public Long getBytes_in_storage() {
        return bytes_in_storage;
    }

    public void setBytes_in_storage(Long bytes_in_storage) {
        this.bytes_in_storage = bytes_in_storage;
    }
}
