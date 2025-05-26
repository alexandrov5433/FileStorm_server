package server.filestorm.exception;

public class FileManagementException extends RuntimeException {
    
    public FileManagementException(String message) {
        super(message);
    }

    public FileManagementException(String message, Throwable cause) {
        super(message, cause);
    }
}
