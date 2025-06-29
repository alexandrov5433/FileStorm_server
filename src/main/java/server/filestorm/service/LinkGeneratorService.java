package server.filestorm.service;

import org.springframework.stereotype.Service;

import server.filestorm.config.ServerConfigurationProperties;
import server.filestorm.exception.ConfigurationException;
import server.filestorm.exception.ProcessingException;

@Service
public class LinkGeneratorService {
    private String domain;

    public LinkGeneratorService(ServerConfigurationProperties confProps) {
        if (confProps.getDomain().trim().length() == 0) {
            throw new ConfigurationException("Base url must be a valid string.");
        }
        this.domain = confProps.getDomain();
    }

    public String generateFileSharingLink(Long fileId, String fileName) throws ProcessingException {
        if (fileId == null || fileName == null || fileName.trim().length() == 0) {
            throw new ProcessingException("File ID and name are required for the sharing link.");
        }
        return String.format("%1$s/api/download_shared_file/%2$d/%3$s",
            this.domain, fileId, fileName);
    }
}
