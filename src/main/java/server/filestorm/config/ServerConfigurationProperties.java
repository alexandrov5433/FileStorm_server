package server.filestorm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("filestorm-server-config")
public class ServerConfigurationProperties {
    private String domain;
    private String file_storage_location;
    private String client_location;
    private String jwt_secret;
    private String available_storage_per_account_gb;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getFileStorageLocation() {
        return file_storage_location;
    }

    public void setFileStorageLocation(String file_storage_location) {
        this.file_storage_location = file_storage_location;
    }

    public String getClientLocation() {
        return this.client_location;
    }

    public void setClientLocation(String client_location) {
        this.client_location = client_location;
    }

    public String getJwtSecret() {
        return this.jwt_secret;
    }

    public void setJwtSecret(String jwt_secret) {
        this.jwt_secret = jwt_secret;
    }

    public int getAvailableStoragePerAccountGb() {
        return Integer.parseInt(available_storage_per_account_gb);
    }

    public Long getAvailableStoragePerAccountBytes() {
        return Long.parseLong(available_storage_per_account_gb) * 1024 * 1024 * 1024;
    }

    public void setAvailableStoragePerAccountGb(String gb) {
        this.available_storage_per_account_gb = gb;
    }
}
