package server.filestorm.service;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import server.filestorm.config.ServerConfigurationProperties;
import server.filestorm.exception.StorageException;

@Service
public class ClientService {
    
    private Path clientRootLocation;
    
    public ClientService(ServerConfigurationProperties confProps) {
        if (confProps.getClientLocation().trim().length() == 0) {
            throw new StorageException("Client file location can not be empty.");
        }
        
        this.clientRootLocation = Paths.get(confProps.getClientLocation());
    }
    
    public Resource loadClientHtmlAsResource() {
        try {
            Resource resource = new UrlResource(this.clientRootLocation.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new StorageException("Could not read client HTML file.");
            }
        } catch (MalformedURLException e) {
            throw new StorageException("Could not read client HTML file.", e);
        }
    }
}
