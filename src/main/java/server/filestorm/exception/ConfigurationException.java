package server.filestorm.exception;

public class ConfigurationException extends RuntimeException {
    
    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
