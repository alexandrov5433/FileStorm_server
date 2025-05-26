package server.filestorm.model.type;

public class ApiResponse<T> {
    private String message;
    private T payload;

    public ApiResponse(String message) {
        this.message = message;
    }

    public ApiResponse(String message, T payload) {
        this.message = message;
        this.payload = payload;
    }

    public String getMessage() {
        return this.message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }
}
