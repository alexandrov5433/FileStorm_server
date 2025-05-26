package server.filestorm.exceptionHandler;

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

    @ExceptionHandler(FileManagementException.class)
    public ResponseEntity<ApiResponse<?>> handleFileManagementException(FileManagementException e) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(e.getMessage()));
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ApiResponse<?>> handleStorageException(StorageException e) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(e.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthenticationException(AuthenticationException e) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(e.getMessage()));
    }

    @ExceptionHandler(ProcessingException.class)
    public ResponseEntity<ApiResponse<?>> handleProcessingException(ProcessingException e) {
        return ResponseEntity.badRequest()
                .body(new ApiResponse<>(e.getMessage()));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleNoHandlerFoundException(NoHandlerFoundException e) {
        return ResponseEntity.status(404)
                .body(new ApiResponse<>("Page not found."));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<?>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(405)
                .body(new ApiResponse<>("This http method is not supported for this URL."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGenericException(Exception e) {
        return ResponseEntity.status(500)
                .body(new ApiResponse<>("Un unexpected error occured."));
    }
}
