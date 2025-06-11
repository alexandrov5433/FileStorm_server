package server.filestorm.service;

import org.springframework.stereotype.Service;

import server.filestorm.config.ServerConfigurationProperties;
import server.filestorm.exception.ConfigurationException;
import server.filestorm.exception.ProcessingException;

@Service
public class LinkGeneratorService {
    private String base_url;

    public LinkGeneratorService(ServerConfigurationProperties confProps) {
        if (confProps.getBaseUrl().trim().length() == 0) {
            throw new ConfigurationException("Base url must be a valid string.");
        }
        this.base_url = confProps.getBaseUrl();
    }

    public String generateFileSharingLink(Long fileId, String fileName) throws ProcessingException {
        if (fileId == null || fileName == null || fileName.trim().length() == 0) {
            throw new ProcessingException("File ID and name are required for the sharing link.");
        }
        return String.format("%1$s/api/download_shared_file/%2$d/%3$s",
            this.base_url, fileId, fileName);
    }
}
