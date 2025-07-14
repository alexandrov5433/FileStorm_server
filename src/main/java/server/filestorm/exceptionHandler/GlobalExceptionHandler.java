package server.filestorm.exceptionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

import server.filestorm.exception.AuthenticationException;
import server.filestorm.exception.FileManagementException;
import server.filestorm.exception.ProcessingException;
import server.filestorm.exception.StorageException;
import server.filestorm.model.type.ApiResponse;

@ControllerAdvice
public class GlobalExceptionHandler {

    Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(FileManagementException.class)
    public ResponseEntity<ApiResponse<?>> handleFileManagementException(FileManagementException e) {
        logger.error(e.getMessage(), e);
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(e.getMessage()));
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiResponse<?>> handleStorageException(StorageException e) {
        logger.error(e.getMessage(), e);
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(e.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthenticationException(AuthenticationException e) {
        logger.error(e.getMessage(), e);
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(e.getMessage()));
    }

    @ExceptionHandler(ProcessingException.class)
    public ResponseEntity<ApiResponse<?>> handleProcessingException(ProcessingException e) {
        logger.error(e.getMessage(), e);
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(e.getMessage()));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNoHandlerFoundException(NoHandlerFoundException e) {
        logger.error(e.getMessage(), e);
        return ResponseEntity.status(404)
                .body(new ApiResponse<>("Page not found."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e) {
        logger.error(e.getMessage(), e);
        return ResponseEntity.status(405)
                .body(new ApiResponse<>("This http method is not supported for this URL."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGenericException(Exception e) {
        logger.error(e.getMessage(), e);
        return ResponseEntity.status(500)
                .body(new ApiResponse<>("Un unexpected error occured."));
    }
}
