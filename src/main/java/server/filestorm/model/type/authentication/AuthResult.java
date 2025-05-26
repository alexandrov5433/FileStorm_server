package server.filestorm.model.type.authentication;

public class AuthResult<T> {
    private Boolean isAuthError;
    private T payload;

    public AuthResult(Boolean isAuthError) {
        this.isAuthError = isAuthError;
    }

    public AuthResult(Boolean isAuthError, T payload) {
        this.isAuthError = isAuthError;
        this.payload = payload;
    }

    public Boolean getIsAuthError() {
        return isAuthError;
    }

    public T getPayload() {
        return payload;
    }
}
